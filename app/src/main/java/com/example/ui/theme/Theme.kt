package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ScanNaijaDarkColorScheme = darkColorScheme(
    primary = NeonEmerald,
    onPrimary = Color.White,
    secondary = NeonPurple,
    onSecondary = Color.White,
    tertiary = WarnOrange,
    background = SpaceNavy,
    surface = SpaceNavyDarkCard,
    onBackground = LightBlueText,
    onSurface = LightBlueText,
    error = AlertRed,
    onError = Color.White
)

// Standard light color scheme fallback matching the Professional Polish M3 lavender palette
private val ScanNaijaLightColorScheme = lightColorScheme(
    primary = NeonEmerald,
    onPrimary = Color.White,
    secondary = NeonPurple,
    onSecondary = Color.White,
    tertiary = WarnOrange,
    background = SpaceNavy,
    surface = SpaceNavyDarkCard,
    onBackground = LightBlueText,
    onSurface = LightBlueText,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Set false to render the light Professional Polish theme by default
    dynamicColor: Boolean = false, // Enforce custom Professional Polish branding
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ScanNaijaDarkColorScheme else ScanNaijaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
