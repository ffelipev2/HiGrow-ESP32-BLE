package cl.higrow.monitor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform

// Decorative vector artwork. It carries no readings or chart semantics.
@Composable
fun BotanicalLeaves(modifier: Modifier = Modifier, subtle: Boolean = false) {
    Canvas(modifier) {
        val leaf = Path().apply {
            moveTo(0f, 0f)
            cubicTo(-0.28f, -0.58f, 0.03f, -1.03f, 0.52f, -1.18f)
            cubicTo(0.65f, -0.63f, 0.39f, -0.15f, 0f, 0f)
            close()
        }
        val scale = size.height * 0.84f
        withTransform({ translate(size.width * 0.72f, size.height * 1.12f); scale(scale, scale, Offset.Zero) }) {
            listOf(-58f, -14f, 34f).forEachIndexed { index, angle ->
                rotate(angle, Offset.Zero) {
                    drawPath(leaf, Brush.linearGradient(
                        listOf(Color(0xFFB5E276), Color(0xFF357E3B), Color(0xFF0C4C32)),
                        start = Offset(0.5f, -1.1f), end = Offset.Zero), alpha = if (subtle) 0.08f else 0.7f)
                    drawLine(Color(0xFFD5F0A6).copy(alpha = if (subtle) 0.06f else 0.4f),
                        Offset.Zero, Offset(0.39f, -0.99f), strokeWidth = 0.006f)
                    if (!subtle) {
                        drawCircle(Color.White.copy(alpha = 0.26f), radius = 0.025f,
                            center = Offset(0.16f, -0.5f - index * 0.08f))
                    }
                }
            }
        }
    }
}

@Composable
fun SensorEmptyArtwork(metric: SensorMetric, ink: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val width = size.width
        val height = size.height * 0.52f
        withTransform({ translate(0f, size.height - height) }) {
            val wave = Path().apply {
                moveTo(0f, height * 0.59f)
                cubicTo(width * 0.13f, height * 0.31f, width * 0.18f, height * 0.67f, width * 0.34f, height * 0.46f)
                cubicTo(width * 0.51f, height * 0.21f, width * 0.56f, height * 0.6f, width * 0.72f, height * 0.34f)
                cubicTo(width * 0.88f, height * 0.12f, width * 0.9f, height * 0.42f, width, height * 0.17f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(wave, Brush.verticalGradient(listOf(ink.copy(alpha = 0.2f), ink.copy(alpha = 0.04f))))
            if (metric == SensorMetric.SOIL) {
                drawOval(Color(0xFF8A6E43).copy(alpha = 0.18f),
                    topLeft = Offset(width * 0.66f, height * 0.7f), size = Size(width * 0.4f, height * 0.43f))
                val stem = Offset(width * 0.86f, height * 0.75f)
                drawLine(ink.copy(alpha = 0.5f), stem, stem - Offset(0f, height * 0.2f), strokeWidth = 3f)
                drawOval(ink.copy(alpha = 0.35f), topLeft = stem - Offset(width * 0.085f, height * 0.29f),
                    size = Size(width * 0.09f, height * 0.13f))
                drawOval(ink.copy(alpha = 0.5f), topLeft = stem - Offset(0f, height * 0.33f),
                    size = Size(width * 0.085f, height * 0.16f))
            }
            if (metric == SensorMetric.LIGHT) {
                drawCircle(ink.copy(alpha = 0.11f), radius = height * 0.16f,
                    center = Offset(width * 0.85f, height * 0.53f), style = Stroke(width = 5f))
            }
        }
    }
}
