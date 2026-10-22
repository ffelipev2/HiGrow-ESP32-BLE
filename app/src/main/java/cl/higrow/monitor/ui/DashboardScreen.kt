package cl.higrow.monitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState
import cl.higrow.monitor.ble.ConnectionStatus
import cl.higrow.monitor.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(state: MonitorUiState, onConnection: () -> Unit, onSettings: () -> Unit) {
    val data = state.readings.latest
    val connected = state.connection.status == ConnectionStatus.CONNECTED
    val accent = MaterialTheme.colorScheme.primary
    val warm = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFE5B896) else Color(0xFF9B5A31)
    val water = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFA4D0DA) else Color(0xFF3A7481)
    val soilColor = when (data?.soilState) {
        SoilState.DRY -> warm
        SoilState.SEMI_WET -> if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFDAC68B) else Color(0xFF83671E)
        else -> accent
    }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Un espacio para crecer", style = MaterialTheme.typography.headlineMedium)
            Text("Observa el ambiente de tu planta, en tiempo real.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ElevatedCard(onClick = onConnection) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (connected) Icons.Outlined.BluetoothConnected else Icons.Outlined.Bluetooth, null,
                        tint = if (connected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("BLE · ${state.connection.status.label}", style = MaterialTheme.typography.titleMedium)
                }
                Text(state.connection.deviceName ?: "Ningún HiGrow conectado")
                Text(state.connection.message, style = MaterialTheme.typography.bodySmall)
                val time = data?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it.receivedAtMillis)) }
                Text(when {
                    !connected -> "Busca un dispositivo para comenzar →"
                    data == null && state.stale -> "Sin datos desde hace más de 6 s. Revisa el ESP32 o vuelve a conectar."
                    data == null -> "Esperando la primera medición…"
                    state.stale -> "Datos desactualizados · última recepción $time · hace ${state.ageSeconds} s"
                    else -> "● Recibiendo datos · $time · hace ${state.ageSeconds ?: 0} s"
                }, style = MaterialTheme.typography.labelMedium,
                    color = if (state.stale) MaterialTheme.colorScheme.error else accent)
            }
        }
        state.readings.packetError?.let {
            Text("Paquete rechazado: $it", color = MaterialTheme.colorScheme.error)
        }
        if (connected && data != null && data.temperatureC == null) {
            Text("El DHT11 no entregó una lectura válida. Sustrato y luz siguen disponibles.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        val cards: List<@Composable (Modifier) -> Unit> = listOf(
            { modifier -> SensorCard("Temperatura ambiental", data?.temperatureC?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—",
                "°C", if (data == null) "Esperando datos" else if (data.temperatureC == null) "DHT11 sin lectura" else "DHT11 · ambiente",
                Icons.Outlined.Thermostat, warm, modifier) },
            { modifier -> SensorCard("Humedad ambiental", data?.humidityPercent?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—",
                "% HR", if (data == null) "Esperando datos" else if (data.humidityPercent == null) "DHT11 sin lectura" else "Humedad relativa del aire",
                Icons.Outlined.WaterDrop, water, modifier) },
            { modifier -> SensorCard("Humedad del sustrato", data?.soilAdc?.toString() ?: "—", "ADC filtrado · GPIO32",
                data?.soilState?.label ?: "Esperando datos", Icons.Outlined.Spa, soilColor, modifier) },
            { modifier -> SensorCard("Luminosidad", data?.lightAdc?.toString() ?: "—", "ADC · GPIO35",
                data?.let { lightState(it.lightAdc, state.lightThresholds).label } ?: "Esperando datos",
                Icons.Outlined.WbSunny, warm, modifier) },
        )
        ResponsiveRows(cards)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Últimas mediciones", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onSettings) { Text("Umbrales LDR") }
        }
        Text("Hasta 60 muestras de esta conexión · ejes ajustados a los datos",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val history = state.readings.history
        val charts: List<@Composable (Modifier) -> Unit> = listOf(
            { SensorChart("Temperatura", "°C", history, warm, it) { d -> d.temperatureC } },
            { SensorChart("Humedad ambiental", "% HR", history, water, it) { d -> d.humidityPercent } },
            { SensorChart("Sustrato", "ADC", history, accent, it) { d -> d.soilAdc.toFloat() } },
            { SensorChart("Luminosidad", "ADC", history, warm, it) { d -> d.lightAdc.toFloat() } },
        )
        ResponsiveRows(charts, minPairWidth = 720)
    }
}

@Composable
private fun ResponsiveRows(items: List<@Composable (Modifier) -> Unit>, minPairWidth: Int = 380) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= minPairWidth.dp && fontScale <= 1.35f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { content -> content(Modifier.weight(1f)) }
                }
            }
        }
    }
}
