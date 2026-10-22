package cl.higrow.monitor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.higrow.monitor.data.SensorData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private fun axisValue(value: Float): String = String.format(Locale.getDefault(), "%.1f", value)
private fun timeLabel(millis: Long): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))

@Composable
fun SensorChart(title: String, unit: String, history: List<SensorData>, color: Color,
    modifier: Modifier = Modifier, select: (SensorData) -> Float?) {
    val values = history.mapNotNull(select)
    val minimum = values.minOrNull() ?: 0f
    val maximum = values.maxOrNull() ?: 1f
    val margin = max((maximum - minimum) * 0.12f, if (unit == "ADC") 10f else 0.5f)
    val bottom = if (unit == "°C") minimum - margin else max(0f, minimum - margin)
    val top = if (unit == "% HR") min(100f, maximum + margin) else maximum + margin
    val grid = MaterialTheme.colorScheme.outlineVariant
    val description = if (values.isEmpty()) "$title: sin mediciones" else
        "$title: ${values.size} mediciones en $unit, mínimo ${axisValue(minimum)}, máximo ${axisValue(maximum)}"
    Card(modifier.semantics { contentDescription = description }, shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("$title · $unit", style = MaterialTheme.typography.titleSmall)
            if (values.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(130.dp)) {
                    Text("Esperando mediciones válidas", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(Modifier.height(130.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.width(54.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        listOf(top, (top + bottom) / 2, bottom).forEach {
                            Text(axisValue(it), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Canvas(Modifier.weight(1f).fillMaxHeight()) {
                        val inset = 5.dp.toPx()
                        val width = size.width - 2 * inset
                        val height = size.height - 2 * inset
                        repeat(3) { i ->
                            val y = inset + height * i / 2
                            drawLine(grid, Offset(inset, y), Offset(inset + width, y), 1.dp.toPx())
                        }
                        val first = history.first().receivedAtElapsed
                        val span = (history.last().receivedAtElapsed - first).coerceAtLeast(1)
                        val path = Path()
                        var started = false
                        history.forEach { data ->
                            val value = select(data)
                            if (value == null) started = false // No unir a través de errores DHT.
                            else {
                                val x = inset + (data.receivedAtElapsed - first).toFloat() / span * width
                                val y = inset + (1 - (value - bottom) / (top - bottom).coerceAtLeast(0.01f)) * height
                                if (started) path.lineTo(x, y) else path.moveTo(x, y)
                                started = true
                                drawCircle(color, 2.5.dp.toPx(), Offset(x, y))
                            }
                        }
                        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = 62.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(timeLabel(history.first().receivedAtMillis), style = MaterialTheme.typography.labelSmall)
                    Text(timeLabel(history.last().receivedAtMillis), style = MaterialTheme.typography.labelSmall)
                }
                Text("Hora de recepción · ${history.size}/60 muestras", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
