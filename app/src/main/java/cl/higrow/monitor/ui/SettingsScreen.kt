package cl.higrow.monitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.BuildConfig
import cl.higrow.monitor.MonitorUiState
import cl.higrow.monitor.ble.ConnectionStatus
import cl.higrow.monitor.data.LightThresholds

@Composable
fun SettingsScreen(state: MonitorUiState, onConnect: () -> Unit, onDisconnect: () -> Unit,
    onAutoReconnect: (Boolean) -> Unit, onThresholds: () -> Unit) {
    var about by remember { mutableStateOf(false) }
    val connected = state.connection.status == ConnectionStatus.CONNECTED
    val limits = state.lightThresholds
    val largeFont = LocalDensity.current.fontScale > 1.3f
    val twoColumns = LocalScreenViewport.current.width.value / LocalDensity.current.fontScale.coerceAtLeast(1f) >= 612f
    val sections: List<@Composable (Modifier) -> Unit> = listOf(
        { modifier -> Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Conexión", style = MaterialTheme.typography.titleMedium)
            OutlinedCard(shape = RoundedCornerShape(17.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(if (connected) "Dispositivo conectado" else "Sin dispositivo conectado",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.connection.deviceName ?: "Tu HiGrow te espera", style = MaterialTheme.typography.titleSmall)
                            state.connection.deviceAddress?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        if (!largeFont) OutlinedButton(onClick = if (connected) onDisconnect else onConnect,
                            contentPadding = PaddingValues(horizontal = 12.dp), shape = RoundedCornerShape(50)) {
                            Text(if (connected) "Desconectar" else "Conectar BLE", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (largeFont) OutlinedButton(onClick = if (connected) onDisconnect else onConnect,
                        shape = RoundedCornerShape(50)) { Text(if (connected) "Desconectar" else "Conectar BLE") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Reconectar automáticamente", style = MaterialTheme.typography.bodyMedium)
                    Text("Hasta 3 intentos al perder la conexión", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(state.autoReconnect, onAutoReconnect)
            }
            if (twoColumns) AboutSettings { about = true }
        } },
        { modifier -> Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Umbrales de luminosidad", style = MaterialTheme.typography.titleMedium)
            OutlinedCard(shape = RoundedCornerShape(17.dp)) {
                val rows = listOf(
                    Triple("Muy Baja", "≥ ${limits.veryLow}", Color(0xFFEB4141)),
                    Triple("Baja", "${limits.low}–${limits.veryLow - 1}", Color(0xFFED9900)),
                    Triple("Media", "${limits.medium}–${limits.low - 1}", Color(0xFFE8BB00)),
                    Triple("Muy Alta", "< ${limits.medium}", Color(0xFF188D27)),
                )
                rows.forEachIndexed { index, (label, range, color) ->
                    Surface(onClick = onThresholds, color = Color.Transparent) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(10.dp).background(color, CircleShape))
                            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(range, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Outlined.ChevronRight, null, Modifier.size(19.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (index < rows.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                }
            }
            Text("Escala de clasificación 0–1023. Las tarjetas muestran ADC sin procesar (0–4095).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ResponsiveRows(sections, minCellWidth = 300)
        if (!twoColumns) AboutSettings { about = true }
    }
    if (about) AlertDialog(onDismissRequest = { about = false }, icon = { Icon(Icons.Outlined.Info, null) },
        title = { Text("HiGrow Monitor") }, text = {
            Text("Versión ${BuildConfig.VERSION_NAME}\n\nMonitorea temperatura, humedad, sustrato y luminosidad con tu ESP32. Guarda hasta 60 mediciones durante la conexión.\n\nSin WiFi, Internet ni servicios en la nube.")
        }, confirmButton = { TextButton(onClick = { about = false }) { Text("Entendido") } })
}

@Composable
private fun AboutSettings(onAbout: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Acerca de", style = MaterialTheme.typography.titleMedium)
        OutlinedCard(onClick = onAbout, shape = RoundedCornerShape(17.dp)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LeafMark(38)
                Column(Modifier.weight(1f)) {
                    Text("HiGrow Monitor", style = MaterialTheme.typography.titleSmall)
                    Text("Versión ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("La conexión se cierra al salir de la app. Todas tus mediciones se reciben por Bluetooth, sin nube.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ThresholdDialog(current: LightThresholds, onDismiss: () -> Unit, onSave: (LightThresholds) -> Unit) {
    var veryLow by remember { mutableStateOf(current.veryLow.toString()) }
    var low by remember { mutableStateOf(current.low.toString()) }
    var medium by remember { mutableStateOf(current.medium.toString()) }
    val valid = runCatching { LightThresholds(veryLow.toInt(), low.toInt(), medium.toInt()) }.getOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Umbrales de luminosidad") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Un valor menor indica más luz. Estos límites cambian la clasificación en tu teléfono.")
            OutlinedTextField(veryLow, { veryLow = it }, label = { Text("Muy Baja desde") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(low, { low = it }, label = { Text("Baja desde") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(medium, { medium = it }, label = { Text("Media desde") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            if (valid == null) Text("Debe cumplirse: 0 < Media < Baja < Muy Baja ≤ 1023.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { veryLow = "1000"; low = "400"; medium = "120" }) { Text("Restablecer valores iniciales") }
        }
    }, confirmButton = { TextButton(enabled = valid != null, onClick = { valid?.let(onSave) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
