package com.peppedess.viandante.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.peppedess.viandante.MainViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PoiDetailScreen(
    vm: MainViewModel,
    index: Int,
    onBack: () -> Unit,
    sharedScope: SharedTransitionScope,
    animScope: AnimatedContentScope
) {
    val state by vm.state.collectAsState()
    val poi = state.pois.getOrNull(index)
    if (poi == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val context = LocalContext.current

    with(sharedScope) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(420.dp)
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
                        Text("\uD83C\uDFDB\uFE0F", fontSize = 72.sp, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.35f),
                                0.5f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.85f)
                            )
                        )
                )
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                }
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
                        Text(
                            "a ${formatDistance(poi.distanceMeters)} da te",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        poi.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(Modifier.padding(24.dp)) {
                Text(
                    poi.description
                        ?: "Non ho trovato una descrizione per questo luogo, ma a volte le sorprese migliori sono quelle da scoprire di persona.",
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(28.dp))
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
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
