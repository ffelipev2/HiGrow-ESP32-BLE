package cl.higrow.monitor.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState
import cl.higrow.monitor.ble.ConnectionStatus

@Composable
fun ScanScreen(state: MonitorUiState, onScan: () -> Unit, onStopScan: () -> Unit,
    onConnect: (String) -> Unit, onDisconnect: () -> Unit, onClose: () -> Unit = {}) {
    val status = state.connection.status
    val scanning = status == ConnectionStatus.SCANNING
    val connected = status == ConnectionStatus.CONNECTED
    val busy = status in listOf(ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Bluetooth, null, tint = MaterialTheme.colorScheme.primary)
            Text("Dispositivos BLE", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClose) { Icon(Icons.Outlined.Close, "Cerrar dispositivos") }
        }
        Text(if (scanning) "Buscando dispositivos HiGrow cercanos…" else state.connection.message,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (scanning || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (connected) {
            OutlinedCard(shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LeafMark()
                    Column(Modifier.weight(1f)) {
                        Text(state.connection.deviceName ?: "HiGrow-ESP32", style = MaterialTheme.typography.titleSmall)
                        state.connection.deviceAddress?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Text("Conectado", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        state.readings.latest?.let { data ->
                            Text("Última recepción: ${readingTime(data.receivedAtMillis)}",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else if (state.devices.isEmpty() && !busy) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Sensors, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (scanning) "Enciende tu HiGrow y mantenlo cerca" else "Aún no hay dispositivos",
                        style = MaterialTheme.typography.titleSmall)
                    Text("Aquí aparecerán los periféricos compatibles con HiGrow Monitor.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (!connected) LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.devices, key = { it.address }) { device ->
                OutlinedCard(onClick = { if (!busy) onConnect(device.address) }, enabled = !busy,
                    shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LeafMark(38)
                        Column(Modifier.weight(1f)) {
                            Text(device.name, style = MaterialTheme.typography.titleSmall)
                            Text(device.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            SignalBars(device.rssi)
                            Text("${device.rssi} dBm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            when {
                connected -> OutlinedButton(onDisconnect) { Text("Desconectar") }
                busy -> OutlinedButton(onDisconnect) { Text("Cancelar conexión") }
                scanning -> OutlinedButton(onStopScan) { Text("Detener búsqueda") }
                else -> Button(onScan) { Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Buscar de nuevo") }
            }
        }
        Text("La señal mostrada corresponde al último resultado de búsqueda.",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun SignalBars(rssi: Int) {
    val active = when { rssi >= -60 -> 4; rssi >= -70 -> 3; rssi >= -80 -> 2; else -> 1 }
    val color = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.size(26.dp, 23.dp).semantics { contentDescription = "Señal de búsqueda $rssi dBm" }) {
        repeat(4) { index ->
            val barWidth = size.width / 6f
            val height = size.height * (index + 1) / 4
            drawRoundRect(if (index < active) color else inactive,
                Offset(index * size.width / 4, size.height - height), Size(barWidth, height), CornerRadius(1.dp.toPx()))
        }
    }
}
