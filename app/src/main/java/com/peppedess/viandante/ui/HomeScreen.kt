package com.peppedess.viandante.ui

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import androidx.palette.graphics.Palette
import com.peppedess.viandante.MainViewModel
import com.peppedess.viandante.data.MotionState
import com.peppedess.viandante.data.PlaceInfo
import com.peppedess.viandante.data.Poi
import com.peppedess.viandante.data.WeatherInfo
import com.peppedess.viandante.data.angleDelta
import com.peppedess.viandante.data.bearingBetween
import com.peppedess.viandante.data.distanceMeters
import com.peppedess.viandante.data.formatDistance
import com.peppedess.viandante.data.sideArrow
import com.peppedess.viandante.data.sideLabel
import com.peppedess.viandante.data.weatherAdvice
import com.peppedess.viandante.data.weatherDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

private val InkColor = Color(0xFF1C1B1F)

/** Chip informativo proattivo mostrato nell'area contenuti. */
data class InsightChip(
    val emoji: String,
    val text: String,
    val highlighted: Boolean = false,
    val onClick: (() -> Unit)? = null
)

private data class Approach(val index: Int, val poi: Poi, val delta: Float, val dist: Float)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onHistoryClick: () -> Unit
) {
    val state by vm.state.collectAsState()
    val haptic = LocalHapticFeedback.current

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (state.isFirstLoading) {
            FirstLoading(error = state.error, onRetry = vm::refreshManual)
        } else {
            val pageCount = 1 + state.pois.size
            val pagerState = rememberPagerState(pageCount = { pageCount })
            var immersive by remember { mutableStateOf(false) }
            var sheetPage by remember { mutableStateOf<Int?>(null) }
            var mapExpanded by remember { mutableStateOf(false) }

            LaunchedEffect(state.place?.name) { pagerState.scrollToPage(0) }
            LaunchedEffect(pagerState.settledPage) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }

            // POI in arrivo lungo la direzione di marcia
            val approaching: Approach? = run {
                val h = state.headingDeg
                val la = state.curLat
                val lo = state.curLon
                if (h == null || la == null || lo == null || state.motion == MotionState.STILL) null
                else state.pois.mapIndexed { i, p ->
                    Approach(i, p, angleDelta(h, bearingBetween(la, lo, p.latitude, p.longitude)),
                        distanceMeters(la, lo, p.latitude, p.longitude))
                }.filter { abs(it.delta) < 55f }.minByOrNull { it.dist }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val poi = if (page == 0) null else state.pois.getOrNull(page - 1)
                val place = state.place

                val chips: List<InsightChip> = if (poi == null) {
                    buildPlaceChips(state, approaching) { idx -> sheetPage = idx + 1 }
                } else {
                    buildPoiChips(state, poi)
                }

                ExplorerPage(
                    eyebrow = if (poi == null) "STAI ATTRAVERSANDO"
                    else "A ${formatDistance(poi.distanceMeters).uppercase(Locale.ITALIAN)} DA TE",
                    title = poi?.title ?: place?.name ?: "\u2026",
                    subtitle = if (poi == null) place?.region else null,
                    description = poi?.description ?: place?.description,
                    imageUrl = poi?.imageUrl ?: place?.imageUrl,
                    chips = chips,
                    pagerState = pagerState,
                    page = page,
                    immersive = immersive,
                    onToggleImmersive = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        immersive = !immersive
                    },
                    onOpenSheet = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sheetPage = page
                    }
                )
            }

            // Barra alta con pill vetrose
            AnimatedVisibility(
                visible = !immersive,
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
                        GlassPill {
                            Text(
                                "VIANDANTE",
                                style = MaterialTheme.typography.labelLarge,
                                color = InkColor,
                                letterSpacing = 4.sp
                            )
                        }
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
                        GlassPill(onClick = { sheetPage = 0 }) {
                            Text(
                                "$emoji " + String.format(Locale.ITALIAN, "%.0f\u00B0", weather.temperature),
                                style = MaterialTheme.typography.titleMedium,
                                color = InkColor
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !immersive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PageIndicator(
                    pageCount = pageCount,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 14.dp)
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
                        .padding(bottom = 44.dp)
                ) {
                    Text(
                        error,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

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
                        state.pois.getOrNull(page - 1)?.let { poi -> PoiSheet(poi) }
                    }
                }
            }

            if (mapExpanded) {
                state.place?.let { place ->
                    FullMapDialog(
                        latitude = place.latitude,
                        longitude = place.longitude,
                        placeName = place.name,
                        heading = state.headingDeg,
                        onDismiss = { mapExpanded = false }
                    )
                }
            }
        }
    }
}

private fun buildPlaceChips(
    state: com.peppedess.viandante.data.UiState,
    approaching: Approach?,
    onApproachClick: (Int) -> Unit
): List<InsightChip> = buildList {
    if (state.motion != MotionState.STILL) {
        add(InsightChip(state.motion.emoji, "${state.speedKmh.roundToInt()} km/h \u00B7 ${state.motion.label}"))
    }
    approaching?.let { ap ->
        add(
            InsightChip(
                sideArrow(ap.delta),
                "${ap.poi.title} \u00B7 ${sideLabel(ap.delta)}",
                highlighted = true,
                onClick = { onApproachClick(ap.index) }
            )
        )
    }
    state.weather?.sunsetMillis?.let { sunset ->
        val minutes = ((sunset - System.currentTimeMillis()) / 60_000L).toInt()
        if (minutes in 1..90) {
            val golden = minutes <= 60
            add(
                InsightChip(
                    if (golden) "\uD83C\uDF07" else "\uD83C\uDF05",
                    if (golden) "Ora d'oro \u00B7 tramonto tra ${minutes}\u2032"
                    else "Tramonto tra ${minutes}\u2032"
                )
            )
        }
    }
    weatherAdvice(state.weather)?.let { (emoji, text) ->
        add(InsightChip(emoji, text))
    }
}

private fun buildPoiChips(
    state: com.peppedess.viandante.data.UiState,
    poi: Poi
): List<InsightChip> = buildList {
    val h = state.headingDeg
    val la = state.curLat
    val lo = state.curLon
    if (h != null && la != null && lo != null && state.motion != MotionState.STILL) {
        val delta = angleDelta(h, bearingBetween(la, lo, poi.latitude, poi.longitude))
        if (abs(delta) < 70f) {
            add(InsightChip(sideArrow(delta), sideLabel(delta), highlighted = true))
        }
    }
}

@Composable
private fun rememberAccent(imageUrl: String?): Color {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.primary
    var target by remember(imageUrl) { mutableStateOf(fallback) }
    LaunchedEffect(imageUrl) {
        if (imageUrl == null) {
            target = fallback
            return@LaunchedEffect
        }
        try {
            val result = context.imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .size(128)
                    .build()
            )
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val rgb = withContext(Dispatchers.Default) {
                    val palette = Palette.from(bitmap).generate()
                    palette.vibrantSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                }
                if (rgb != null) target = Color(rgb)
            }
        } catch (e: Exception) {
            // Mantieni il fallback
        }
    }
    val animated by animateColorAsState(targetValue = target, animationSpec = tween(600), label = "accent")
    return animated
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExplorerPage(
    eyebrow: String,
    title: String,
    subtitle: String?,
    description: String?,
    imageUrl: String?,
    chips: List<InsightChip>,
    pagerState: PagerState,
    page: Int,
    immersive: Boolean,
    onToggleImmersive: () -> Unit,
    onOpenSheet: () -> Unit
) {
    val accent = rememberAccent(imageUrl)
    val imageFraction by animateFloatAsState(
        targetValue = if (immersive) 1f else 0.56f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "imageFraction"
    )
    val contentAlpha = ((1f - imageFraction) / 0.44f).coerceIn(0f, 1f)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(imageFraction)
                .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleImmersive
                )
        ) {
            val infinite = rememberInfiniteTransition(label = "kenburns")
            val kbScale by infinite.animateFloat(
                initialValue = 1.03f,
                targetValue = 1.14f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 16_000, easing = LinearEasing),
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
                            val depth = 1f - 0.06f * abs(pageOffset).coerceIn(0f, 1f)
                            translationX = pageOffset * size.width * 0.16f
                            scaleX = kbScale * depth
                            scaleY = kbScale * depth
                        }
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.9f))
                            )
                        )
                ) {
                    Text("\uD83C\uDFDB\uFE0F", fontSize = 80.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
            // Velo d'accento in basso, per continuit\u00E0 cromatica con l'area bianca
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1f to accent.copy(alpha = 0.28f)
                        )
                    )
            )
            val pillAlpha by animateFloatAsState(
                targetValue = if (immersive) 0f else 1f,
                animationSpec = tween(250),
                label = "pillAlpha"
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .graphicsLayer { alpha = pillAlpha }
            ) {
                GlassPill {
                    Text(
                        eyebrow,
                        style = MaterialTheme.typography.labelLarge,
                        color = InkColor,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight((1f - imageFraction).coerceAtLeast(0.001f))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .graphicsLayer { alpha = contentAlpha }
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                title,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (chips.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { chip -> InsightChipView(chip, accent) }
                }
            }
            description?.let {
                Spacer(Modifier.height(14.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 25.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(18.dp))
            val onAccent = if (accent.luminance() > 0.5f) InkColor else Color.White
            Surface(
                onClick = onOpenSheet,
                shape = CircleShape,
                color = accent,
                shadowElevation = 3.dp,
                modifier = Modifier.height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = onAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("Scopri di pi\u00F9", style = MaterialTheme.typography.titleMedium, color = onAccent)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun GlassPill(
    onClick: (() -> Unit)? = null,
    bg: Color = Color.White.copy(alpha = 0.9f),
    content: @Composable RowScope.() -> Unit
) {
    val base = Modifier
        .shadow(6.dp, CircleShape, clip = false)
        .clip(CircleShape)
        .background(bg)
        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
    val modifier = if (onClick != null) base.clickable(onClick = onClick) else base
    Row(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun InsightChipView(chip: InsightChip, accent: Color) {
    val bg = if (chip.highlighted) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val base = Modifier
        .clip(CircleShape)
        .background(bg)
        .then(
            if (chip.highlighted) Modifier.border(1.dp, accent.copy(alpha = 0.5f), CircleShape)
            else Modifier
        )
    val modifier = if (chip.onClick != null) base.clickable(onClick = chip.onClick) else base
    Row(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(chip.emoji, fontSize = 15.sp)
        Spacer(Modifier.width(7.dp))
        Text(
            chip.text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 230.dp)
        )
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = InkColor)
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
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
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
                onClick = { openUrl(context, url) },
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
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
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
                    val uri = Uri.parse(
                        "geo:${poi.latitude},${poi.longitude}?q=${poi.latitude},${poi.longitude}(${Uri.encode(poi.title)})"
                    )
                    try {
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
                FilledTonalButton(onClick = { openUrl(context, url) }, modifier = Modifier.height(52.dp)) {
                    Text("Wikipedia")
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        // Nessun browser disponibile
    }
}

@Composable
private fun FullMapDialog(
    latitude: Double,
    longitude: Double,
    placeName: String,
    heading: Float?,
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
                .background(Color.White)
        ) {
            MiniMap(latitude, longitude, zoom = zoom, heading = heading, modifier = Modifier.fillMaxSize())
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
            ) {
                GlassPill {
                    Text(placeName, style = MaterialTheme.typography.titleMedium, color = InkColor)
                }
            }
            FilledIconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White, contentColor = InkColor
                ),
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
                FilledIconButton(
                    onClick = { if (zoom < 17) zoom++ },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White, contentColor = InkColor
                    )
                ) { Icon(Icons.Filled.Add, contentDescription = "Zoom avanti") }
                FilledIconButton(
                    onClick = { if (zoom > 10) zoom-- },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White, contentColor = InkColor
                    )
                ) { Icon(Icons.Filled.Remove, contentDescription = "Zoom indietro") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FirstLoading(error: String?, onRetry: () -> Unit) {
    // Sfondo a gradiente animato per un tocco moderno
    val infinite = rememberInfiniteTransition(label = "loadingBg")
    val shift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift"
    )
    val c1 = MaterialTheme.colorScheme.primaryContainer
    val c2 = MaterialTheme.colorScheme.tertiaryContainer
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(c1, c2),
                    start = androidx.compose.ui.geometry.Offset(shift * 600f, 0f),
                    end = androidx.compose.ui.geometry.Offset(600f + shift * 400f, 1400f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (error == null) {
                LoadingIndicator(modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(24.dp))
                Text(
                    "Sto scoprendo dove ti trovi\u2026",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
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
                Text(
                    error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onRetry) { Text("Riprova") }
            }
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
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
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
        StatTile("\u26F0\uFE0F", place.elevation?.let { "$it m" } ?: "\u2014", "Altitudine", Modifier.weight(1f))
        StatTile(
            "\uD83D\uDC65",
            place.population?.let { NumberFormat.getInstance(Locale.ITALIAN).format(it) } ?: "\u2014",
            "Abitanti",
            Modifier.weight(1f)
        )
        StatTile("\uD83D\uDCCD", place.province ?: place.region ?: "\u2014", "Provincia", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Box(Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                GlassPill {
                    Text("Tocca per espandere", style = MaterialTheme.typography.labelMedium, color = InkColor)
                }
            }
        }
    }
}

@Composable
fun MiniMap(
    latitude: Double,
    longitude: Double,
    zoom: Int = 13,
    heading: Float? = null,
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
        if (heading != null) {
            Icon(
                Icons.Filled.Navigation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
                    .rotate(heading)
            )
        } else {
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
