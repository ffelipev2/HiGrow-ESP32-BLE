package cl.higrow.monitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState
import cl.higrow.monitor.ble.ConnectionStatus

@Composable
fun DashboardScreen(state: MonitorUiState, onConnection: () -> Unit, onSensor: (SensorMetric) -> Unit,
    onDisconnect: () -> Unit) {
    val viewport = LocalScreenViewport.current
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    if (viewport.width < 600.dp && viewport.width.value / fontScale >= 288f && fontScale <= 1.25f) {
        DashboardOverview(state, onConnection, onSensor)
        return
    }
    val wide = viewport.width.value / fontScale >= 900f && viewport.height >= 480.dp
    if (wide) {
        // Measure against the viewport before the page scrolls, so the sensor area
        // uses both dimensions instead of leaving an empty region below one row.
        val height = (viewport.height - 12.dp).coerceAtLeast((440 * fontScale).dp + 12.dp)
        Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.width((300 * fontScale).dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PlantCompanionCard(state, compact = true)
                ConnectionPanel(state, onConnection, stackActions = true)
                DashboardAlerts(state)
                DeviceBar(state, onConnection)
                ConnectionFooter(state, onDisconnect)
            }
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SensorMetric.entries.chunked(2).forEach { row ->
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { metric ->
                            SensorCard(metric, state.readings.latest, state.readings.history, state.lightThresholds,
                                { onSensor(metric) }, Modifier.weight(1f).fillMaxHeight(), expanded = true)
                        }
                    }
                }
            }
        }
        return
    }
    val expandedCards = viewport.width >= 600.dp
    val alertCount = listOf(state.stale, state.readings.packetError != null,
        state.readings.latest?.let { it.temperatureC == null } == true).count { it }
    val cardHeight = if (expandedCards) {
        ((viewport.height - ((500 + alertCount * 64) * fontScale).dp) / 2).coerceAtLeast(220.dp)
    } else 180.dp
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        PlantCompanionCard(state)
        ConnectionPanel(state, onConnection)
        DashboardAlerts(state)
        val data = state.readings.latest
        val cards: List<@Composable (Modifier) -> Unit> = SensorMetric.entries.map { metric ->
            { modifier -> SensorCard(metric, data, state.readings.history, state.lightThresholds,
                { onSensor(metric) }, modifier, expanded = expandedCards, minHeight = cardHeight) }
        }
        ResponsiveRows(cards, minCellWidth = 240, compactPairs = true)
        DeviceBar(state, onConnection)
        ConnectionFooter(state, onDisconnect)
    }
}

private enum class OverviewSlot { SUMMARY, SENSORS }

@Composable
private fun DashboardOverview(state: MonitorUiState, onConnection: () -> Unit, onSensor: (SensorMetric) -> Unit) {
    val viewport = LocalScreenViewport.current
    // Measure the actual summary first: combined conditions and errors can wrap.
    // Give the remaining viewport height to the four cards in the same layout pass.
    // Minimum readable card sizes can still make the surrounding page scroll on
    // unusually short windows; neither values nor warnings are clipped to fit.
    SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
        val childConstraints = constraints.copy(minWidth = constraints.maxWidth, minHeight = 0, maxHeight = Constraints.Infinity)
        val gap = 8.dp.roundToPx()
        val summary = subcompose(OverviewSlot.SUMMARY) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlantCompanionCard(state, dense = true)
                ConnectionStrip(state, onConnection)
                DashboardAlerts(state)
                if (state.connection.status == ConnectionStatus.ERROR) Notice(state.connection.message)
            }
        }.single().measure(childConstraints)
        // HiGrowApp adds a final 12 dp spacer after the screen content.
        val cardHeight = ((viewport.height.roundToPx() - summary.height - gap * 2 - 12.dp.roundToPx()) / 2)
            .coerceAtLeast(148.dp.roundToPx()).toDp()
        val sensors = subcompose(OverviewSlot.SENSORS) {
            val cards: List<@Composable (Modifier) -> Unit> = SensorMetric.entries.map { metric ->
                { modifier -> SensorCard(metric, state.readings.latest, state.readings.history, state.lightThresholds,
                    { onSensor(metric) }, modifier, minHeight = cardHeight, dense = true) }
            }
            ResponsiveRows(cards, minCellWidth = 140, spacing = 8.dp)
        }.single().measure(childConstraints)
        layout(constraints.maxWidth, summary.height + gap + sensors.height) {
            summary.placeRelative(0, 0)
            sensors.placeRelative(0, summary.height + gap)
        }
    }
}

@Composable
private fun ConnectionStrip(state: MonitorUiState, onConnection: () -> Unit) {
    val connected = state.connection.status == ConnectionStatus.CONNECTED
    val busy = state.connection.status in listOf(ConnectionStatus.SCANNING, ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING)
    val dot = when {
        connected && !state.stale -> MaterialTheme.colorScheme.primary
        busy -> Color(0xFFE6A000)
        else -> MaterialTheme.colorScheme.error
    }
    Surface(onClick = onConnection, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(8.dp).background(dot, CircleShape))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (connected) state.connection.deviceName ?: "HiGrow-ESP32" else state.connection.status.label,
                        style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (connected) when {
                        state.stale -> "Conectado · Lectura desactualizada"
                        state.ageSeconds != null -> "Conectado · hace ${state.ageSeconds} s"
                        else -> "Conectado · Esperando datos"
                    } else if (busy) "Toca para ver la conexión" else "Toca para buscar tu HiGrow",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (connected) {
                    Icon(Icons.Outlined.BluetoothConnected, "Gestionar conexión BLE", tint = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
                } else TextButton(onConnection) {
                    Text(if (busy) "Ver BLE" else "Conectar", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().height(3.dp))
        }
    }
}

@Composable
private fun DashboardAlerts(state: MonitorUiState) {
    if (state.stale) Notice("Lecturas desactualizadas. Revisa la conexión con tu HiGrow.")
    state.readings.packetError?.let { Notice("Paquete rechazado: $it") }
    val data = state.readings.latest
    if (data != null && data.temperatureC == null) Notice("DHT11 sin lectura válida. Sustrato y luz siguen disponibles.")
}

@Composable
private fun DeviceBar(state: MonitorUiState, onConnection: () -> Unit) {
    OutlinedCard(onConnection, shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(11.dp)) {
                Icon(Icons.Outlined.Bluetooth, null, Modifier.padding(9.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text("Dispositivos BLE", style = MaterialTheme.typography.titleSmall)
                Text(if (state.connection.status == ConnectionStatus.CONNECTED) "Gestionar dispositivo conectado" else "Buscar dispositivos cercanos",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConnectionFooter(state: MonitorUiState, onDisconnect: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (state.connection.status == ConnectionStatus.CONNECTED) {
            TextButton(onDisconnect) { Text("Desconectar dispositivo") }
        } else {
            Text("Tus plantas, cerca. Monitoreo local por Bluetooth.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 3.dp))
        }
    }
}

@Composable
private fun ConnectionPanel(state: MonitorUiState, onConnection: () -> Unit, stackActions: Boolean = false) {
    val connection = state.connection
    val connected = connection.status == ConnectionStatus.CONNECTED
    val busy = connection.status in listOf(ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING, ConnectionStatus.SCANNING)
    val dot = if (connected && !state.stale) MaterialTheme.colorScheme.primary else if (busy) Color(0xFFE6A000) else MaterialTheme.colorScheme.error
    val largeFont = stackActions || LocalDensity.current.fontScale > 1.25f
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(10.dp).background(dot, CircleShape))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(connection.status.label, style = MaterialTheme.typography.titleSmall)
                    if (connected) {
                        Text(connection.deviceName ?: "HiGrow-ESP32", style = MaterialTheme.typography.titleSmall)
                        connection.deviceAddress?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else Text(if (busy) connection.message else "Conecta tu dispositivo para comenzar",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!largeFont) {
                    if (connected) ReceptionStatus(state) else ConnectButton(onConnection, busy)
                }
            }
            if (largeFont) {
                if (connected) ReceptionStatus(state) else ConnectButton(onConnection, busy)
            }
            if (connected && state.readings.latest != null) Text("Última recepción: ${readingTime(state.readings.latest.receivedAtMillis)}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (connection.status == ConnectionStatus.ERROR) Text(connection.message, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().height(3.dp))
        }
        }
}
@Composable private fun ConnectButton(onClick: () -> Unit, busy: Boolean) {
    Button(onClick, contentPadding = PaddingValues(horizontal = 13.dp, vertical = 10.dp), shape = RoundedCornerShape(50)) {
        Icon(Icons.Outlined.Bluetooth, null, Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (busy) "Ver conexión" else "Conectar BLE", style = MaterialTheme.typography.labelMedium)
    }
}
@Composable private fun ReceptionStatus(state: MonitorUiState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(horizontalAlignment = Alignment.End) {
            Text("Última lectura", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (state.stale) "Desactualizada" else state.ageSeconds?.let { "hace $it s" } ?: "Esperando…",
                style = MaterialTheme.typography.labelSmall, color = if (state.stale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.BluetoothConnected, "Conexión BLE activa", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
    }
}
@Composable fun Notice(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(13.dp)) {
        Text(message, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer)
    }
}
