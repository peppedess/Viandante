package com.peppedess.viandante

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.peppedess.viandante.ui.HistoryScreen
import com.peppedess.viandante.ui.HomeScreen
import com.peppedess.viandante.ui.PoiDetailScreen
import com.peppedess.viandante.ui.theme.ViandanteTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ViandanteTheme {
                val vm: MainViewModel = viewModel()
                var hasPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            this, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(
                                this, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->
                    hasPermission = result.values.any { it }
                }
                if (hasPermission) {
                    LaunchedEffect(Unit) { vm.startTracking() }
                    AppNav(vm)
                } else {
                    PermissionScreen {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNav(vm: MainViewModel) {
    val nav = rememberNavController()
    SharedTransitionLayout {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onPoiClick = { index -> nav.navigate("poi/$index") },
                    onHistoryClick = { nav.navigate("history") },
                    sharedScope = this@SharedTransitionLayout,
                    animScope = this
                )
            }
            composable(
                route = "poi/{index}",
                arguments = listOf(navArgument("index") { type = NavType.IntType })
            ) { entry ->
                val index = entry.arguments?.getInt("index") ?: 0
                PoiDetailScreen(
                    vm = vm,
                    index = index,
                    onBack = { nav.popBackStack() },
                    sharedScope = this@SharedTransitionLayout,
                    animScope = this
                )
            }
            composable("history") {
                HistoryScreen(vm = vm, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.primaryContainer, colors.surface)))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "\uD83E\uDDED", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Viandante",
                style = MaterialTheme.typography.displayLarge,
                color = colors.onSurface
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "La guida turistica che viaggia con te. Attiva la posizione per scoprire dove sei e cosa c'\u00E8 di bello intorno a te.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = colors.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Attiva la posizione", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
