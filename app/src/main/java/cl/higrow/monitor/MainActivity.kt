package cl.higrow.monitor

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.higrow.monitor.ui.HiGrowApp
import cl.higrow.monitor.ui.HiGrowTheme

class MainActivity : ComponentActivity() {
    private val model: SensorViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HiGrowTheme {
                val state by model.uiState.collectAsStateWithLifecycle()
                val enableBluetooth = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (model.bluetoothEnabled()) model.scan()
                    else model.reportError("Bluetooth está apagado. Actívalo para buscar tu HiGrow.")
                }
                val requestBluetooth = {
                    if (!model.bluetoothAvailable()) model.reportError("Este equipo no dispone de Bluetooth BLE")
                    else if (model.bluetoothEnabled()) model.scan()
                    else runCatching { enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }
                        .onFailure { model.reportError("Activa Bluetooth desde los ajustes del sistema") }
                    Unit
                }
                val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    if (model.hasPermissions()) requestBluetooth()
                    else model.reportError("Permisos rechazados. Puedes habilitarlos en Ajustes → Aplicaciones → HiGrow Monitor → Permisos.")
                }
                HiGrowApp(state,
                    onScan = {
                        if (model.hasPermissions()) requestBluetooth()
                        else permissions.launch(model.requiredPermissions())
                    },
                    onStopScan = model::stopScan,
                    onConnect = model::connect,
                    onDisconnect = model::disconnect,
                    onThresholds = model::saveThresholds)
            }
        }
    }
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) model.onBackground()
    }
}
