package cl.higrow.monitor

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.edit
import cl.higrow.monitor.ble.*
import cl.higrow.monitor.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

data class MonitorUiState(
    val connection: ConnectionState = ConnectionState(),
    val devices: List<DiscoveredDevice> = emptyList(),
    val readings: SensorReadings = SensorReadings(),
    val stale: Boolean = false,
    val ageSeconds: Long? = null,
    val lightThresholds: LightThresholds = LightThresholds(),
    val autoReconnect: Boolean = true,
)

class SensorViewModel(application: Application) : AndroidViewModel(application) {
    private val ble = BleManager(application)
    private val repository = SensorRepository(ble, viewModelScope)
    private val preferences = application.getSharedPreferences("higrow", 0)
    private val thresholds = MutableStateFlow(runCatching {
        LightThresholds(preferences.getInt("veryLow", 1000), preferences.getInt("low", 400),
            preferences.getInt("medium", 120))
    }.getOrDefault(LightThresholds()))
    private val autoReconnect = MutableStateFlow(preferences.getBoolean("autoReconnect", true))
    init { ble.setAutoReconnect(autoReconnect.value) }
    private val clock = flow {
        while (true) { emit(SystemClock.elapsedRealtime()); delay(1000) }
    }
    private val settings = combine(thresholds, autoReconnect) { limits, reconnect -> limits to reconnect }
    val uiState = combine(ble.connection, ble.devices, repository.readings, clock, settings) {
            connection, devices, readings, now, settings ->
        val visible = if (connection.status == ConnectionStatus.CONNECTED) readings else SensorReadings()
        val age = visible.latest?.let { ((now - it.receivedAtElapsed).coerceAtLeast(0)) / 1000 }
        val waitingTooLong = visible.latest == null && connection.connectedAtElapsed?.let { now - it >= 6000 } == true
        MonitorUiState(connection, devices, visible, (age != null && age >= 6) || waitingTooLong,
            age, settings.first, settings.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonitorUiState())

    fun requiredPermissions() = ble.requiredPermissions()
    fun hasPermissions() = ble.hasPermissions()
    fun bluetoothAvailable() = ble.bluetoothAvailable()
    fun bluetoothEnabled() = ble.bluetoothEnabled()
    fun scan() = ble.startScan()
    fun stopScan() = ble.stopScan()
    fun connect(address: String) = ble.connect(address)
    fun disconnect() = ble.disconnect()
    fun reportError(message: String) = ble.reportError(message)
    fun saveThresholds(value: LightThresholds) {
        thresholds.value = value
        preferences.edit {
            putInt("veryLow", value.veryLow); putInt("low", value.low); putInt("medium", value.medium)
        }
    }
    fun setAutoReconnect(enabled: Boolean) {
        autoReconnect.value = enabled
        preferences.edit { putBoolean("autoReconnect", enabled) }
        ble.setAutoReconnect(enabled)
    }
    // Solo primer plano. Una rotación conserva el ViewModel y su conexión.
    fun onBackground() = ble.disconnect()
    override fun onCleared() { ble.close() }
}
