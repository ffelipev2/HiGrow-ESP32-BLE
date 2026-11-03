package cl.higrow.monitor.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import cl.higrow.monitor.data.*

@Composable
fun SensorCard(metric: SensorMetric, data: SensorData?, history: List<SensorData>,
    thresholds: LightThresholds, onClick: () -> Unit, modifier: Modifier = Modifier,
    expanded: Boolean = false, minHeight: Dp = 180.dp, dense: Boolean = false) {
    val tint = metric.tint()
    val ink = metric.ink()
    val surface = MaterialTheme.colorScheme.surface
    val empty = history.none { metric.value(it) != null }
    Card(onClick, modifier.heightIn(min = minHeight), shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, metric.ink().copy(alpha = 0.18f)),
        colors = CardDefaults.cardColors(containerColor = tint)) {
        val contentModifier = if (expanded || dense) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
        Box(contentModifier.background(Brush.linearGradient(listOf(tint, lerp(tint, surface, 0.36f))))) {
            if (expanded && empty) SensorEmptyArtwork(metric, ink, Modifier.matchParentSize())
            Column(contentModifier.padding(if (dense) 12.dp else if (expanded) 18.dp else 15.dp),
                verticalArrangement = Arrangement.spacedBy(if (dense) 4.dp else 7.dp)) {
                if (expanded || dense) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (dense) 6.dp else 12.dp)) {
                        Icon(metric.icon, null, tint = metric.ink(), modifier = Modifier.size(if (dense) 20.dp else 32.dp))
                        Text(metric.title, style = if (dense) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f))
                        if (expanded) Surface(shape = RoundedCornerShape(12.dp), color = ink.copy(alpha = 0.08f)) {
                            Icon(Icons.Outlined.BarChart, null, Modifier.padding(8.dp).size(18.dp), tint = ink.copy(alpha = 0.65f))
                        }
                    }
                } else {
                    Icon(metric.icon, null, tint = metric.ink(), modifier = Modifier.size(29.dp))
                    Text(metric.title, style = MaterialTheme.typography.titleSmall)
                }
                AnimatedContent(targetState = metric.display(data), label = "lectura",
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }) { value ->
                    Text(value, style = if (expanded) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                        fontSize = if (expanded) 40.sp else MaterialTheme.typography.headlineMedium.fontSize,
                        fontWeight = FontWeight.Bold)
                }
                when (metric) {
                    SensorMetric.SOIL -> if (data != null) StateBadge(data.soilState.label, soil = data.soilState)
                        else Text("ADC filtrado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SensorMetric.LIGHT -> if (data != null) StateBadge(lightState(data.lightAdc, thresholds).label)
                        else Text("ADC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> if (!dense || data == null || metric.value(data) == null) Text(
                        if (data == null) "Sin lectura" else if (metric.value(data) == null) "DHT11 sin lectura" else "DHT11",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (expanded || dense) {
                    Box(Modifier.weight(1f).fillMaxWidth().heightIn(min = if (dense) 20.dp else 48.dp)
                        .padding(top = if (dense) 2.dp else 12.dp),
                        contentAlignment = if (expanded && empty) Alignment.BottomCenter else Alignment.Center) {
                        if (expanded && empty) {
                            Surface(shape = RoundedCornerShape(50), color = surface.copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, surface.copy(alpha = 0.5f))) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.BarChart, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Sin datos para graficar", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else if (empty) {
                            Text("Sin datos para graficar", style = MaterialTheme.typography.labelSmall,
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
