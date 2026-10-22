package cl.higrow.monitor.data

enum class SoilState(val label: String) {
    WET("Húmedo"), SEMI_WET("Semi Húmedo"), DRY("Seco");
    companion object {
        fun fromAdc(adc: Int): SoilState = when {
            adc >= 2100 -> DRY
            adc > 1700 -> SEMI_WET
            else -> WET
        }
    }
}

data class LightThresholds(val veryLow: Int = 1000, val low: Int = 400, val medium: Int = 120) {
    init { require(medium in 1..1021 && low in (medium + 1)..1022 && veryLow in (low + 1)..1023) }
}
enum class LightState(val label: String) {
    VERY_LOW("Muy Baja"), LOW("Baja"), MEDIUM("Media"), VERY_HIGH("Muy Alta")
}
fun lightState(raw: Int, thresholds: LightThresholds = LightThresholds()): LightState {
    require(raw in 0..4095)
    val mapped = raw * 1023 / 4095  // Arduino map(), división entera.
    return when {
        mapped >= thresholds.veryLow -> LightState.VERY_LOW
        mapped >= thresholds.low -> LightState.LOW
        mapped >= thresholds.medium -> LightState.MEDIUM
        else -> LightState.VERY_HIGH
    }
}

data class SensorData(
    val temperatureC: Float?,
    val humidityPercent: Float?,
    val soilAdc: Int,
    val lightAdc: Int,
    val soilState: SoilState,
    val receivedAtMillis: Long,
    val receivedAtElapsed: Long,
)
