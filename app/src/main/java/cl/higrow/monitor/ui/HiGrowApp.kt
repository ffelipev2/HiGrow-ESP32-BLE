package cl.higrow.monitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState
import cl.higrow.monitor.data.LightThresholds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiGrowApp(state: MonitorUiState, onScan: () -> Unit, onStopScan: () -> Unit,
    onConnect: (String) -> Unit, onDisconnect: () -> Unit, onThresholds: (LightThresholds) -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Spa, null, tint = MaterialTheme.colorScheme.primary)
                    Text("HiGrow Monitor", style = MaterialTheme.typography.titleLarge)
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected == 0, { selected = 0 }, { Icon(Icons.Outlined.Spa, null) }, label = { Text("Mi planta") })
                NavigationBarItem(selected == 1, { selected = 1 }, { Icon(Icons.Outlined.Bluetooth, null) }, label = { Text("Conexión") })
            }
        }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 1100.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) {
                if (selected == 0) DashboardScreen(state, { selected = 1 }, { settings = true })
                else ScanScreen(state, onScan, onStopScan,
                    onConnect = { address -> onConnect(address); selected = 0 }, onDisconnect)
            }
        }
    }
    if (settings) ThresholdDialog(state.lightThresholds, { settings = false }) {
        onThresholds(it); settings = false
    }
}

@Composable
private fun ThresholdDialog(current: LightThresholds, onDismiss: () -> Unit, onSave: (LightThresholds) -> Unit) {
    var veryLow by remember { mutableStateOf(current.veryLow.toString()) }
    var low by remember { mutableStateOf(current.low.toString()) }
    var medium by remember { mutableStateOf(current.medium.toString()) }
    val valid = runCatching { LightThresholds(veryLow.toInt(), low.toInt(), medium.toInt()) }.getOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Umbrales de luminosidad") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Escala Arduino 0–1023. Un valor menor indica más luz. Solo cambia la clasificación en esta app.")
            OutlinedTextField(veryLow, { veryLow = it }, label = { Text("Muy Baja desde") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(low, { low = it }, label = { Text("Baja desde") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(medium, { medium = it }, label = { Text("Media desde") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("Muy Alta: inferior a Media. Debe cumplirse: 0 < Media < Baja < Muy Baja ≤ 1023.",
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { veryLow = "1000"; low = "400"; medium = "120" }) { Text("Restablecer valores iniciales") }
        }
    }, confirmButton = { TextButton(enabled = valid != null, onClick = { valid?.let(onSave) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
