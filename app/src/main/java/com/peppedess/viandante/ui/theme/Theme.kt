package com.peppedess.viandante.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val baseTypography = Typography()

val ViandanteTypography = baseTypography.copy(
    displayLarge = baseTypography.displayLarge.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.5).sp
    ),
    displayMedium = baseTypography.displayMedium.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1).sp
    ),
    displaySmall = baseTypography.displaySmall.copy(
        fontWeight = FontWeight.ExtraBold
    ),
    headlineMedium = baseTypography.headlineMedium.copy(
        fontWeight = FontWeight.Bold
    ),
    headlineSmall = baseTypography.headlineSmall.copy(
        fontWeight = FontWeight.Bold
    ),
    labelLarge = baseTypography.labelLarge.copy(
        letterSpacing = 1.2.sp
    )
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ViandanteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = ViandanteTypography,
        content = content
    )
}
