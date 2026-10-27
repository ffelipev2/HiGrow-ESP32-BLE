package cl.higrow.monitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.MonitorUiState
import cl.higrow.monitor.ble.ConnectionStatus
import cl.higrow.monitor.data.*

fun MonitorUiState.plantAssessment() = PlantAssessment.evaluate(
    connection.status == ConnectionStatus.CONNECTED, stale, readings.latest, lightThresholds, readings.packetError != null)

fun PlantSignal.metric(): SensorMetric = when (this) {
    PlantSignal.SOIL -> SensorMetric.SOIL
    PlantSignal.LIGHT -> SensorMetric.LIGHT
    PlantSignal.TEMPERATURE -> SensorMetric.TEMPERATURE
    PlantSignal.HUMIDITY -> SensorMetric.HUMIDITY
}

@Composable
fun PlantCompanionCard(state: MonitorUiState, modifier: Modifier = Modifier, compact: Boolean = false,
    dense: Boolean = false) {
    val assessment = state.plantAssessment()
    var details by rememberSaveable { mutableStateOf(false) }
    var greetings by rememberSaveable(assessment.activeMoods) { mutableIntStateOf(0) }
    val accent = when (assessment.mood) {
        PlantMood.THIRSTY, PlantMood.HOT, PlantMood.DRY_AIR -> Color(0xFFFFDB8B)
        PlantMood.COLD, PlantMood.INCOMPLETE, PlantMood.STALE -> Color(0xFFACDCE9)
        else -> Color(0xFFBDF297)
    }
    val copy = when (greetings % 3) {
        1 -> if (assessment.mood == PlantMood.OFFLINE) "¡Hola! Conecta tu HiGrow para que despierte." else "¡Hola! Soy Brote. Mi expresión sigue tus sensores."
        2 -> assessment.tip
        else -> assessment.summary
    }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp),
        color = Color(0xFF143F2E), contentColor = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5FAD78).copy(alpha = 0.35f))) {
        BoxWithConstraints(Modifier.background(Brush.linearGradient(
            listOf(Color(0xFF173F30), Color(0xFF102A21), Color(0xFF214F32)))).padding(if (dense) 12.dp else 18.dp)) {
            val stacked = maxWidth < (if (compact) 228.dp else 292.dp) || LocalDensity.current.fontScale > 1.3f
            if (dense) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(when (greetings % 3) {
                            1 -> "¡Hola! Soy Brote"
                            2 -> "Aquí estoy contigo"
                            else -> "Estado de tu planta"
                        }, style = MaterialTheme.typography.labelSmall, color = Color(0xFFD2E3D4))
                        CompanionCopy(assessment, copy, accent, onDetails = { details = true }, dense = true)
                    }
                    PlantFace(assessment.mood, assessment.title, Modifier.size(92.dp),
                        activeMoods = assessment.activeMoods) { greetings++ }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Eco, null, tint = accent, modifier = Modifier.size(20.dp))
                        Text("Estado de tu planta", style = MaterialTheme.typography.titleSmall)
                    }
                    if (stacked) {
                        PlantFace(assessment.mood, assessment.title, Modifier.size(144.dp).align(Alignment.CenterHorizontally),
                            activeMoods = assessment.activeMoods) { greetings++ }
                        CompanionCopy(assessment, copy, accent, onDetails = { details = true })
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Column(Modifier.weight(1f)) {
                                CompanionCopy(assessment, copy, accent, onDetails = { details = true })
                            }
                            PlantFace(assessment.mood, assessment.title, Modifier.size(if (compact) 104.dp else 136.dp),
                                activeMoods = assessment.activeMoods) { greetings++ }
                        }
                    }
                }
            }
        }
    }
    if (details) PlantDetailsDialog(state, assessment) { details = false }
}

@Composable
private fun CompanionCopy(assessment: PlantAssessment, copy: String, accent: Color, onDetails: () -> Unit,
    dense: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(if (dense) 4.dp else 8.dp)) {
        Text(assessment.title, style = if (dense) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = accent)
        if (!dense) Text(copy, style = MaterialTheme.typography.bodySmall, color = Color(0xFFD2E3D4))
        TextButton(onClick = onDetails, contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = accent)) {
            Text("Ver condiciones", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PlantDetailsDialog(state: MonitorUiState, assessment: PlantAssessment, onDismiss: () -> Unit) {
    val data = state.readings.latest.takeIf { state.connection.status == ConnectionStatus.CONNECTED && !state.stale }
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Outlined.Eco, null) },
        title = { Text("Brote · ${assessment.title}") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(assessment.summary)
                Text(assessment.tip, style = MaterialTheme.typography.bodyMedium)
                assessment.observations.forEach { observation ->
                    val metric = observation.signal.metric()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(metric.icon, null, tint = metric.ink(), modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(metric.title, style = MaterialTheme.typography.labelMedium)
                            Text(observation.label, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(metric.display(data) + if (data != null && metric.unit == "ADC") " ADC" else "",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
                Text("Cómo cambia mi expresión", style = MaterialTheme.typography.titleSmall)
                Text("El sustrato y la luz usan los umbrales de HiGrow y Ajustes. " +
                    "La referencia ambiental general marca frío a 12 °C o menos, calor desde 32 °C, " +
                    "aire seco por debajo de 30 % y humedad alta por encima de 85 %. " +
                    "No es un perfil específico para tu especie ni un diagnóstico de salud.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Toca la carita para saludarme. Siempre mantengo la expresión que corresponda a las lecturas.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }, confirmButton = { TextButton(onDismiss) { Text("Entendido") } })
}
