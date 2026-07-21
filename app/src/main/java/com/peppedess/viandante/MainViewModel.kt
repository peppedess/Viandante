package com.peppedess.viandante

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.peppedess.viandante.data.AppDatabase
import com.peppedess.viandante.data.LocationClient
import com.peppedess.viandante.data.MotionState
import com.peppedess.viandante.data.OpenMeteoApi
import com.peppedess.viandante.data.PlaceInfo
import com.peppedess.viandante.data.Poi
import com.peppedess.viandante.data.UiState
import com.peppedess.viandante.data.VisitedPlace
import com.peppedess.viandante.data.WikipediaApi
import com.peppedess.viandante.data.angleDelta
import com.peppedess.viandante.data.bearingBetween
import com.peppedess.viandante.data.distanceMeters
import com.peppedess.viandante.data.formatDistance
import com.peppedess.viandante.data.motionFrom
import com.peppedess.viandante.data.reverseGeocode
import com.peppedess.viandante.data.sideLabel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.get(application)

    val history = database.visitedDao().all()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var trackingJob: Job? = null
    private var refreshJob: Job? = null

    private var hasAnchor = false
    private var anchorLat = 0.0
    private var anchorLon = 0.0

    private var hasPrev = false
    private var prevLat = 0.0
    private var prevLon = 0.0
    private var prevTime = 0L
    private var lastHeading: Float? = null

    private var lastSavedName: String? = null
    private val notifiedPois = mutableSetOf<String>()

    init {
        Notifier.ensureChannel(application)
    }

    fun startTracking() {
        if (trackingJob != null) return
        trackingJob = viewModelScope.launch {
            LocationClient(getApplication()).updates().collect { location ->
                onLocation(location)
            }
        }
    }

    fun refreshManual() {
        if (hasAnchor) refresh(anchorLat, anchorLon)
        else if (_state.value.curLat != null) refresh(_state.value.curLat!!, _state.value.curLon!!)
    }

    fun clearHistory() {
        viewModelScope.launch { database.visitedDao().clear() }
    }

    private fun onLocation(location: Location) {
        val now = System.currentTimeMillis()
        var speedMs = if (location.hasSpeed()) location.speed else 0f
        var heading: Float? = if (location.hasBearing()) location.bearing else lastHeading

        if (hasPrev) {
            val res = FloatArray(3)
            Location.distanceBetween(prevLat, prevLon, location.latitude, location.longitude, res)
            val dt = (now - prevTime) / 1000f
            if (dt > 0f && res[0] > 5f) {
                if (!location.hasSpeed()) speedMs = res[0] / dt
                if (!location.hasBearing()) heading = res[1]
            }
        }
        prevLat = location.latitude
        prevLon = location.longitude
        prevTime = now
        hasPrev = true
        if (heading != null) lastHeading = heading

        val motion = motionFrom(speedMs)
        _state.update {
            it.copy(
                curLat = location.latitude,
                curLon = location.longitude,
                headingDeg = heading,
                speedKmh = speedMs * 3.6f,
                motion = motion
            )
        }

        maybeNotifyApproaching(location.latitude, location.longitude, heading, motion)

        if (hasAnchor && !_state.value.isFirstLoading) {
            val res = FloatArray(1)
            Location.distanceBetween(anchorLat, anchorLon, location.latitude, location.longitude, res)
            if (res[0] < 1200f) return
        }
        hasAnchor = true
        anchorLat = location.latitude
        anchorLon = location.longitude
        refresh(location.latitude, location.longitude)
    }

    private fun maybeNotifyApproaching(lat: Double, lon: Double, heading: Float?, motion: MotionState) {
        if (heading == null || motion == MotionState.STILL) return
        if (!Notifier.canNotify(getApplication())) return
        val pois = _state.value.pois
        if (pois.isEmpty()) return
        val best = pois
            .map { poi ->
                val delta = angleDelta(heading, bearingBetween(lat, lon, poi.latitude, poi.longitude))
                Triple(poi, delta, distanceMeters(lat, lon, poi.latitude, poi.longitude))
            }
            .filter { abs(it.second) < 50f && it.third in 150f..2200f }
            .minByOrNull { it.third } ?: return

        val poi = best.first
        if (poi.title in notifiedPois) return
        notifiedPois.add(poi.title)
        val side = sideLabel(best.second).replaceFirstChar { it.uppercase() }
        Notifier.approaching(
            getApplication(),
            poi.title.hashCode(),
            poi.title,
            "$side \u00B7 tra ${formatDistance(best.third.toInt())}"
        )
    }

    private fun refresh(latitude: Double, longitude: Double) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            try {
                val context = getApplication<Application>()
                val address = reverseGeocode(context, latitude, longitude)
                val name = address?.locality ?: address?.province ?: address?.region ?: "Luogo sconosciuto"

                coroutineScope {
                    val summaryDeferred = async { WikipediaApi.summary(name) }
                    val geoDeferred = async { WikipediaApi.geoSearch(latitude, longitude) }
                    val weatherDeferred = async { OpenMeteoApi.currentWeather(latitude, longitude) }
                    val elevationDeferred = async { OpenMeteoApi.elevation(latitude, longitude) }

                    val summary = summaryDeferred.await()
                    val populationDeferred = async {
                        summary?.wikidataId?.let { WikipediaApi.population(it) }
                    }

                    val nearby = geoDeferred.await()
                        .filter { !it.title.equals(name, ignoreCase = true) }
                        .sortedBy { it.distanceMeters }
                        .take(12)

                    val pois = nearby.map { geoResult ->
                        async {
                            val poiSummary = WikipediaApi.summary(geoResult.title)
                            Poi(
                                title = geoResult.title,
                                distanceMeters = geoResult.distanceMeters,
                                description = poiSummary?.extract,
                                imageUrl = poiSummary?.imageUrl,
                                latitude = geoResult.latitude,
                                longitude = geoResult.longitude,
                                pageUrl = poiSummary?.pageUrl
                            )
                        }
                    }.awaitAll().filter { it.imageUrl != null || it.description != null }

                    val place = PlaceInfo(
                        name = name,
                        region = address?.region,
                        province = address?.province,
                        description = summary?.extract,
                        imageUrl = summary?.imageUrl ?: pois.firstOrNull { it.imageUrl != null }?.imageUrl,
                        latitude = latitude,
                        longitude = longitude,
                        elevation = elevationDeferred.await(),
                        population = populationDeferred.await(),
                        pageUrl = summary?.pageUrl
                    )

                    _state.update {
                        it.copy(
                            isFirstLoading = false,
                            isRefreshing = false,
                            place = place,
                            pois = pois,
                            weather = weatherDeferred.await()
                        )
                    }

                    if (name != "Luogo sconosciuto" && name != lastSavedName) {
                        lastSavedName = name
                        notifiedPois.clear()
                        database.visitedDao().insert(
                            VisitedPlace(
                                name = name,
                                region = address?.region,
                                imageUrl = place.imageUrl,
                                latitude = latitude,
                                longitude = longitude,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                        val short = place.description?.substringBefore(". ")?.take(150)
                            ?: address?.region ?: ""
                        Notifier.crossing(context, name, short)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRefreshing = false,
                        error = "Impossibile recuperare i dati. Controlla la connessione."
                    )
                }
            }
        }
    }
}
