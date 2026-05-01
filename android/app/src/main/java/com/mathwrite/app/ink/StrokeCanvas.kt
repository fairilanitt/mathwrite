package com.mathwrite.app.ink

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

@Composable
fun StrokeCanvas(
    strokes: List<InkStroke>,
    onStrokeFinished: (InkStroke) -> Unit,
    onStylusButtonPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPoints by remember { mutableStateOf<List<InkPoint>>(emptyList()) }
    var wasStylusButtonPressed by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .motionEventSpy { event ->
                val pressed = StylusButtonPolicy.isSendButtonPressed(
                    event.toStylusToolKind(),
                    event.buttonState,
                )

                if (pressed && !wasStylusButtonPressed) {
                    onStylusButtonPressed()
                }

                wasStylusButtonPressed = pressed
                if (event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL ||
                    event.actionMasked == MotionEvent.ACTION_BUTTON_RELEASE
                ) {
                    wasStylusButtonPressed = false
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!PointerInputPolicy.accepts(down.type.toInkPointerKind())) {
                        down.consume()
                        currentPoints = emptyList()
                        return@awaitEachGesture
                    }

                    down.consume()

                    val pointerId = down.id
                    val points = mutableListOf(down.position.toInkPoint())
                    currentPoints = points.toList()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!PointerInputPolicy.accepts(change.type.toInkPointerKind())) {
                            change.consume()
                            currentPoints = emptyList()
                            break
                        }

                        val point = change.position.toInkPoint()

                        if (change.pressed) {
                            if (points.last().isMeaningfullyDifferentFrom(point)) {
                                points.add(point)
                                currentPoints = points.toList()
                            }
                            change.consume()
                        } else {
                            change.consume()
                            val stroke = if (points.size == 1) {
                                InkStroke.dotAt(points.first())
                            } else {
                                InkStroke(points.toList())
                            }

                            if (stroke.isUsable) {
                                onStrokeFinished(stroke)
                            }

                            currentPoints = emptyList()
                            break
                        }
                    }
                }
            },
    ) {
        strokes.forEach { stroke ->
            drawStroke(stroke, Color(0xFF111827))
        }

        if (currentPoints.isNotEmpty()) {
            drawStroke(InkStroke(currentPoints), Color(0xFF2563EB))
        }
    }
}

private fun Offset.toInkPoint(): InkPoint = InkPoint(x, y)

private fun PointerType.toInkPointerKind(): InkPointerKind {
    return when (this) {
        PointerType.Mouse -> InkPointerKind.Mouse
        PointerType.Touch -> InkPointerKind.Touch
        PointerType.Stylus -> InkPointerKind.Stylus
        PointerType.Eraser -> InkPointerKind.Eraser
        else -> InkPointerKind.Unknown
    }
}

private fun MotionEvent.toStylusToolKind(): StylusToolKind {
    val toolType = if (pointerCount > 0) getToolType(actionIndex.coerceAtMost(pointerCount - 1)) else MotionEvent.TOOL_TYPE_UNKNOWN
    return when (toolType) {
        MotionEvent.TOOL_TYPE_STYLUS -> StylusToolKind.Stylus
        MotionEvent.TOOL_TYPE_ERASER -> StylusToolKind.Eraser
        MotionEvent.TOOL_TYPE_FINGER -> StylusToolKind.Touch
        MotionEvent.TOOL_TYPE_MOUSE -> StylusToolKind.Mouse
        else -> StylusToolKind.Unknown
    }
}

private fun InkPoint.isMeaningfullyDifferentFrom(other: InkPoint): Boolean {
    return abs(x - other.x) >= 0.5f || abs(y - other.y) >= 0.5f
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(stroke: InkStroke, color: Color) {
    if (stroke.points.isEmpty()) {
        return
    }

    val path = Path().apply {
        val first = stroke.points.first()
        moveTo(first.x, first.y)
        stroke.points.drop(1).forEach { point ->
            lineTo(point.x, point.y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
