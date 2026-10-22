package cl.higrow.monitor.data

import android.os.SystemClock
import cl.higrow.monitor.ble.BleManager
import cl.higrow.monitor.ble.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SensorReadings(
    val latest: SensorData? = null,
    val history: List<SensorData> = emptyList(),
    val packetError: String? = null,
)

class SensorRepository(ble: BleManager, scope: CoroutineScope) {
    private val mutableReadings = MutableStateFlow(SensorReadings())
    val readings = mutableReadings.asStateFlow()
    init {
        scope.launch {
            ble.connection.collect { state ->
                if (state.status != ConnectionStatus.CONNECTED) mutableReadings.value = SensorReadings()
            }
        }
        scope.launch {
            ble.packets.collect { bytes ->
                if (ble.connection.value.status != ConnectionStatus.CONNECTED) return@collect
                runCatching { BlePacketParser.parse(bytes, System.currentTimeMillis(), SystemClock.elapsedRealtime()) }
                    .onSuccess { data ->
                        mutableReadings.value = SensorReadings(data,
                            (mutableReadings.value.history + data).takeLast(60))
                    }.onFailure { error ->
                        mutableReadings.value = mutableReadings.value.copy(packetError = error.message)
                    }
            }
        }
    }
}
