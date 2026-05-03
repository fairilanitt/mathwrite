package com.mathwrite.app.sketch

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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.mathwrite.app.ink.InkPoint
import com.mathwrite.app.ink.InkPointerKind
import com.mathwrite.app.ink.PointerInputPolicy
import kotlin.math.abs

@Composable
fun SketchCanvas(
    strokes: List<SketchStroke>,
    style: SketchStyle,
    backgroundArgb: Int,
    onStrokeFinished: (SketchStroke) -> Unit,
    onSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentPoints by remember { mutableStateOf<List<InkPoint>>(emptyList()) }

    Canvas(
        modifier = modifier
            .onSizeChanged(onSizeChanged)
            .pointerInput(style) {
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
                            val stroke = SketchStroke(points.toList(), style)
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
        drawRect(Color(backgroundArgb))
        strokes.forEach { stroke -> drawSketchStroke(stroke, backgroundArgb) }
        if (currentPoints.isNotEmpty()) {
            drawSketchStroke(SketchStroke(currentPoints, style), backgroundArgb)
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

private fun InkPoint.isMeaningfullyDifferentFrom(other: InkPoint): Boolean {
    return abs(x - other.x) >= 0.5f || abs(y - other.y) >= 0.5f
}

private fun DrawScope.drawSketchStroke(stroke: SketchStroke, backgroundArgb: Int) {
    if (stroke.points.isEmpty()) {
        return
    }

    val color = Color(stroke.style.effectiveColorArgb(backgroundArgb))
    val width = stroke.style.width

    if (stroke.points.size == 1) {
        val point = stroke.points.first()
        drawCircle(color = color, radius = width / 2f, center = Offset(point.x, point.y))
        return
    }

    val path = Path().apply {
        val first = stroke.points.first()
        moveTo(first.x, first.y)
        stroke.points.drop(1).forEach { point -> lineTo(point.x, point.y) }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
