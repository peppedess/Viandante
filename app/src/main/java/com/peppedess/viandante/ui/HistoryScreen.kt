package com.peppedess.viandante.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.peppedess.viandante.MainViewModel
import com.peppedess.viandante.data.VisitedPlace
import com.peppedess.viandante.data.distanceMeters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(vm: MainViewModel, onBack: () -> Unit) {
    val history by vm.history.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
            }
            Text(
                "I tuoi viaggi",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            if (history.isNotEmpty()) {
                IconButton(onClick = vm::clearHistory) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Svuota cronologia")
                }
            }
        }
        if (history.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("\uD83E\uDDF3", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text("Nessun viaggio registrato", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Muoviti in treno o in macchina e qui troverai tutti i luoghi che hai attraversato.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)
            ) {
                item { JourneyStats(history) }
                itemsIndexed(history, key = { _, item -> item.id }) { index, visited ->
                    HistoryCard(visited, index)
                }
            }
        }
    }
}

@Composable
private fun JourneyStats(history: List<VisitedPlace>) {
    val totalKm = remember(history) {
        var meters = 0f
        for (i in 0 until history.size - 1) {
            meters += distanceMeters(
                history[i].latitude, history[i].longitude,
                history[i + 1].latitude, history[i + 1].longitude
            )
        }
        (meters / 1000f).toInt()
    }
    val regions = remember(history) { history.mapNotNull { it.region }.distinct().size }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            Modifier.padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell("${history.size}", "tappe")
            StatCell("~$totalKm", "km percorsi")
            StatCell("$regions", if (regions == 1) "regione" else "regioni")
        }
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun HistoryCard(visited: VisitedPlace, index: Int) {
    val formatter = SimpleDateFormat("d MMMM yyyy \u00B7 HH:mm", Locale.ITALIAN)
    Box(
        Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .height(190.dp)
            .staggeredEntrance(index)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        if (visited.imageUrl != null) {
            AsyncImage(
                model = visited.imageUrl,
                contentDescription = visited.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.3f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Text(
                visited.name,
                style = MaterialTheme.typography.headlineSmall,
                color = if (visited.imageUrl != null) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    visited.region?.let { append(it).append(" \u00B7 ") }
                    append(formatter.format(Date(visited.timestamp)))
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (visited.imageUrl != null) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
