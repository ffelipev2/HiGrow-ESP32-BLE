package cl.higrow.monitor.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import cl.higrow.monitor.data.*

@Composable
fun SensorCard(metric: SensorMetric, data: SensorData?, history: List<SensorData>,
    thresholds: LightThresholds, onClick: () -> Unit, modifier: Modifier = Modifier,
    expanded: Boolean = false, minHeight: Dp = 180.dp) {
    val tint = metric.tint()
    val surface = MaterialTheme.colorScheme.surface
    Card(onClick, modifier.heightIn(min = minHeight), shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, metric.ink().copy(alpha = 0.18f)),
        colors = CardDefaults.cardColors(containerColor = tint)) {
        val contentModifier = if (expanded) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
        Column(contentModifier.background(Brush.linearGradient(listOf(tint, lerp(tint, surface, 0.36f))))
            .padding(if (expanded) 20.dp else 15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (expanded) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(metric.icon, null, tint = metric.ink(), modifier = Modifier.size(32.dp))
                    Text(metric.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
            } else {
                Icon(metric.icon, null, tint = metric.ink(), modifier = Modifier.size(29.dp))
                Text(metric.title, style = MaterialTheme.typography.titleSmall)
            }
            AnimatedContent(targetState = metric.display(data), label = "lectura",
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }) { value ->
                Text(value, style = if (expanded) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
            }
            when (metric) {
                SensorMetric.SOIL -> if (data != null) StateBadge(data.soilState.label, soil = data.soilState)
                    else Text("ADC filtrado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SensorMetric.LIGHT -> if (data != null) StateBadge(lightState(data.lightAdc, thresholds).label)
                    else Text("ADC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Text(if (data == null) "Sin lectura" else if (metric.value(data) == null) "DHT11 sin lectura" else "DHT11",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (expanded) {
                Box(Modifier.weight(1f).fillMaxWidth().heightIn(min = 64.dp).padding(top = 12.dp),
                    contentAlignment = Alignment.Center) {
                    if (history.none { metric.value(it) != null }) {
                        Text("Sin datos para graficar", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else SensorSparkline(history, metric, Modifier.fillMaxSize())
                }
            } else if (history.mapNotNull(metric::value).isEmpty()) {
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
