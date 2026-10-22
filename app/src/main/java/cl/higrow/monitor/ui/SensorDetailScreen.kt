package cl.higrow.monitor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState
import cl.higrow.monitor.data.*

@Composable
fun SensorDetailScreen(state: MonitorUiState, selected: SensorMetric) {
    val metrics = listOf(selected) + SensorMetric.entries.filterNot { it == selected }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.stale) Notice("Estas mediciones están desactualizadas.")
        metrics.forEach { metric ->
            val data = state.readings.latest
            val history = state.readings.history
            val stats = MeasurementStats.from(history.map(metric::value))
            OutlinedCard(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = metric.tint(), shape = RoundedCornerShape(12.dp)) {
                            Icon(metric.icon, null, Modifier.padding(9.dp).size(28.dp), tint = metric.ink())
                        }
                        Column(Modifier.weight(1f)) {
                            Text(metric.title, style = MaterialTheme.typography.titleSmall)
                            Text(metric.display(data), style = MaterialTheme.typography.headlineMedium)
                        }
                        if (metric == SensorMetric.SOIL && data != null) StateBadge(data.soilState.label, data.soilState)
                    }
                    if (history.mapNotNull(metric::value).isNotEmpty()) {
                        SensorSparkline(history, metric, Modifier.fillMaxWidth().height(48.dp), dots = true)
                    } else Text("Esperando mediciones válidas", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (metric == SensorMetric.SOIL) SoilScale(data?.soilAdc)
                    if (metric == SensorMetric.LIGHT && data != null) StateBadge(lightState(data.lightAdc, state.lightThresholds).label)
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Mínima" to stats?.minimum, "Máxima" to stats?.maximum, "Promedio" to stats?.average).forEach { (label, value) ->
                            Column(Modifier.weight(1f).padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(metric.format(value) + " " + metric.unit, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Text("${stats?.count ?: 0} lecturas válidas en esta conexión", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
@Composable private fun SoilScale(adc: Int?) {
    val marker = MaterialTheme.colorScheme.onSurface
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.fillMaxWidth().height(12.dp)) {
            Row(Modifier.fillMaxSize().clip(RoundedCornerShape(50))) {
                Box(Modifier.weight(1701f).fillMaxHeight().background(Color(0xFF69BE52)))
                Box(Modifier.weight(399f).fillMaxHeight().background(Color(0xFFFFD96A)))
                Box(Modifier.weight(1996f).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
            }
            if (adc != null) Canvas(Modifier.fillMaxSize()) {
                val x = (adc / 4095f * size.width).coerceIn(2.dp.toPx(), size.width - 2.dp.toPx())
                drawLine(marker, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("Húmedo" to "0–1700", "Semi Húmedo" to "1701–2099", "Seco" to "2100–4095").forEach { (label, range) ->
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(range, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
