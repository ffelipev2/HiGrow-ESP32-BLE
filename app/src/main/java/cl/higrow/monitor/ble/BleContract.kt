package cl.higrow.monitor.ble

import java.util.UUID

object BleContract {
    const val DEVICE_NAME = "HiGrow-ESP32"
    val SERVICE: UUID = UUID.fromString("7b183224-9168-443e-a927-7aeea07e8101")
    val SENSOR: UUID = UUID.fromString("7b183224-9168-443e-a927-7aeea07e8102")
    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}
enum class ConnectionStatus(val label: String) {
    DISCONNECTED("Desconectado"), SCANNING("Buscando"), CONNECTING("Conectando"),
    CONNECTED("Conectado"), RECONNECTING("Reconectando"), ERROR("Error")
}
data class ConnectionState(
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val deviceName: String? = null,
    val message: String = "Conecta tu HiGrow para empezar",
    val session: Int = 0,
    val connectedAtElapsed: Long? = null,
    val deviceAddress: String? = null,
)
data class DiscoveredDevice(val address: String, val name: String, val rssi: Int)
