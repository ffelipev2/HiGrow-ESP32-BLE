package cl.higrow.monitor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F7056), onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EBD9), onPrimaryContainer = Color(0xFF173D28),
    secondary = Color(0xFF6A7452), background = Color(0xFFF4F6F0),
    surface = Color(0xFFFAFCF7), surfaceVariant = Color(0xFFE7ECE3),
    onSurface = Color(0xFF202B23), onSurfaceVariant = Color(0xFF566158),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA2D0AE), onPrimary = Color(0xFF103822),
    primaryContainer = Color(0xFF294E37), onPrimaryContainer = Color(0xFFD7EBD9),
    secondary = Color(0xFFC0CAA5), background = Color(0xFF111A15),
    surface = Color(0xFF19241D), surfaceVariant = Color(0xFF29362D),
    onSurface = Color(0xFFE1E9DF), onSurfaceVariant = Color(0xFFB8C6B8),
)
@Composable fun HiGrowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
