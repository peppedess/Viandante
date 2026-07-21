package com.peppedess.viandante.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.coroutines.resume

object Http {
    suspend fun getJson(urlString: String): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "Viandante/1.0 (Android; app di peppedess)")
            connection.setRequestProperty("Accept", "application/json")
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}

object WikipediaApi {

    private const val LANG = "it"

    suspend fun geoSearch(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 10_000,
        limit: Int = 30
    ): List<GeoResult> {
        val url = "https://$LANG.wikipedia.org/w/api.php?action=query&list=geosearch" +
            "&gscoord=$latitude%7C$longitude&gsradius=$radiusMeters&gslimit=$limit&format=json"
        val json = Http.getJson(url)
        val array = json.optJSONObject("query")?.optJSONArray("geosearch") ?: return emptyList()
        val results = mutableListOf<GeoResult>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            results.add(
                GeoResult(
                    title = item.getString("title"),
                    distanceMeters = item.optDouble("dist", 0.0).toInt(),
                    latitude = item.getDouble("lat"),
                    longitude = item.getDouble("lon")
                )
            )
        }
        return results
    }

    suspend fun summary(title: String): PageSummary? {
        return try {
            val encoded = Uri.encode(title.replace(' ', '_'))
            val json = Http.getJson("https://$LANG.wikipedia.org/api/rest_v1/page/summary/$encoded")
            if (json.optString("type") == "disambiguation") return null
            PageSummary(
                title = json.optString("title", title),
                extract = json.optString("extract").ifBlank { null },
                imageUrl = bestImage(json),
                pageUrl = json.optJSONObject("content_urls")
                    ?.optJSONObject("desktop")
                    ?.optString("page")
                    ?.ifBlank { null },
                wikidataId = json.optString("wikibase_item").ifBlank { null }
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun population(wikidataId: String): Long? {
        return try {
            val json = Http.getJson(
                "https://www.wikidata.org/w/api.php?action=wbgetclaims&entity=$wikidataId&property=P1082&format=json"
            )
            val claims = json.optJSONObject("claims")?.optJSONArray("P1082") ?: return null
            if (claims.length() == 0) return null
            val latest = claims.getJSONObject(claims.length() - 1)
            latest.optJSONObject("mainsnak")
                ?.optJSONObject("datavalue")
                ?.optJSONObject("value")
                ?.optString("amount")
                ?.removePrefix("+")
                ?.toDoubleOrNull()
                ?.toLong()
        } catch (e: Exception) {
            null
        }
    }

    private fun bestImage(json: JSONObject): String? {
        val thumbnail = json.optJSONObject("thumbnail")?.optString("source")?.ifBlank { null }
        val original = json.optJSONObject("originalimage")?.optString("source")?.ifBlank { null }
        val enlarged = thumbnail?.replace(Regex("/(\\d+)px-"), "/1280px-")
        return enlarged ?: original
    }
}

object OpenMeteoApi {

    suspend fun currentWeather(latitude: Double, longitude: Double): WeatherInfo? {
        return try {
            val json = Http.getJson(
                "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m" +
                    "&daily=sunrise,sunset&timezone=auto"
            )
            val current = json.getJSONObject("current")
            val daily = json.optJSONObject("daily")
            val sunrise = daily?.optJSONArray("sunrise")?.optString(0)?.let { parseLocalMillis(it) }
            val sunset = daily?.optJSONArray("sunset")?.optString(0)?.let { parseLocalMillis(it) }
            WeatherInfo(
                temperature = current.getDouble("temperature_2m"),
                apparentTemperature = if (current.has("apparent_temperature")) current.getDouble("apparent_temperature") else null,
                weatherCode = current.optInt("weather_code", 0),
                windSpeedKmh = current.optDouble("wind_speed_10m", 0.0),
                humidityPercent = if (current.has("relative_humidity_2m")) current.getInt("relative_humidity_2m") else null,
                sunriseMillis = sunrise,
                sunsetMillis = sunset
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun elevation(latitude: Double, longitude: Double): Int? {
        return try {
            val json = Http.getJson("https://api.open-meteo.com/v1/elevation?latitude=$latitude&longitude=$longitude")
            val array = json.optJSONArray("elevation") ?: return null
            if (array.length() == 0) null else array.getDouble(0).toInt()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLocalMillis(iso: String): Long? = try {
        LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }
}

suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): GeoAddress? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                Geocoder(context, Locale.ITALIAN).getFromLocation(latitude, longitude, 1) { addresses ->
                    continuation.resume(addresses.firstOrNull()?.toGeoAddress())
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.ITALIAN)
                    .getFromLocation(latitude, longitude, 1)
                    ?.firstOrNull()
                    ?.toGeoAddress()
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun Address.toGeoAddress() = GeoAddress(
    locality = locality ?: subLocality,
    province = subAdminArea,
    region = adminArea,
    country = countryName
)
