package cl.higrow.monitor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ForestGreen = Color(0xFF153F2C)
private val LightColors = lightColorScheme(
    primary = Color(0xFF087C23), onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F6DA), onPrimaryContainer = Color(0xFF174523),
    secondary = Color(0xFF526258), background = ForestGreen,
    surface = Color(0xFFFCFDFC), surfaceVariant = Color(0xFFF1F5F1),
    onSurface = Color(0xFF162119), onSurfaceVariant = Color(0xFF667168),
    outline = Color(0xFFAFB9B1), outlineVariant = Color(0xFFE2E8E2), error = Color(0xFFC73036),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF91DFA1), onPrimary = Color(0xFF073915),
    primaryContainer = Color(0xFF284D2D), onPrimaryContainer = Color(0xFFCDF0CD),
    secondary = Color(0xFFADC7B0), background = Color(0xFF0E251A),
    surface = Color(0xFF15221B), surfaceVariant = Color(0xFF203127),
    onSurface = Color(0xFFEDF4EC), onSurfaceVariant = Color(0xFFB2C2B4),
    outlineVariant = Color(0xFF354B3C),
)
private val HiGrowTypography = Typography(
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp),
)
@Composable fun HiGrowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = HiGrowTypography, content = content)
}
