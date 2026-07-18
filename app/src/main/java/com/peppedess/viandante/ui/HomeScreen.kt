package com.peppedess.viandante.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onHistoryClick: () -> Unit
) {
    val state by vm.state.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.isFirstLoading) {
            FirstLoading(error = state.error, onRetry = vm::refreshManual)
        } else {
            val pageCount = 1 + state.pois.size
            val pagerState = rememberPagerState(pageCount = { pageCount })
            var overlayVisible by remember { mutableStateOf(true) }
            var sheetPage by remember { mutableStateOf<Int?>(null) }
            var mapExpanded by remember { mutableStateOf(false) }

            LaunchedEffect(state.place?.name) {
                pagerState.scrollToPage(0)
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val poi = if (page == 0) null else state.pois.getOrNull(page - 1)
                val place = state.place
                ExplorerPage(
                    eyebrow = if (poi == null) "STAI ATTRAVERSANDO"
                    else "A ${formatDistance(poi.distanceMeters).uppercase(Locale.ITALIAN)} DA TE",
                    title = poi?.title ?: place?.name ?: "\u2026",
                    description = poi?.description ?: place?.description,
                    imageUrl = poi?.imageUrl ?: place?.imageUrl,
                    pagerState = pagerState,
                    page = page,
                    overlayVisible = overlayVisible,
                    onToggle = { overlayVisible = !overlayVisible },
                    onOpenSheet = { sheetPage = page }
                )
            }

            // Overlay globali: barra alta, meteo, indicatore pagine, progresso
            AnimatedVisibility(
                visible = overlayVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "VIANDANTE",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 4.sp
                        )
                        Spacer(Modifier.weight(1f))
                        GlassIconButton(Icons.Filled.Map, "Mappa") { mapExpanded = true }
                        Spacer(Modifier.width(8.dp))
                        GlassIconButton(Icons.Filled.History, "Cronologia", onHistoryClick)
                        Spacer(Modifier.width(8.dp))
                        GlassIconButton(Icons.Filled.Refresh, "Aggiorna", vm::refreshManual)
                    }
                    state.weather?.let { weather ->
                        Spacer(Modifier.height(10.dp))
                        val (emoji, _) = weatherDescription(weather.weatherCode)
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.35f),
                            onClick = { sheetPage = 0 }
                        ) {
                            Text(
                                "$emoji " + String.format(Locale.ITALIAN, "%.0f\u00B0", weather.temperature),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = overlayVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PageIndicator(
                    pageCount = pageCount,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 18.dp)
                )
            }

            if (state.isRefreshing) {
                LinearWavyProgressIndicator(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                )
            }

            state.error?.let { error ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 48.dp)
                ) {
                    Text(
                        error,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Bottom sheet con i dettagli del luogo o del POI
            sheetPage?.let { page ->
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { sheetPage = null },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    if (page == 0) {
                        state.place?.let { place ->
                            PlaceSheet(place, state.weather) { mapExpanded = true }
                        }
                    } else {
                        state.pois.getOrNull(page - 1)?.let { poi ->
                            PoiSheet(poi)
                        }
                    }
                }
            }

            // Mappa a tutto schermo con zoom
            if (mapExpanded) {
                state.place?.let { place ->
                    FullMapDialog(
                        latitude = place.latitude,
                        longitude = place.longitude,
                        placeName = place.name,
                        onDismiss = { mapExpanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplorerPage(
    eyebrow: String,
    title: String,
    description: String?,
    imageUrl: String?,
    pagerState: PagerState,
    page: Int,
    overlayVisible: Boolean,
    onToggle: () -> Unit,
    onOpenSheet: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
    ) {
        // Immagine con parallasse legata al gesto + effetto Ken Burns
        val infinite = rememberInfiniteTransition(label = "kenburns")
        val kbScale by infinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "kbScale"
        )
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = pagerState.currentPage - page + pagerState.currentPageOffsetFraction
                        translationX = pageOffset * size.width * 0.3f
                        scaleX = kbScale
                        scaleY = kbScale
                    }
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
            ) {
                Text(
                    "\uD83C\uDFDB\uFE0F",
                    fontSize = 80.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        AnimatedVisibility(visible = overlayVisible, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize()) {
                // Scrim alto e basso per leggibilit\u00E0
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.45f),
                                0.35f to Color.Transparent,
                                0.55f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.85f)
                            )
                        )
                )
                // Testi che sfumano e scorrono col gesto (parallasse in primo piano)
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, bottom = 56.dp)
                        .graphicsLayer {
                            val pageOffset = pagerState.currentPage - page + pagerState.currentPageOffsetFraction
                            alpha = (1f - abs(pageOffset) * 1.6f).coerceIn(0f, 1f)
                            translationX = pageOffset * size.width * 0.15f
                        }
                ) {
                    Text(
                        eyebrow,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    description?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.88f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.22f),
                        onClick = onOpenSheet
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Scopri di pi\u00F9",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.35f),
            contentColor = Color.White
        )
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) 26.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "indicatorWidth"
            )
            Box(
                Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (selected) 0.95f else 0.4f))
            )
        }
    }
}

@Composable
private fun PlaceSheet(place: PlaceInfo, weather: WeatherInfo?, onMapClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            place.name,
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        place.region?.let {
            Text(
                it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        place.description?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        weather?.let {
            Spacer(Modifier.height(8.dp))
            WeatherCard(it)
        }
        Spacer(Modifier.height(4.dp))
        TechInfoRow(place)
        Spacer(Modifier.height(4.dp))
        MapCard(place.latitude, place.longitude, onClick = onMapClick)
        place.pageUrl?.let { url ->
            Spacer(Modifier.height(20.dp))
            FilledTonalButton(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        // Nessun browser disponibile
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .height(52.dp)
            ) {
                Text("Approfondisci su Wikipedia")
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PoiSheet(poi: Poi) {
    val context = LocalContext.current
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                "a ${formatDistance(poi.distanceMeters)} da te",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(poi.title, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))
        Text(
            poi.description
                ?: "Non ho trovato una descrizione per questo luogo, ma a volte le sorprese migliori sono quelle da scoprire di persona.",
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 28.sp
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    try {
                        val uri = Uri.parse(
                            "geo:${poi.latitude},${poi.longitude}?q=${poi.latitude},${poi.longitude}(${Uri.encode(poi.title)})"
                        )
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: Exception) {
                        // Nessuna app di mappe disponibile
                    }
                },
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Filled.Directions, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Portami qui")
            }
            poi.pageUrl?.let { url ->
                FilledTonalButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: Exception) {
                            // Nessun browser disponibile
                        }
                    },
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("Wikipedia")
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun FullMapDialog(
    latitude: Double,
    longitude: Double,
    placeName: String,
    onDismiss: () -> Unit
) {
    var zoom by remember { mutableStateOf(13) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            MiniMap(latitude, longitude, zoom = zoom, modifier = Modifier.fillMaxSize())
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
            ) {
                Text(
                    placeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
            FilledIconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Chiudi")
            }
            Column(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledIconButton(onClick = { if (zoom < 17) zoom++ }) {
                    Icon(Icons.Filled.Add, contentDescription = "Zoom avanti")
                }
                FilledIconButton(onClick = { if (zoom > 10) zoom-- }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Zoom indietro")
                }
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
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Un attimo e ti racconto tutto su questo posto.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        } else {
            Text("\uD83D\uDE15", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) { Text("Riprova") }
        }
    }
}

@Composable
fun WeatherCard(weather: WeatherInfo) {
    val (emoji, label) = weatherDescription(weather.weatherCode)
    val onColor = MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
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
fun TechInfoRow(place: PlaceInfo) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapCard(latitude: Double, longitude: Double, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp)
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            MiniMap(latitude, longitude, modifier = Modifier.fillMaxSize())
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Text(
                    "Tocca per espandere",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
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
