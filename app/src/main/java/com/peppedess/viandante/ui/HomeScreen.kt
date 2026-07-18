package com.peppedess.viandante.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.peppedess.viandante.MainViewModel
import com.peppedess.viandante.data.PlaceInfo
import com.peppedess.viandante.data.Poi
import com.peppedess.viandante.data.WeatherInfo
import com.peppedess.viandante.data.weatherDescription
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onPoiClick: (Int) -> Unit,
    onHistoryClick: () -> Unit,
    sharedScope: SharedTransitionScope,
    animScope: AnimatedContentScope
) {
    val state by vm.state.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (state.isFirstLoading) {
            FirstLoading(error = state.error, onRetry = vm::refreshManual)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 130.dp)
            ) {
                item { HeroHeader(state.place, state.isRefreshing) }
                state.place?.description?.let { description ->
                    item { DescriptionBlock(description) }
                }
                state.weather?.let { weather ->
                    item { WeatherCard(weather) }
                }
                state.place?.let { place ->
                    item { TechInfoRow(place) }
                    item { MapCard(place.latitude, place.longitude) }
                }
                if (state.pois.isNotEmpty()) {
                    item {
                        Text(
                            "Da vedere nei dintorni",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 8.dp)
                        )
                    }
                    itemsIndexed(state.pois, key = { _, poi -> poi.title }) { index, poi ->
                        PoiCard(poi, index, sharedScope, animScope) { onPoiClick(index) }
                    }
                }
                state.error?.let { error ->
                    item {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }

        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            FilledIconButton(onClick = { }) {
                Icon(Icons.Filled.Explore, contentDescription = "Esplora")
            }
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Filled.History, contentDescription = "Cronologia")
            }
            IconButton(onClick = vm::refreshManual) {
                Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FirstLoading(error: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (error == null) {
            LoadingIndicator(modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(24.dp))
            Text(
                "Sto scoprendo dove ti trovi\u2026",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Un attimo e ti racconto tutto su questo posto.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Text("\uD83D\uDE15", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text(error, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Riprova") }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroHeader(place: PlaceInfo?, refreshing: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(480.dp)
    ) {
        if (place?.imageUrl != null) {
            AsyncImage(
                model = place.imageUrl,
                contentDescription = place.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.25f),
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.8f)
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Text(
                "STAI ATTRAVERSANDO",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                place?.name ?: "\u2026",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                place?.region?.let { HeroChip(it) }
                place?.let {
                    HeroChip(String.format(Locale.ITALIAN, "%.4f\u00B0, %.4f\u00B0", it.latitude, it.longitude))
                }
            }
        }
        if (refreshing) {
            LinearWavyProgressIndicator(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun DescriptionBlock(description: String) {
    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp)
    ) {
        Text("In breve", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 26.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeatherCard(weather: WeatherInfo) {
    val (emoji, label) = weatherDescription(weather.weatherCode)
    val onColor = MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp)
            .fillMaxWidth()
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 52.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    String.format(Locale.ITALIAN, "%.0f\u00B0", weather.temperature),
                    style = MaterialTheme.typography.displayMedium,
                    color = onColor
                )
                Text(label, style = MaterialTheme.typography.titleMedium, color = onColor)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "\uD83D\uDCA8 " + String.format(Locale.ITALIAN, "%.0f km/h", weather.windSpeedKmh),
                    style = MaterialTheme.typography.labelLarge,
                    color = onColor
                )
                weather.humidityPercent?.let {
                    Text("\uD83D\uDCA7 $it%", style = MaterialTheme.typography.labelLarge, color = onColor)
                }
                weather.apparentTemperature?.let {
                    Text(
                        "Percepiti " + String.format(Locale.ITALIAN, "%.0f\u00B0", it),
                        style = MaterialTheme.typography.labelLarge,
                        color = onColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TechInfoRow(place: PlaceInfo) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatTile(
            emoji = "\u26F0\uFE0F",
            value = place.elevation?.let { "$it m" } ?: "\u2014",
            label = "Altitudine",
            modifier = Modifier.weight(1f)
        )
        StatTile(
            emoji = "\uD83D\uDC65",
            value = place.population?.let { NumberFormat.getInstance(Locale.ITALIAN).format(it) } ?: "\u2014",
            label = "Abitanti",
            modifier = Modifier.weight(1f)
        )
        StatTile(
            emoji = "\uD83D\uDCCD",
            value = place.province ?: place.region ?: "\u2014",
            label = "Provincia",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MapCard(latitude: Double, longitude: Double) {
    Card(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .height(200.dp)
    ) {
        MiniMap(latitude, longitude, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun MiniMap(
    latitude: Double,
    longitude: Double,
    zoom: Int = 13,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val density = LocalDensity.current
        val tilePx = with(density) { 256.dp.toPx() }
        val tileCount = 1 shl zoom
        val xTile = (longitude + 180.0) / 360.0 * tileCount
        val latRadians = Math.toRadians(latitude)
        val yTile = (1.0 - ln(tan(latRadians) + 1.0 / cos(latRadians)) / PI) / 2.0 * tileCount
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val leftPx = (xTile * tilePx - widthPx / 2f).toFloat()
        val topPx = (yTile * tilePx - heightPx / 2f).toFloat()
        val firstX = floor(leftPx / tilePx).toInt()
        val firstY = floor(topPx / tilePx).toInt()
        val lastX = floor((leftPx + widthPx) / tilePx).toInt()
        val lastY = floor((topPx + heightPx) / tilePx).toInt()
        for (tx in firstX..lastX) {
            for (ty in firstY..lastY) {
                if (ty < 0 || ty >= tileCount) continue
                val wrappedX = ((tx % tileCount) + tileCount) % tileCount
                AsyncImage(
                    model = "https://tile.openstreetmap.org/$zoom/$wrappedX/$ty.png",
                    contentDescription = null,
                    modifier = Modifier
                        .offset { IntOffset((tx * tilePx - leftPx).roundToInt(), (ty * tilePx - topPx).roundToInt()) }
                        .size(256.dp)
                )
            }
        }
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(44.dp)
                .align(Alignment.Center)
                .offset(y = (-18).dp)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PoiCard(
    poi: Poi,
    index: Int,
    sharedScope: SharedTransitionScope,
    animScope: AnimatedContentScope,
    onClick: () -> Unit
) {
    with(sharedScope) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(240.dp)
                .staggeredEntrance(index)
                .clip(RoundedCornerShape(28.dp))
                .clickable(onClick = onClick)
        ) {
            if (poi.imageUrl != null) {
                AsyncImage(
                    model = poi.imageUrl,
                    contentDescription = poi.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(rememberSharedContentState(key = "poi-img-${poi.title}"), animScope)
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text("\uD83C\uDFDB\uFE0F", fontSize = 64.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.4f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
            )
            Text(
                poi.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            )
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
            ) {
                Text(
                    formatDistance(poi.distanceMeters),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun Modifier.staggeredEntrance(index: Int): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "staggerAlpha"
    )
    val translation by animateFloatAsState(
        targetValue = if (visible) 0f else 90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "staggerTranslation"
    )
    return this.graphicsLayer {
        this.alpha = alpha
        translationY = translation
    }
}

fun formatDistance(meters: Int): String =
    if (meters < 1000) "$meters m"
    else String.format(Locale.ITALIAN, "%.1f km", meters / 1000.0)
