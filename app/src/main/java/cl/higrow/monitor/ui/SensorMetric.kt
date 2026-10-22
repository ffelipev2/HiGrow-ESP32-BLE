package cl.higrow.monitor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import cl.higrow.monitor.data.SensorData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SensorMetric(val title: String, val unit: String, val icon: ImageVector,
    val color: Color, val lightTint: Color, val darkTint: Color) {
    TEMPERATURE("Temperatura", "°C", Icons.Outlined.Thermostat, Color(0xFFF52F37), Color(0xFFFFEEEE), Color(0xFF382328)),
    HUMIDITY("Humedad ambiental", "%", Icons.Outlined.WaterDrop, Color(0xFF0787F5), Color(0xFFEBF5FF), Color(0xFF183042)),
    SOIL("Humedad del sustrato", "ADC", Icons.Outlined.Spa, Color(0xFF158A26), Color(0xFFF0F9E9), Color(0xFF213A24)),
    LIGHT("Luminosidad", "ADC", Icons.Outlined.WbSunny, Color(0xFFE8A300), Color(0xFFFFF8E6), Color(0xFF393322));

    fun value(data: SensorData): Float? = when (this) {
        TEMPERATURE -> data.temperatureC
        HUMIDITY -> data.humidityPercent
        SOIL -> data.soilAdc.toFloat()
        LIGHT -> data.lightAdc.toFloat()
    }
    fun format(value: Float?): String = when {
        value == null -> "—"
        this == TEMPERATURE -> String.format(Locale.getDefault(), "%.1f", value)
        value % 1f == 0f -> value.toInt().toString()
        else -> String.format(Locale.getDefault(), "%.1f", value)
    }
    fun display(data: SensorData?): String = format(data?.let(::value)) +
        if (this == TEMPERATURE || this == HUMIDITY) " $unit" else ""
    @Composable fun tint() = if (isSystemInDarkTheme()) darkTint else lightTint
    @Composable fun ink(): Color = if (!isSystemInDarkTheme()) color else when (this) {
        TEMPERATURE -> Color(0xFFFF9296)
        HUMIDITY -> Color(0xFF7EC6FF)
        SOIL -> Color(0xFF98DC91)
        LIGHT -> Color(0xFFFFD36E)
    }
}
fun readingTime(millis: Long): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))
