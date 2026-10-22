package cl.higrow.monitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState
import cl.higrow.monitor.ble.ConnectionStatus

@Composable
fun ScanScreen(state: MonitorUiState, onScan: () -> Unit, onStopScan: () -> Unit,
    onConnect: (String) -> Unit, onDisconnect: () -> Unit) {
    val status = state.connection.status
    val busy = status in listOf(ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tu conexión con la naturaleza", style = MaterialTheme.typography.headlineSmall)
        Text("Enciende tu HiGrow y mantenlo cerca. Recibirás sus mediciones por Bluetooth.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.AutoMirrored.Outlined.BluetoothSearching, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp))
                Text(state.connection.status.label, style = MaterialTheme.typography.titleLarge)
                Text(state.connection.message)
                if (busy || status == ConnectionStatus.SCANNING) LinearProgressIndicator(Modifier.fillMaxWidth())
                when {
                    status == ConnectionStatus.CONNECTED -> OutlinedButton(onClick = onDisconnect) { Text("Desconectar") }
                    busy -> OutlinedButton(onClick = onDisconnect) { Text("Cancelar conexión") }
                    status == ConnectionStatus.SCANNING -> OutlinedButton(onClick = onStopScan) { Text("Detener búsqueda") }
                    else -> Button(onClick = onScan) { Text("Buscar HiGrow") }
                }
            }
        }
        if (!busy && status != ConnectionStatus.CONNECTED) state.devices.forEach { device ->
            OutlinedCard(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(device.name, style = MaterialTheme.typography.titleMedium)
                    Text("${device.address} · ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { onConnect(device.address) }) { Text("Conectar") }
                }
            }
        }
        Text("Sin WiFi ni nube. La conexión se cierra al salir de la app; al volver puedes conectar otra vez.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
