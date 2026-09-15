package com.kazuya.weartube.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kazuya.weartube.ui.WearTubeColors

/**
 * design.md 4.2 のシークバー。進捗＝青、残り＝白 40%、ノブ＝白の円（径 14dp）。
 * タップで位置指定、ドラッグ中はノブだけ動かし、離したときにシークする。
 */
@Composable
fun SeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val shown = if (dragging) dragProgress else progress

    Canvas(
        modifier =
            modifier
                .height(24.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
                }.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            dragging = false
                            onSeek(dragProgress)
                        },
                        onDragCancel = { dragging = false },
                    ) { change, _ ->
                        dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        change.consume()
                    }
                },
    ) {
        val cy = size.height / 2
        val stroke = 4.dp.toPx()
        val knobRadius = 7.dp.toPx()
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(0f, cy),
            end = Offset(size.width, cy),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = WearTubeColors.ProgressBlue,
            start = Offset(0f, cy),
            end = Offset(size.width * shown, cy),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(color = Color.White, radius = knobRadius, center = Offset(size.width * shown, cy))
    }
}
