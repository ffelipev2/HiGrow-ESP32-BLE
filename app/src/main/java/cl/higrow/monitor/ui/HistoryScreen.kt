package cl.higrow.monitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState

@Composable
fun HistoryScreen(state: MonitorUiState) {
    val history = state.readings.history
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Lecturas de esta conexión", style = MaterialTheme.typography.titleMedium)
        Text("Hasta 60 mediciones en memoria. Se vacían al desconectar.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (history.isEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.History, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Tu historial comienza aquí", style = MaterialTheme.typography.titleMedium)
                    Text("Conecta tu HiGrow para recibir las primeras mediciones.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        history.asReversed().forEachIndexed { index, data ->
            OutlinedCard(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(readingTime(data.receivedAtMillis), style = MaterialTheme.typography.titleSmall)
                        Text("#${history.size - index}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SensorMetric.entries.chunked(2).forEach { metrics ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            metrics.forEach { metric ->
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(metric.icon, null, Modifier.size(16.dp), tint = metric.ink())
                                    Text(metric.display(data) + if (metric.unit == "ADC") " ADC" else "",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
