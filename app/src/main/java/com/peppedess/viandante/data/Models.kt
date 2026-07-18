package com.peppedess.viandante.data

data class PlaceInfo(
    val name: String,
    val region: String?,
    val province: String?,
    val description: String?,
    val imageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val elevation: Int?,
    val population: Long?,
    val pageUrl: String?
)

data class Poi(
    val title: String,
    val distanceMeters: Int,
    val description: String?,
    val imageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val pageUrl: String?
)

data class WeatherInfo(
    val temperature: Double,
    val apparentTemperature: Double?,
    val weatherCode: Int,
    val windSpeedKmh: Double,
    val humidityPercent: Int?
)

data class GeoResult(
    val title: String,
    val distanceMeters: Int,
    val latitude: Double,
    val longitude: Double
)

data class PageSummary(
    val title: String,
    val extract: String?,
    val imageUrl: String?,
    val pageUrl: String?,
    val wikidataId: String?
)

data class GeoAddress(
    val locality: String?,
    val province: String?,
    val region: String?,
    val country: String?
)

data class UiState(
    val isFirstLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val place: PlaceInfo? = null,
    val pois: List<Poi> = emptyList(),
    val weather: WeatherInfo? = null,
    val error: String? = null
)

fun weatherDescription(code: Int): Pair<String, String> = when (code) {
    0 -> "\u2600\uFE0F" to "Sereno"
    1 -> "\uD83C\uDF24\uFE0F" to "Prevalentemente sereno"
    2 -> "\u26C5" to "Parzialmente nuvoloso"
    3 -> "\u2601\uFE0F" to "Coperto"
    45, 48 -> "\uD83C\uDF2B\uFE0F" to "Nebbia"
    in 51..57 -> "\uD83C\uDF26\uFE0F" to "Pioviggine"
    in 61..67 -> "\uD83C\uDF27\uFE0F" to "Pioggia"
    in 71..77 -> "\uD83C\uDF28\uFE0F" to "Neve"
    in 80..82 -> "\uD83C\uDF27\uFE0F" to "Rovesci di pioggia"
    85, 86 -> "\uD83C\uDF28\uFE0F" to "Rovesci di neve"
    95 -> "\u26C8\uFE0F" to "Temporale"
    96, 99 -> "\u26C8\uFE0F" to "Temporale con grandine"
    else -> "\uD83C\uDF21\uFE0F" to "Condizioni variabili"
}
