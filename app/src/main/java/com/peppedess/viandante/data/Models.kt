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
    val humidityPercent: Int?,
    val sunriseMillis: Long?,
    val sunsetMillis: Long?
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

/** Stato di movimento dedotto dalla velocit\u00E0 GPS. */
enum class MotionState(val emoji: String, val label: String) {
    STILL("\uD83E\uDDCD", "Fermo"),
    WALK("\uD83D\uDEB6", "A piedi"),
    CITY("\uD83D\uDE8C", "In citt\u00E0"),
    ROAD("\uD83D\uDE97", "In viaggio"),
    TRAIN("\uD83D\uDE86", "In corsa")
}

fun motionFrom(speedMs: Float): MotionState = when {
    speedMs < 0.8f -> MotionState.STILL
    speedMs < 3.5f -> MotionState.WALK
    speedMs < 12.5f -> MotionState.CITY
    speedMs < 44f -> MotionState.ROAD
    else -> MotionState.TRAIN
}

data class UiState(
    val isFirstLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val place: PlaceInfo? = null,
    val pois: List<Poi> = emptyList(),
    val weather: WeatherInfo? = null,
    val error: String? = null,
    // Contesto di movimento, aggiornato a ogni posizione
    val curLat: Double? = null,
    val curLon: Double? = null,
    val headingDeg: Float? = null,
    val speedKmh: Float = 0f,
    val motion: MotionState = MotionState.STILL
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

/** Consiglio contestuale in base al meteo; null se non serve. */
fun weatherAdvice(weather: WeatherInfo?): Pair<String, String>? {
    if (weather == null) return null
    val code = weather.weatherCode
    return when {
        code in 51..67 || code in 80..82 || code in 95..99 ->
            "\u2614" to "Pioggia in zona, tieni l'ombrello a portata"
        code in 71..77 || code == 85 || code == 86 ->
            "\u2744\uFE0F" to "Neve in arrivo, copriti bene"
        code == 45 || code == 48 ->
            "\uD83C\uDF2B\uFE0F" to "Nebbia, visibilit\u00E0 ridotta"
        code <= 2 && weather.temperature >= 30 ->
            "\uD83D\uDCA7" to "Caldo intenso, resta idratato"
        code <= 1 && weather.temperature <= 3 ->
            "\uD83E\uDDE3" to "Freddo pungente, vestiti a strati"
        else -> null
    }
}
