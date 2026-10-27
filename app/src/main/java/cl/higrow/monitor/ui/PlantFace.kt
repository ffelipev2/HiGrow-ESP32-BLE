package cl.higrow.monitor.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import cl.higrow.monitor.data.PlantMood
import kotlinx.coroutines.launch

/** Native vector character: no network, bitmap assets or simulated sensor values. */
@Composable
fun PlantFace(mood: PlantMood, description: String, modifier: Modifier = Modifier, onGreet: () -> Unit = {}) {
    val motion = rememberInfiniteTransition(label = "Brote vivo")
    val breath = motion.animateFloat(0.985f, 1.015f,
        infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "Respirar")
    val sway = motion.animateFloat(-3f, 3f,
        infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "Hojas")
    val blink = motion.animateFloat(1f, 1f, infiniteRepeatable(keyframes {
        durationMillis = 5300
        1f at 0; 1f at 4500; 0.08f at 4620; 1f at 4760; 1f at 5300
    }), label = "Parpadear")
    val greeting = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(modifier.aspectRatio(1f).clickable(role = Role.Button, onClickLabel = "Saludar a Brote") {
        onGreet()
        scope.launch {
            greeting.snapTo(1f)
            greeting.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 160f))
        }
    }.semantics {
        contentDescription = "Brote, tu compañero de planta"
        stateDescription = description
    }) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.minDimension / 240f
            translate((size.width - unit * 240) / 2, (size.height - unit * 240) / 2) {
                scale(unit, unit, pivot = Offset.Zero) {
                    drawPlant(mood, breath.value, sway.value, blink.value, greeting.value)
                }
            }
        }
    }
}

private fun DrawScope.drawPlant(mood: PlantMood, breath: Float, sway: Float, blink: Float, greeting: Float) {
    val resting = mood in listOf(PlantMood.OFFLINE, PlantMood.STALE, PlantMood.WAITING)
    val sleepy = resting || mood == PlantMood.LOW_LIGHT
    val green = Color(0xFF8DDB58)
    val ink = Color(0xFF214730)
    val tint = when (mood) {
        PlantMood.THIRSTY, PlantMood.HOT, PlantMood.DRY_AIR -> Color(0xFFE5BE6F)
        PlantMood.COLD -> Color(0xFF94CED6)
        PlantMood.OFFLINE, PlantMood.STALE -> Color(0xFF9EB6A0)
        else -> Color(0xFFB8E77B)
    }
    drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = 0.26f), Color.Transparent), Offset(120f, 140f), 114f),
        114f, Offset(120f, 140f))
    drawOval(Color(0xFF071F16).copy(alpha = 0.25f), Offset(36f, 204f), Size(170f, 21f))
    drawOval(Color(0xFF4B6240), Offset(58f, 201f), Size(126f, 17f))
    repeat(5) { i ->
        drawCircle(Color(0xFF809A50).copy(alpha = 0.6f), 3f, Offset(76f + i * 22f, 209f + (i % 2) * 3))
    }
    translate(top = -greeting * 10f) {
        val tilt = if (mood == PlantMood.COLD) sway * 0.5f else sway * 0.22f
        rotate(tilt, Offset(120f, 175f)) {
            scale(breath, breath, Offset(120f, 192f)) {
                drawOval(green.copy(alpha = if (resting) 0.5f else 1f), Offset(76f, 192f), Size(36f, 18f))
                drawOval(green.copy(alpha = if (resting) 0.5f else 1f), Offset(133f, 192f), Size(36f, 18f))
                rotate(sway + greeting * 8f, Offset(120f, 85f)) {
                    val stem = Path().apply {
                        moveTo(121f, 91f); cubicTo(120f, 74f, 124f, 58f, 136f, 45f)
                    }
                    drawPath(stem, Color(0xFF4E9C38), style = Stroke(7f, cap = StrokeCap.Round))
                    drawLeaf(Offset(121f, 79f), Offset(63f, 35f), Color(0xFFCCF17A), Color(0xFF438D35))
                    drawLeaf(Offset(125f, 72f), Offset(182f, 21f), Color(0xFFE0FA8D), Color(0xFF4C9F30))
                }
                drawCircle(Color(0xFF102E1B).copy(alpha = 0.16f), 67f, Offset(123f, 147f))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFF1F7C8), tint, lerp(tint, ink, 0.22f)),
                    Offset(96f, 111f), 116f), 67f, Offset(120f, 140f))
                drawCircle(Color(0xFFF1FACB).copy(alpha = 0.62f), 67f, Offset(120f, 140f), style = Stroke(1.5f))
                drawOval(Color.White.copy(alpha = 0.13f), Offset(77f, 91f), Size(50f, 25f))
                drawCircle(Color(0xFFEF9D84).copy(alpha = if (resting) 0.25f else 0.68f), 10f, Offset(80f, 154f))
                drawCircle(Color(0xFFEF9D84).copy(alpha = if (resting) 0.25f else 0.68f), 10f, Offset(160f, 154f))
                scale(1f, if (greeting > 0.4f) 0.18f else blink.coerceAtLeast(0.08f), Offset(120f, 137f)) {
                    listOf(96f, 144f).forEach { x ->
                        when {
                            sleepy -> drawArc(ink, 0f, 180f, false, Offset(x - 9f, 130f), Size(18f, 10f),
                                style = Stroke(4.5f, cap = StrokeCap.Round))
                            mood == PlantMood.HAPPY -> drawArc(ink, 180f, 180f, false,
                                Offset(x - 9f, 129f), Size(18f, 15f), style = Stroke(4.5f, cap = StrokeCap.Round))
                            else -> {
                                drawOval(ink, Offset(x - 5f, 129f), Size(10f, 15f))
                                drawCircle(Color.White.copy(alpha = 0.85f), 2f, Offset(x - 1f, 132f))
                            }
                        }
                    }
                }
                when (mood) {
                    PlantMood.HAPPY -> {
                        val mouth = Path().apply {
                            moveTo(103f, 158f); quadraticTo(120f, 166f, 137f, 158f)
                            cubicTo(136f, 183f, 104f, 183f, 103f, 158f); close()
                        }
                        drawPath(mouth, Color(0xFF57362D))
                        drawOval(Color(0xFFDC8A83), Offset(111f, 169f), Size(18f, 9f))
                    }
                    PlantMood.THIRSTY, PlantMood.DRY_AIR, PlantMood.HUMID_AIR -> {
                        val mouth = Path().apply { moveTo(109f, 167f); quadraticTo(120f, 155f, 131f, 167f) }
                        drawPath(mouth, ink, style = Stroke(4f, cap = StrokeCap.Round))
                        drawLine(ink, Offset(86f, 122f), Offset(101f, 118f), 3f, StrokeCap.Round)
                        drawLine(ink, Offset(139f, 118f), Offset(154f, 122f), 3f, StrokeCap.Round)
                    }
                    PlantMood.HOT -> drawOval(Color(0xFF694D35), Offset(113f, 157f), Size(14f, 21f))
                    PlantMood.COLD -> {
                        drawRoundRect(ink, Offset(108f, 159f), Size(24f, 12f), style = Stroke(3f))
                        drawLine(ink, Offset(109f, 165f), Offset(131f, 165f), 2f)
                    }
                    PlantMood.INCOMPLETE -> {
                        drawLine(ink, Offset(110f, 166f), Offset(131f, 161f), 3.5f, StrokeCap.Round)
                        drawLine(ink, Offset(137f, 119f), Offset(154f, 123f), 3f, StrokeCap.Round)
                    }
                    else -> drawArc(ink.copy(alpha = 0.8f), 10f, 160f, false, Offset(112f, 159f), Size(16f, 9f),
                        style = Stroke(3f, cap = StrokeCap.Round))
                }
                if (mood == PlantMood.HOT || mood == PlantMood.THIRSTY) {
                    val drop = Path().apply {
                        moveTo(164f, 116f); cubicTo(149f, 132f, 159f, 140f, 166f, 135f)
                        cubicTo(173f, 131f, 167f, 123f, 164f, 116f); close()
                    }
                    drawPath(drop, Color(0xFF7ED7EA))
                }
                if (resting || mood == PlantMood.LOW_LIGHT) {
                    drawCircle(Color(0xFFE1F6BE).copy(alpha = 0.75f), 4f, Offset(191f, 84f))
                    drawCircle(Color(0xFFE1F6BE).copy(alpha = 0.45f), 2.5f, Offset(205f, 65f))
                } else {
                    listOf(Offset(39f, 108f), Offset(200f, 146f), Offset(191f, 90f)).forEachIndexed { i, at ->
                        drawCircle(tint.copy(alpha = 0.7f), if (i == 0) 3f else 2f, at)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawLeaf(base: Offset, tip: Offset, light: Color, dark: Color) {
    val direction = if (tip.x > base.x) 1 else -1
    val leaf = Path().apply {
        moveTo(base.x, base.y)
        cubicTo(base.x - 16 * direction, tip.y - 3, tip.x - 8 * direction, tip.y - 5, tip.x, tip.y)
        cubicTo(tip.x + 9 * direction, tip.y + 27, base.x + 21 * direction, base.y + 1, base.x, base.y)
        close()
    }
    drawPath(leaf, Brush.linearGradient(listOf(light, dark), tip, base))
    drawPath(leaf, light.copy(alpha = 0.5f), style = Stroke(1.3f))
    drawLine(dark.copy(alpha = 0.65f), base, Offset(tip.x - 8 * direction, tip.y + 11), 2f, StrokeCap.Round)
}
