package cl.higrow.monitor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import cl.higrow.monitor.data.SensorData
import kotlin.math.max

private data class ChartBounds(val low: Float, val high: Float)
private fun bounds(history: List<SensorData>, metric: SensorMetric, fixed: Boolean): ChartBounds {
    if (fixed && metric != SensorMetric.TEMPERATURE) {
        return ChartBounds(0f, if (metric == SensorMetric.HUMIDITY) 100f else 4095f)
    }
    val values = history.mapNotNull(metric::value)
    val low = values.minOrNull() ?: 0f
    val high = values.maxOrNull() ?: 1f
    val padding = max((high - low) * 0.18f, if (metric.unit == "ADC") 10f else 0.5f)
    return ChartBounds(if (metric == SensorMetric.TEMPERATURE) low - padding else max(0f, low - padding), high + padding)
}

@Composable
fun SensorSparkline(history: List<SensorData>, metric: SensorMetric, modifier: Modifier = Modifier,
    fixedScale: Boolean = false, grid: Boolean = false, dots: Boolean = false) {
    val color = metric.ink()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val limits = bounds(history, metric, fixedScale)
    Canvas(modifier) {
        val inset = 3.dp.toPx()
        val width = (size.width - inset * 2).coerceAtLeast(1f)
        val height = (size.height - inset * 2).coerceAtLeast(1f)
        if (grid) repeat(3) { i ->
            val y = inset + height * i / 2f
            drawLine(gridColor.copy(alpha = 0.6f), Offset(inset, y), Offset(inset + width, y), 0.5.dp.toPx())
        }
        if (history.isEmpty()) return@Canvas
        val first = history.first().receivedAtElapsed
        val span = (history.last().receivedAtElapsed - first).coerceAtLeast(1)
        val segments = mutableListOf<MutableList<Offset>>()
        var segment = mutableListOf<Offset>()
        history.forEach { data ->
            val value = metric.value(data)
            if (value == null) {
                if (segment.isNotEmpty()) segments.add(segment)
                segment = mutableListOf()
            } else {
                val x = inset + (data.receivedAtElapsed - first).toFloat() / span * width
                val y = inset + (1f - (value - limits.low) / (limits.high - limits.low).coerceAtLeast(0.01f)) * height
                segment.add(Offset(x, y))
            }
        }
        if (segment.isNotEmpty()) segments.add(segment)
        segments.forEach { points ->
            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            if (points.size > 1) {
                val area = Path().apply {
                    addPath(line)
                    lineTo(points.last().x, inset + height)
                    lineTo(points.first().x, inset + height)
                    close()
                }
                drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.14f), color.copy(alpha = 0.01f))))
                drawPath(line, color, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (dots || points.size == 1) points.forEach { drawCircle(color, 1.9.dp.toPx(), it) }
        }
    }
}

@Composable
fun SensorChart(metric: SensorMetric, history: List<SensorData>, modifier: Modifier = Modifier,
    plotHeight: Dp = 72.dp, onClick: () -> Unit = {}) {
    val values = history.mapNotNull(metric::value)
    val limits = bounds(history, metric, fixed = true)
    val color = metric.ink()
    val description = if (values.isEmpty()) "${metric.title}: sin mediciones" else
        "${metric.title}: ${values.size} mediciones, mínimo ${metric.format(values.min())}, máximo ${metric.format(values.max())} ${metric.unit}"
    OutlinedCard(onClick, modifier.semantics { contentDescription = description }, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${metric.title} (${metric.unit})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(metric.format(history.lastOrNull()?.let(metric::value)), color = color,
                    style = MaterialTheme.typography.titleSmall)
            }
            if (values.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(plotHeight + 8.dp), contentAlignment = Alignment.Center) {
                    Text("Esperando mediciones", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(Modifier.height(plotHeight), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(Modifier.width(36.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        listOf(limits.high, (limits.high + limits.low) / 2f, limits.low).forEach {
                            Text(if (metric.unit == "ADC") it.toInt().toString() else metric.format(it),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    SensorSparkline(history, metric, Modifier.weight(1f).fillMaxHeight(),
                        fixedScale = true, grid = true, dots = true)
                }
                Row(Modifier.fillMaxWidth().padding(start = 42.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(readingTime(history.first().receivedAtMillis), style = MaterialTheme.typography.labelSmall)
                    if (history.size > 2) Text(readingTime(history[history.size / 2].receivedAtMillis), style = MaterialTheme.typography.labelSmall)
                    Text(readingTime(history.last().receivedAtMillis), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
