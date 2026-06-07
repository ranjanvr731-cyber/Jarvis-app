package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    primaryContainer = DeepSpaceNavy,
    onPrimaryContainer = CyberCyan,
    secondary = HologramBlue,
    onSecondary = Color.Black,
    secondaryContainer = Color(0x3300A2FF),
    onSecondaryContainer = TextHologram,
    background = DarkVacuum,
    onBackground = TextHologram,
    surface = TechCardGlass,
    onSurface = TextHologram,
    surfaceVariant = DeepSpaceNavy,
    onSurfaceVariant = CyberCyan,
    outline = GridOutline,
    error = Color(0xFFFF4D4D)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
