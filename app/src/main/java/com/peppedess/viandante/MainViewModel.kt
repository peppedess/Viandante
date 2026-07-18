package com.peppedess.viandante

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.peppedess.viandante.data.AppDatabase
import com.peppedess.viandante.data.LocationClient
import com.peppedess.viandante.data.OpenMeteoApi
import com.peppedess.viandante.data.PlaceInfo
import com.peppedess.viandante.data.Poi
import com.peppedess.viandante.data.UiState
import com.peppedess.viandante.data.VisitedPlace
import com.peppedess.viandante.data.WikipediaApi
import com.peppedess.viandante.data.reverseGeocode
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.get(application)

    val history = database.visitedDao().all()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var trackingJob: Job? = null
    private var refreshJob: Job? = null
    private var hasLastPosition = false
    private var lastLatitude = 0.0
    private var lastLongitude = 0.0
    private var lastSavedName: String? = null

    fun startTracking() {
        if (trackingJob != null) return
        trackingJob = viewModelScope.launch {
            LocationClient(getApplication()).updates().collect { location ->
                onLocation(location)
            }
        }
    }

    fun refreshManual() {
        if (hasLastPosition) refresh(lastLatitude, lastLongitude)
    }

    fun clearHistory() {
        viewModelScope.launch { database.visitedDao().clear() }
    }

    private fun onLocation(location: Location) {
        if (hasLastPosition && !_state.value.isFirstLoading) {
            val results = FloatArray(1)
            Location.distanceBetween(
                lastLatitude, lastLongitude,
                location.latitude, location.longitude,
                results
            )
            if (results[0] < 1200f) return
        }
        hasLastPosition = true
        lastLatitude = location.latitude
        lastLongitude = location.longitude
        refresh(location.latitude, location.longitude)
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
