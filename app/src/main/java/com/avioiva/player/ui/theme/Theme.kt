package com.avioiva.player.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Fixed "AVIOIVA" palette used on Android 8-11 (dynamic color needs 12+).
private val AVIOIVATeal = Color(0xFF5EEAD4)
private val AVIOIVASky = Color(0xFF38BDF8)
private val AVIOIVANight = Color(0xFF10141B)
private val AVIOIVASurface = Color(0xFF1C2733)

private val AVIOIVADarkColors = darkColorScheme(
    primary = AVIOIVATeal,
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFF9CF2E5),
    secondary = AVIOIVASky,
    onSecondary = Color(0xFF00344D),
    secondaryContainer = Color(0xFF004B6B),
    onSecondaryContainer = Color(0xFFC2E8FF),
    tertiary = Color(0xFFB8C3FF),
    onTertiary = Color(0xFF253B8F),
    background = AVIOIVANight,
    onBackground = Color(0xFFE1E3E8),
    surface = AVIOIVANight,
    onSurface = Color(0xFFE1E3E8),
    surfaceVariant = AVIOIVASurface,
    onSurfaceVariant = Color(0xFFBFC9D4),
    outline = Color(0xFF8994A0),
    surfaceContainer = Color(0xFF18222D),
    surfaceContainerHigh = Color(0xFF202B38),
    surfaceContainerHighest = Color(0xFF283443),
)

/**
 * Dark-first Material You theme: always dark surfaces; on Android 12+ the
 * accent colors are derived from the user's wallpaper.
 */
@Composable
fun AVIOIVATheme(content: @Composable () -> Unit) {
    val colorScheme =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(LocalContext.current)
        } else {
            AVIOIVADarkColors
        }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
