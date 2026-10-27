package cl.higrow.monitor.data

enum class PlantMood { OFFLINE, WAITING, STALE, THIRSTY, HOT, COLD, LOW_LIGHT, DRY_AIR, HUMID_AIR, INCOMPLETE, HAPPY }
enum class PlantSignal { SOIL, LIGHT, TEMPERATURE, HUMIDITY }
enum class SignalLevel { NORMAL, ATTENTION, UNKNOWN }

data class PlantObservation(val signal: PlantSignal, val label: String, val level: SignalLevel)

data class PlantAssessment(
    val mood: PlantMood,
    val title: String,
    val summary: String,
    val tip: String,
    val observations: List<PlantObservation>,
    val activeMoods: List<PlantMood> = listOf(mood),
) {
    companion object {
        // These are visible, general-purpose UI reference ranges, not a species profile.
        const val COLD_C = 12f
        const val HOT_C = 32f
        const val DRY_AIR_PERCENT = 30f
        const val HUMID_AIR_PERCENT = 85f

        fun evaluate(connected: Boolean, stale: Boolean, data: SensorData?,
            thresholds: LightThresholds = LightThresholds(), packetError: Boolean = false): PlantAssessment {
            val unavailable = PlantSignal.entries.map { PlantObservation(it, "Sin datos actuales", SignalLevel.UNKNOWN) }
            if (!connected) return PlantAssessment(PlantMood.OFFLINE, "En descanso",
                "Conecta tu HiGrow para saber cómo estoy.", "Mi expresión despertará con las primeras lecturas por Bluetooth.", unavailable)
            if (stale) return PlantAssessment(PlantMood.STALE, "¿Sigues ahí?",
                "Hace tiempo que no recibo una lectura válida.", "Revisa la conexión. No evalúo la planta con datos antiguos.", unavailable)
            if (data == null) return PlantAssessment(PlantMood.WAITING, "Despertando",
                "Ya estamos conectados. Espero a los sensores.", "En cuanto llegue una lectura, te mostraré cómo están las condiciones.", unavailable)

            val temperature = data.temperatureC?.takeIf { it.isFinite() }
            val humidity = data.humidityPercent?.takeIf { it.isFinite() && it in 0f..100f }
            val soil = SoilState.fromAdc(data.soilAdc)
            val light = lightState(data.lightAdc, thresholds)
            val observations = listOf(
                PlantObservation(PlantSignal.SOIL, soil.label,
                    if (soil == SoilState.DRY) SignalLevel.ATTENTION else SignalLevel.NORMAL),
                PlantObservation(PlantSignal.LIGHT, "Luz ${light.label.lowercase()}",
                    if (light in listOf(LightState.LOW, LightState.VERY_LOW)) SignalLevel.ATTENTION else SignalLevel.NORMAL),
                PlantObservation(PlantSignal.TEMPERATURE, when {
                    temperature == null -> "DHT11 sin lectura"
                    temperature <= COLD_C -> "Frío"
                    temperature >= HOT_C -> "Calor"
                    else -> "En rango"
                }, when {
                    temperature == null -> SignalLevel.UNKNOWN
                    temperature <= COLD_C || temperature >= HOT_C -> SignalLevel.ATTENTION
                    else -> SignalLevel.NORMAL
                }),
                PlantObservation(PlantSignal.HUMIDITY, when {
                    humidity == null -> "DHT11 sin lectura"
                    humidity < DRY_AIR_PERCENT -> "Aire seco"
                    humidity > HUMID_AIR_PERCENT -> "Humedad alta"
                    else -> "En rango"
                }, when {
                    humidity == null -> SignalLevel.UNKNOWN
                    humidity < DRY_AIR_PERCENT || humidity > HUMID_AIR_PERCENT -> SignalLevel.ATTENTION
                    else -> SignalLevel.NORMAL
                }),
            )
            // Known conditions still matter when the DHT fails. Missing readings and
            // rejected packets must never result in a reassuring happy expression.
            val activeMoods = buildList {
                if (soil == SoilState.DRY) add(PlantMood.THIRSTY)
                if (temperature != null && temperature >= HOT_C) add(PlantMood.HOT)
                if (temperature != null && temperature <= COLD_C) add(PlantMood.COLD)
                if (light in listOf(LightState.LOW, LightState.VERY_LOW)) add(PlantMood.LOW_LIGHT)
                if (humidity != null && humidity < DRY_AIR_PERCENT) add(PlantMood.DRY_AIR)
                if (humidity != null && humidity > HUMID_AIR_PERCENT) add(PlantMood.HUMID_AIR)
                if (temperature == null || humidity == null || packetError) add(PlantMood.INCOMPLETE)
                if (isEmpty()) add(PlantMood.HAPPY)
            }
            val mood = activeMoods.first()
            val messages = activeMoods.map(::message)
            val title = if (activeMoods.size == 1) messages.single().first else {
                val labels = activeMoods.map(::shortLabel)
                if (labels.size == 2) "${labels.first()} y ${labels.last().lowercase()}" else labels.joinToString(" · ")
            }
            return PlantAssessment(mood, title, messages.joinToString(" ") { it.second },
                messages.joinToString(" ") { it.third }, observations, activeMoods)
        }

        private fun shortLabel(mood: PlantMood): String = when (mood) {
            PlantMood.THIRSTY -> "Sed"
            PlantMood.HOT -> "Calor"
            PlantMood.COLD -> "Frío"
            PlantMood.LOW_LIGHT -> "Poca luz"
            PlantMood.DRY_AIR -> "Aire seco"
            PlantMood.HUMID_AIR -> "Humedad alta"
            PlantMood.INCOMPLETE -> "Datos incompletos"
            else -> message(mood).first
        }

        private fun message(mood: PlantMood): Triple<String, String, String> = when (mood) {
                PlantMood.THIRSTY -> Triple("Tengo sed", "El sensor indica sustrato seco.",
                    "Comprueba el sustrato y las necesidades de tu planta antes de regar.")
                PlantMood.HOT -> Triple("Tengo calor", "La temperatura está por encima de mi referencia.",
                    "Revisa la temperatura y la exposición al sol según la especie.")
                PlantMood.COLD -> Triple("Tengo frío", "La temperatura está por debajo de mi referencia.",
                    "Comprueba si esta temperatura es adecuada para tu planta.")
                PlantMood.LOW_LIGHT -> Triple("Con sueño", "Ahora el sensor detecta poca luz.",
                    "Puede ser de noche. Comprueba la luz durante el día antes de cambiarme de lugar.")
                PlantMood.DRY_AIR -> Triple("Aire seco", "La humedad ambiental está baja.",
                    "Revisa si tu especie necesita más humedad ambiental; esto no indica cuánto regar.")
                PlantMood.HUMID_AIR -> Triple("Mucha humedad", "La humedad ambiental está alta.",
                    "Comprueba la ventilación y las necesidades de tu especie.")
                PlantMood.INCOMPLETE -> Triple("Me faltan datos", "Hay lecturas ausentes o un paquete rechazado.",
                    "Las mediciones disponibles siguen visibles. Revisa el DHT11 o la conexión.")
                else -> Triple("Feliz", "Sin alertas en las lecturas actuales.",
                    "Sustrato con humedad y luz disponible. Mi expresión resume sensores, no diagnostica la salud de la planta.")
        }
    }
}
