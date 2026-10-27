package cl.higrow.monitor.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState
import cl.higrow.monitor.ble.ConnectionState
import cl.higrow.monitor.ble.ConnectionStatus
import cl.higrow.monitor.ble.DiscoveredDevice
import cl.higrow.monitor.data.*
import kotlin.math.sin

// Datos exclusivos del editor de diseño. MainActivity nunca accede a estas muestras.
private fun previewState(connected: Boolean): MonitorUiState {
    if (!connected) return MonitorUiState()
    val samples = (0 until 32).map { index ->
        val soil = 1820 + (sin(index / 3.0) * 70).toInt()
        SensorData(24.6f + sin(index / 4.0).toFloat() * 0.7f,
            62f + sin(index / 5.0).toFloat() * 3f, soil,
            1500 + (sin(index / 3.0) * 120).toInt(), SoilState.fromAdc(soil),
            1_790_000_000_000L + index * 2000L, index * 2000L)
    }
    return MonitorUiState(ConnectionState(ConnectionStatus.CONNECTED, "HiGrow-ESP32",
        "Notificaciones habilitadas · cada 2 s", deviceAddress = "24:6F:28:3A:1B:7C"),
        readings = SensorReadings(samples.last(), samples), ageSeconds = 2)
}

@Composable
private fun PanelPreview(connected: Boolean = true, tab: Int = 0, detail: SensorMetric? = null,
    sampleState: MonitorUiState? = null) {
    HiGrowTheme {
        Column {
            Text("VISTA PREVIA · DATOS DE DISEÑO", color = Color.White, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth().background(ForestGreen).padding(4.dp))
            Box(Modifier.weight(1f)) {
                HiGrowApp(sampleState ?: previewState(connected), {}, {}, {}, {}, {}, {}, initialTab = tab, initialDetail = detail)
            }
        }
    }
}

@Preview(name = "01 Inicio vacío", group = "Paneles HiGrow", widthDp = 393, heightDp = 800)
@Composable fun DashboardEmptyPreview() { PanelPreview(connected = false) }
@Preview(name = "02 Inicio conectado", group = "Paneles HiGrow", widthDp = 393, heightDp = 800)
@Composable fun DashboardConnectedPreview() { PanelPreview() }
@Preview(name = "03 Gráficos", group = "Paneles HiGrow", widthDp = 393, heightDp = 800)
@Composable fun GraphsPreview() { PanelPreview(tab = 1) }
@Preview(name = "04 Detalle", group = "Paneles HiGrow", widthDp = 393, heightDp = 800)
@Composable fun DetailPreview() { PanelPreview(detail = SensorMetric.TEMPERATURE) }
@Preview(name = "05 Ajustes", group = "Paneles HiGrow", widthDp = 393, heightDp = 800)
@Composable fun SettingsPreview() { PanelPreview(tab = 3) }
@Preview(name = "06 Historial", group = "Paneles HiGrow", widthDp = 393, heightDp = 800)
@Composable fun HistoryPreview() { PanelPreview(tab = 2) }
@Preview(name = "07 Modo oscuro", group = "Accesibilidad", widthDp = 393, heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable fun DarkPreview() { PanelPreview() }
@Preview(name = "08 Texto grande", group = "Accesibilidad", widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable fun LargeFontPreview() { PanelPreview() }

@Preview(name = "10 Tablet · Inicio horizontal", group = "Tablet", widthDp = 1333, heightDp = 800)
@Composable fun TabletDashboardPreview() { PanelPreview() }
@Preview(name = "11 Tablet · Gráficos horizontal", group = "Tablet", widthDp = 1333, heightDp = 800)
@Composable fun TabletGraphsPreview() { PanelPreview(tab = 1) }
@Preview(name = "12 Tablet · Gráficos vertical", group = "Tablet", widthDp = 800, heightDp = 1333)
@Composable fun TabletPortraitGraphsPreview() { PanelPreview(tab = 1) }
@Preview(name = "13 Tablet · Detalle", group = "Tablet", widthDp = 1333, heightDp = 800)
@Composable fun TabletDetailPreview() { PanelPreview(detail = SensorMetric.TEMPERATURE) }
@Preview(name = "14 Tablet · Historial", group = "Tablet", widthDp = 1333, heightDp = 800)
@Composable fun TabletHistoryPreview() { PanelPreview(tab = 2) }
@Preview(name = "15 Tablet · Ajustes", group = "Tablet", widthDp = 1333, heightDp = 800)
@Composable fun TabletSettingsPreview() { PanelPreview(tab = 3) }
@Preview(name = "16 Ventana estrecha · Gráficos", group = "Tablet", widthDp = 600, heightDp = 800)
@Composable fun SplitWindowGraphsPreview() { PanelPreview(tab = 1) }
@Preview(name = "17 Tablet · Texto grande", group = "Tablet", widthDp = 960, heightDp = 600, fontScale = 1.5f)
@Composable fun TabletLargeFontPreview() { PanelPreview(tab = 1) }
@Preview(name = "18 Teléfono horizontal", group = "Accesibilidad", widthDp = 850, heightDp = 393)
@Composable fun LandscapePhonePreview() { PanelPreview(tab = 1) }

@Preview(name = "19 Tablet · Inicio sin conexión", group = "Tablet", widthDp = 1333, heightDp = 800)
@Composable fun TabletDashboardEmptyPreview() { PanelPreview(connected = false) }
@Preview(name = "20 Tablet · Inicio vertical", group = "Tablet", widthDp = 800, heightDp = 1333)
@Composable fun TabletPortraitDashboardPreview() { PanelPreview() }
@Preview(name = "21 Tablet · Inicio texto grande", group = "Tablet", widthDp = 1333, heightDp = 800, fontScale = 1.5f)
@Composable fun TabletDashboardLargeFontPreview() { PanelPreview() }

private fun previewForMood(mood: PlantMood): MonitorUiState {
    if (mood == PlantMood.OFFLINE) return MonitorUiState()
    val state = previewState(true)
    if (mood == PlantMood.WAITING) return state.copy(readings = SensorReadings())
    if (mood == PlantMood.STALE) return state.copy(stale = true, ageSeconds = 12)
    val history = state.readings.history.map { data -> when (mood) {
        PlantMood.THIRSTY -> data.copy(soilAdc = 2900, soilState = SoilState.DRY)
        PlantMood.HOT -> data.copy(temperatureC = 36f)
        PlantMood.COLD -> data.copy(temperatureC = 10f)
        PlantMood.LOW_LIGHT -> data.copy(lightAdc = 4095)
        PlantMood.DRY_AIR -> data.copy(humidityPercent = 20f)
        PlantMood.HUMID_AIR -> data.copy(humidityPercent = 90f)
        PlantMood.INCOMPLETE -> data.copy(temperatureC = null, humidityPercent = null)
        else -> data
    } }
    return state.copy(readings = SensorReadings(history.last(), history))
}

@Preview(name = "22 Brote · Sed", group = "Brote", widthDp = 393, heightDp = 850)
@Composable fun BroteThirstyPreview() { PanelPreview(sampleState = previewForMood(PlantMood.THIRSTY)) }
@Preview(name = "23 Brote · Sin datos", group = "Brote", widthDp = 393, heightDp = 850)
@Composable fun BroteOfflinePreview() { PanelPreview(connected = false) }
@Preview(name = "24 Brote · Texto grande", group = "Brote", widthDp = 320, heightDp = 720, fontScale = 1.5f)
@Composable fun BroteLargeTextPreview() { PanelPreview() }

@Preview(name = "25 Brote · Expresiones", group = "Brote", widthDp = 393, heightDp = 700)
@Composable fun BroteExpressionsPreview() {
    HiGrowTheme {
        Column(Modifier.fillMaxSize().background(Color(0xFF143F2E)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("VISTA PREVIA · DATOS DE DISEÑO", color = Color.White, style = MaterialTheme.typography.labelSmall)
            PlantMood.entries.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { mood ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            PlantFace(mood, mood.name, Modifier.size(102.dp))
                            Text(previewForMood(mood).plantAssessment().title, color = Color.White,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Preview(name = "26 Brote · Interacción", group = "Brote", widthDp = 393, heightDp = 850)
@Composable fun BroteInteractivePreview() {
    var mood by remember { mutableStateOf(PlantMood.HAPPY) }
    HiGrowTheme {
        Column {
            Text("VISTA PREVIA · DATOS DE DISEÑO", color = Color.White,
                modifier = Modifier.fillMaxWidth().background(ForestGreen).padding(4.dp), style = MaterialTheme.typography.labelSmall)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(PlantMood.HAPPY, PlantMood.THIRSTY, PlantMood.LOW_LIGHT, PlantMood.HOT,
                    PlantMood.INCOMPLETE, PlantMood.OFFLINE).forEach { choice ->
                    FilterChip(mood == choice, { mood = choice }, label = { Text(previewForMood(choice).plantAssessment().title) })
                }
            }
            Box(Modifier.weight(1f)) { HiGrowApp(previewForMood(mood), {}, {}, {}, {}, {}, {}) }
        }
    }
}

@Preview(name = "09 Selector BLE", group = "Paneles HiGrow", widthDp = 393, heightDp = 600)
@Composable fun BleSelectorPreview() {
    HiGrowTheme {
        Column(Modifier.fillMaxSize().background(ForestGreen), verticalArrangement = Arrangement.Bottom) {
            Text("VISTA PREVIA · DATOS DE DISEÑO", color = Color.White,
                style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(12.dp))
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                Box(Modifier.padding(top = 12.dp)) {
                    ScanScreen(MonitorUiState(devices = listOf(
                        DiscoveredDevice("24:6F:28:3A:1B:7C", "HiGrow-ESP32", -52),
                        DiscoveredDevice("24:6F:28:3A:1B:7D", "HiGrow-ESP32", -71),
                    )), {}, {}, {}, {})
                }
            }
        }
    }
}
