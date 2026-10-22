package cl.higrow.monitor.data

data class MeasurementStats(val minimum: Float, val maximum: Float, val average: Float, val count: Int) {
    companion object {
        fun from(values: List<Float?>): MeasurementStats? {
            val valid = values.filterNotNull().filter { it.isFinite() }
            if (valid.isEmpty()) return null
            return MeasurementStats(valid.min(), valid.max(), valid.average().toFloat(), valid.size)
        }
    }
}
