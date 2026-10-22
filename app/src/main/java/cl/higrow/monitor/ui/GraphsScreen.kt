package cl.higrow.monitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState

@Composable
fun GraphsScreen(state: MonitorUiState, onSensor: (SensorMetric) -> Unit) {
    var count by rememberSaveable { mutableIntStateOf(60) }
    var expanded by remember { mutableStateOf(false) }
    val history = state.readings.history.takeLast(count)
    val viewport = LocalScreenViewport.current
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val twoColumns = viewport.width.value / fontScale >= 572f
    // Reserve room for the selector, labels, axes, spacing and any stale-data notice.
    // On tablets both rows grow with the viewport; on phones keep the compact charts.
    val plotHeight = if (twoColumns) {
        ((viewport.height - (if (state.stale) 156.dp else 104.dp)) / 2 - (78 * fontScale).dp)
            .coerceIn(100.dp, 280.dp)
    } else 72.dp
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.widthIn(max = 360.dp).fillMaxWidth()) {
            Surface(onClick = { expanded = true }, shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Últimas $count mediciones", style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            DropdownMenu(expanded, { expanded = false }) {
                listOf(15, 30, 60).forEach { limit ->
                    DropdownMenuItem(text = { Text("Últimas $limit mediciones") }, onClick = { count = limit; expanded = false })
                }
            }
        }
        if (state.stale) Notice("Datos desactualizados. Las curvas muestran las últimas lecturas recibidas.")
        val charts: List<@Composable (Modifier) -> Unit> = SensorMetric.entries.map { metric ->
            { modifier -> SensorChart(metric, history, modifier, plotHeight = plotHeight, onClick = { onSensor(metric) }) }
        }
        ResponsiveRows(charts)
        Text("${history.size} muestras recibidas · hora del teléfono",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
