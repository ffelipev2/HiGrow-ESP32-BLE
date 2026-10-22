package cl.higrow.monitor.data

import java.nio.ByteBuffer
import java.nio.ByteOrder

object BlePacketParser {
    const val PACKET_SIZE = 12
    fun parse(bytes: ByteArray, receivedAtMillis: Long, receivedAtElapsed: Long): SensorData {
        require(bytes.size == PACKET_SIZE) { "Paquete incompleto: ${bytes.size} bytes (se esperan 12)" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(buffer.get().toInt() == 1) { "Versión de protocolo no compatible" }
        val temperature = buffer.short.toInt()
        val humidity = buffer.short.toInt() and 0xffff
        val soil = buffer.short.toInt() and 0xffff
        val light = buffer.short.toInt() and 0xffff
        val soilCode = buffer.get().toInt() and 0xff
        val flags = buffer.get().toInt() and 0xff
        buffer.get() // Reservado: se ignora para permitir ampliaciones compatibles.
        require(soil in 0..4095 && light in 0..4095) { "Valor ADC fuera de rango" }
        val state = SoilState.entries.getOrNull(soilCode)
        require(state == SoilState.fromAdc(soil)) { "Estado de sustrato inválido o inconsistente" }
        val validDht = flags and 1 != 0
        require(!validDht || (temperature in -400..800 && humidity in 0..1000)) { "DHT fuera de rango" }
        return SensorData(
            if (validDht) temperature / 10f else null,
            if (validDht) humidity / 10f else null,
            soil, light, requireNotNull(state), receivedAtMillis, receivedAtElapsed,
        )
    }
}
