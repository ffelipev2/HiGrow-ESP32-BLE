package cl.higrow.monitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState
import cl.higrow.monitor.ble.ConnectionStatus

@Composable
fun DashboardScreen(state: MonitorUiState, onConnection: () -> Unit, onSensor: (SensorMetric) -> Unit,
    onDisconnect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        ConnectionPanel(state, onConnection)
        if (state.stale) Notice("Lecturas desactualizadas. Revisa la conexión con tu HiGrow.")
        state.readings.packetError?.let { Notice("Paquete rechazado: $it") }
        val data = state.readings.latest
        if (data != null && data.temperatureC == null) Notice("DHT11 sin lectura válida. Sustrato y luz siguen disponibles.")
        val cards: List<@Composable (Modifier) -> Unit> = SensorMetric.entries.map { metric ->
            { modifier -> SensorCard(metric, data, state.readings.history, state.lightThresholds,
                { onSensor(metric) }, modifier) }
        }
        ResponsiveRows(cards)
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
        if (state.connection.status == ConnectionStatus.CONNECTED) {
            TextButton(onDisconnect, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Desconectar dispositivo") }
        } else {
            Text("Tus plantas, cerca. Monitoreo local por Bluetooth.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 3.dp))
        }
    }
}

@Composable
private fun ConnectionPanel(state: MonitorUiState, onConnection: () -> Unit) {
    val connection = state.connection
    val connected = connection.status == ConnectionStatus.CONNECTED
    val busy = connection.status in listOf(ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING, ConnectionStatus.SCANNING)
    val dot = if (connected && !state.stale) MaterialTheme.colorScheme.primary else if (busy) Color(0xFFE6A000) else MaterialTheme.colorScheme.error
    val largeFont = LocalDensity.current.fontScale > 1.25f
    Column(Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

@Composable
fun ResponsiveRows(items: List<@Composable (Modifier) -> Unit>, minPairWidth: Int = 320) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= minPairWidth.dp && fontScale <= 1.35f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { content -> content(Modifier.weight(1f)) }
                }
            }
        }
    }
}
