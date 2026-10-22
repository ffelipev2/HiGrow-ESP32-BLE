package cl.higrow.monitor.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.data.*

@Composable
fun SensorCard(metric: SensorMetric, data: SensorData?, history: List<SensorData>,
    thresholds: LightThresholds, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick, modifier.heightIn(min = 180.dp), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = metric.tint())) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(metric.icon, null, tint = metric.ink(), modifier = Modifier.size(29.dp))
            Text(metric.title, style = MaterialTheme.typography.titleSmall)
            AnimatedContent(targetState = metric.display(data), label = "lectura",
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }) { value ->
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            when (metric) {
                SensorMetric.SOIL -> if (data != null) StateBadge(data.soilState.label, soil = data.soilState)
                    else Text("ADC filtrado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SensorMetric.LIGHT -> if (data != null) StateBadge(lightState(data.lightAdc, thresholds).label)
                    else Text("ADC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Text(if (data == null) "Sin lectura" else if (metric.value(data) == null) "DHT11 sin lectura" else "DHT11",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (history.mapNotNull(metric::value).isEmpty()) {
                Text("Sin datos para graficar", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            } else {
                SensorSparkline(history, metric, Modifier.fillMaxWidth().height(25.dp))
            }
        }
    }
}

@Composable
fun StateBadge(label: String, soil: SoilState? = null) {
    val background = when (soil) {
        SoilState.WET -> Color(0xFFD8EFD3)
        SoilState.DRY -> Color(0xFFFFD6CB)
        else -> Color(0xFFFFE283)
    }
    Surface(color = background, contentColor = Color(0xFF273019), shape = RoundedCornerShape(50)) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}
