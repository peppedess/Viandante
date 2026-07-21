package com.peppedess.viandante.data

import java.util.Locale
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Rotta iniziale (0-360°) dal punto 1 al punto 2. */
fun bearingBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val dLon = Math.toRadians(lon2 - lon1)
    val y = sin(dLon) * cos(Math.toRadians(lat2))
    val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
        sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
    return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
}

/** Differenza angolare firmata (-180..180) tra due rotte. */
fun angleDelta(from: Float, to: Float): Float {
    var d = (to - from) % 360f
    if (d < -180f) d += 360f
    if (d > 180f) d -= 360f
    return d
}

/** Distanza in metri (haversine). */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return (2 * r * asin(min(1.0, sqrt(a)))).toFloat()
}

fun sideLabel(delta: Float): String = when {
    delta > 20f -> "sulla tua destra"
    delta < -20f -> "sulla tua sinistra"
    else -> "dritto davanti"
}

fun sideArrow(delta: Float): String = when {
    delta > 20f -> "\u27A1\uFE0F"
    delta < -20f -> "\u2B05\uFE0F"
    else -> "\u2B06\uFE0F"
}

fun formatDistance(meters: Int): String =
    if (meters < 1000) "$meters m"
    else String.format(Locale.ITALIAN, "%.1f km", meters / 1000.0)
