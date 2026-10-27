package cl.higrow.monitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Dimensions of the content viewport, before applying vertical scrolling.
// This also responds to split-screen resizing, rather than the device orientation alone.
data class ScreenViewport(val width: Dp = 0.dp, val height: Dp = 0.dp)
val LocalScreenViewport = staticCompositionLocalOf { ScreenViewport() }

@Composable
fun ResponsiveRows(items: List<@Composable (Modifier) -> Unit>, minCellWidth: Int = 280,
    maxColumns: Int = 2, compactPairs: Boolean = false, spacing: Dp = 12.dp) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val readableWidth = maxWidth.value / fontScale
        val fitted = if (compactPairs && readableWidth in 320f..559f) 2 else
            ((readableWidth + spacing.value) / (minCellWidth + spacing.value)).toInt().coerceIn(1, maxColumns)
        val columns = if (items.size == 4 && fitted == 3) 2 else fitted
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            items.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    row.forEach { content -> content(Modifier.weight(1f).fillMaxHeight()) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
