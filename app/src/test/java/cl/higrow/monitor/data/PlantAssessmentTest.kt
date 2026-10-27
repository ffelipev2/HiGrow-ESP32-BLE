package cl.higrow.monitor.data

import org.junit.Assert.*
import org.junit.Test

class PlantAssessmentTest {
    private val sample = SensorData(24f, 55f, 1800, 1000, SoilState.SEMI_WET, 1000, 1000)
    private fun assess(data: SensorData? = sample, connected: Boolean = true, stale: Boolean = false,
        limits: LightThresholds = LightThresholds(), packetError: Boolean = false) =
        PlantAssessment.evaluate(connected, stale, data, limits, packetError)

    @Test fun disconnectedNeverUsesRetainedReadings() {
        val result = assess(connected = false)
        assertEquals(PlantMood.OFFLINE, result.mood)
        assertTrue(result.observations.all { it.level == SignalLevel.UNKNOWN })
    }
    @Test fun connectedWithoutMeasurementsWaits() {
        assertEquals(PlantMood.WAITING, assess(data = null).mood)
    }
    @Test fun staleDataCannotBeHappyOrThirsty() {
        assertEquals(PlantMood.STALE, assess(stale = true).mood)
        assertEquals(PlantMood.STALE, assess(sample.copy(soilAdc = 3000), stale = true).mood)
        assertEquals(PlantMood.STALE, assess(data = null, stale = true).mood)
    }
    @Test fun normalCurrentReadingsCanBeHappy() {
        assertEquals(PlantMood.HAPPY, assess().mood)
    }
    @Test fun exactSoilBoundaryChangesExpression() {
        assertEquals(PlantMood.HAPPY, assess(sample.copy(soilAdc = 2099)).mood)
        assertEquals(PlantMood.THIRSTY, assess(sample.copy(soilAdc = 2100)).mood)
    }
    @Test fun wetIsNotAutomaticallyOverwatering() {
        assertEquals(PlantMood.HAPPY, assess(sample.copy(soilAdc = 100)).mood)
    }
    @Test fun temperatureBoundariesAreInclusive() {
        assertEquals(PlantMood.HOT, assess(sample.copy(temperatureC = 32f)).mood)
        assertEquals(PlantMood.COLD, assess(sample.copy(temperatureC = 12f)).mood)
        assertEquals(PlantMood.HAPPY, assess(sample.copy(temperatureC = 31.9f)).mood)
        assertEquals(PlantMood.HAPPY, assess(sample.copy(temperatureC = 12.1f)).mood)
    }
    @Test fun humidityBoundariesAllowReferenceEndpoints() {
        assertEquals(PlantMood.DRY_AIR, assess(sample.copy(humidityPercent = 29f)).mood)
        assertEquals(PlantMood.HUMID_AIR, assess(sample.copy(humidityPercent = 86f)).mood)
        assertEquals(PlantMood.HAPPY, assess(sample.copy(humidityPercent = 30f)).mood)
        assertEquals(PlantMood.HAPPY, assess(sample.copy(humidityPercent = 85f)).mood)
    }
    @Test fun lightUsesExistingIntegerMappingAndCustomThresholds() {
        assertEquals(PlantMood.LOW_LIGHT, assess(sample.copy(lightAdc = 1602)).mood)
        assertEquals(PlantMood.HAPPY, assess(sample.copy(lightAdc = 1601)).mood)
        assertEquals(PlantMood.LOW_LIGHT, assess(limits = LightThresholds(400, 200, 100)).mood)
    }
    @Test fun invalidDhtNeverProducesHappy() {
        assertEquals(PlantMood.INCOMPLETE, assess(sample.copy(temperatureC = null)).mood)
        assertEquals(PlantMood.INCOMPLETE, assess(sample.copy(humidityPercent = null)).mood)
        assertEquals(PlantMood.INCOMPLETE, assess(sample.copy(temperatureC = Float.NaN)).mood)
    }
    @Test fun validSoilAlertSurvivesDhtFailure() {
        val result = assess(sample.copy(temperatureC = null, humidityPercent = null, soilAdc = 3000))
        assertEquals(PlantMood.THIRSTY, result.mood)
        assertEquals(SignalLevel.UNKNOWN, result.observations.first { it.signal == PlantSignal.TEMPERATURE }.level)
    }
    @Test fun multipleConditionsRemainVisibleAndDrySoilHasPriority() {
        val result = assess(sample.copy(soilAdc = 3000, temperatureC = 40f, humidityPercent = 20f, lightAdc = 4095))
        assertEquals(PlantMood.THIRSTY, result.mood)
        assertEquals(4, result.observations.count { it.level == SignalLevel.ATTENTION })
    }
    @Test fun rejectedPacketCannotProduceHappy() {
        assertEquals(PlantMood.INCOMPLETE, assess(packetError = true).mood)
    }
}
