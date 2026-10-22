package cl.higrow.monitor.data

import org.junit.Assert.*
import org.junit.Test

class MeasurementStatsTest {
    @Test fun emptyOrInvalidHistoryHasNoStatistics() {
        assertNull(MeasurementStats.from(emptyList()))
        assertNull(MeasurementStats.from(listOf(null, Float.NaN, Float.POSITIVE_INFINITY)))
    }
    @Test fun statisticsIgnoreMissingDhtReadingsAndKeepRealZero() {
        val stats = MeasurementStats.from(listOf(-5f, null, 0f, 20f))!!
        assertEquals(-5f, stats.minimum, 0.001f)
        assertEquals(20f, stats.maximum, 0.001f)
        assertEquals(5f, stats.average, 0.001f)
        assertEquals(3, stats.count)
    }
}
