package cl.higrow.monitor.data

import org.junit.Assert.*
import org.junit.Test

class BlePacketParserTest {
    private val golden = byteArrayOf(0x01, 0xEB.toByte(), 0x00, 0x37, 0x02,
        0x6C, 0x07, 0x00, 0x08, 0x01, 0x01, 0x00)
    private fun parse(packet: ByteArray = golden) = BlePacketParser.parse(packet, 1234, 5678)

    @Test fun decodesGoldenLittleEndianVector() {
        val result = parse()
        assertEquals(23.5f, result.temperatureC!!, 0.001f)
        assertEquals(56.7f, result.humidityPercent!!, 0.001f)
        assertEquals(1900, result.soilAdc)
        assertEquals(2048, result.lightAdc)
        assertEquals(SoilState.SEMI_WET, result.soilState)
        assertEquals(1234L, result.receivedAtMillis)
        assertEquals(5678L, result.receivedAtElapsed)
    }
    @Test fun signedTemperatureAndUnsignedAdc() {
        val packet = golden.copyOf()
        packet[1] = 0xCB.toByte(); packet[2] = 0xFF.toByte() // -53 -> -5.3 °C
        packet[7] = 0xFF.toByte(); packet[8] = 0x0F
        assertEquals(-5.3f, parse(packet).temperatureC!!, 0.001f)
        assertEquals(4095, parse(packet).lightAdc)
    }
    @Test fun invalidDhtNeverBecomesZeroOrAnOldReading() {
        val packet = golden.copyOf()
        packet[10] = 0
        packet[1] = 0xFF.toByte(); packet[2] = 0x7F
        packet[3] = 0xFF.toByte(); packet[4] = 0xFF.toByte()
        val result = parse(packet)
        assertNull(result.temperatureC)
        assertNull(result.humidityPercent)
        assertEquals(1900, result.soilAdc)
    }
    @Test fun rejectsTruncatedAndOversizePackets() {
        listOf(0, 1, 11, 13, 20).forEach { size -> assertThrows(IllegalArgumentException::class.java) { parse(golden.copyOf(size)) } }
    }
    @Test fun rejectsUnknownVersion() {
        assertThrows(IllegalArgumentException::class.java) { parse(golden.copyOf().also { it[0] = 2 }) }
    }
    @Test fun rejectsBadAdcAndSoilState() {
        assertThrows(IllegalArgumentException::class.java) { parse(golden.copyOf().also { it[6] = 0x10 }) }
        assertThrows(IllegalArgumentException::class.java) { parse(golden.copyOf().also { it[9] = 3 }) }
        assertThrows(IllegalArgumentException::class.java) { parse(golden.copyOf().also { it[9] = 2 }) }
    }
    @Test fun rejectsOutOfRangeValidDht() {
        assertThrows(IllegalArgumentException::class.java) { parse(golden.copyOf().also { it[4] = 5 }) }
        assertThrows(IllegalArgumentException::class.java) { parse(golden.copyOf().also { it[2] = 4 }) }
    }
    @Test fun allowsFutureReservedBits() {
        val result = parse(golden.copyOf().also { it[10] = 0x81.toByte(); it[11] = 0xFF.toByte() })
        assertEquals(23.5f, result.temperatureC!!, 0.001f)
    }
    @Test fun soilBoundariesAreExact() {
        mapOf(0 to SoilState.WET, 1699 to SoilState.WET, 1700 to SoilState.WET,
            1701 to SoilState.SEMI_WET, 2099 to SoilState.SEMI_WET,
            2100 to SoilState.DRY, 4095 to SoilState.DRY).forEach { (adc, expected) ->
            assertEquals(expected, SoilState.fromAdc(adc))
        }
    }
    @Test fun lightBoundariesMatchArduinoIntegerMap() {
        (0..4095).forEach { raw ->
            val mapped = raw * 1023 / 4095
            val expected = when {
                mapped >= 1000 -> LightState.VERY_LOW
                mapped >= 400 -> LightState.LOW
                mapped >= 120 -> LightState.MEDIUM
                else -> LightState.VERY_HIGH
            }
            assertEquals("raw=$raw", expected, lightState(raw))
        }
        assertEquals(LightState.VERY_HIGH, lightState(480)) // map = 119
        assertEquals(LightState.MEDIUM, lightState(481)) // map = 120
        assertEquals(LightState.LOW, lightState(1602)) // map = 400
        assertEquals(LightState.VERY_LOW, lightState(4003)) // map = 1000
    }
    @Test fun customThresholdsAndValidation() {
        assertEquals(LightState.VERY_LOW, lightState(2048, LightThresholds(500, 300, 100)))
        listOf(Triple(400, 400, 120), Triple(1000, 400, 0), Triple(1024, 400, 120)).forEach { (a,b,c) ->
            assertThrows(IllegalArgumentException::class.java) { LightThresholds(a,b,c) }
        }
    }
}
