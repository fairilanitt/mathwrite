package com.mathwrite.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class MathwriteIcon {
    CheckConnection,
    ChangeConnection,
    Connect,
    Scan,
    MathMode,
    SketchMode,
    Pen,
    StylusOnly,
    Undo,
    Clear,
    SendLatex,
    SendSketch,
    More,
    Highlighter,
    Eraser,
    Fill,
}

@Composable
fun ToolbarStrip(content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun IconCircleButton(
    icon: MathwriteIcon,
    contentDescription: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val background = when {
        selected -> Color.Black
        enabled -> Color.White
        else -> Color(0xFFE5E7EB)
    }
    val tint = when {
        selected -> Color.White
        enabled -> Color.Black
        else -> Color(0xFF94A3B8)
    }

    Surface(
        modifier = Modifier
            .size(46.dp)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
                if (!enabled) disabled()
            }
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = background,
        tonalElevation = if (selected) 0.dp else 1.dp,
        shadowElevation = if (selected) 0.dp else 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
    ) {
        MathwriteGlyph(
            icon = icon,
            tint = tint,
            modifier = Modifier
                .padding(11.dp)
                .size(24.dp),
        )
    }
}

@Composable
fun MathwriteGlyph(
    icon: MathwriteIcon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (icon) {
            MathwriteIcon.CheckConnection -> drawCheckConnection(tint)
            MathwriteIcon.ChangeConnection -> drawChangeConnection(tint)
            MathwriteIcon.Connect -> drawConnect(tint)
            MathwriteIcon.Scan -> drawScan(tint)
            MathwriteIcon.MathMode -> drawMathMode(tint)
            MathwriteIcon.SketchMode -> drawSketchMode(tint)
            MathwriteIcon.Pen -> drawPen(tint)
            MathwriteIcon.StylusOnly -> drawStylusOnly(tint)
            MathwriteIcon.Undo -> drawUndo(tint)
            MathwriteIcon.Clear -> drawClear(tint)
            MathwriteIcon.SendLatex -> drawSendLatex(tint)
            MathwriteIcon.SendSketch -> drawSendSketch(tint)
            MathwriteIcon.More -> drawMore(tint)
            MathwriteIcon.Highlighter -> drawHighlighter(tint)
            MathwriteIcon.Eraser -> drawEraser(tint)
            MathwriteIcon.Fill -> drawFill(tint)
        }
    }
}

private fun DrawScope.stroke(color: Color, width: Float = size.minDimension * 0.095f) =
    Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

private fun DrawScope.drawCheckConnection(color: Color) {
    drawCircle(color, radius = size.minDimension * 0.36f, style = stroke(color))
    drawLine(color, Offset(size.width * 0.32f, size.height * 0.52f), Offset(size.width * 0.45f, size.height * 0.65f), strokeWidth = size.minDimension * 0.1f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.45f, size.height * 0.65f), Offset(size.width * 0.70f, size.height * 0.38f), strokeWidth = size.minDimension * 0.1f, cap = StrokeCap.Round)
}

private fun DrawScope.drawChangeConnection(color: Color) {
    drawLine(color, Offset(size.width * 0.28f, size.height * 0.35f), Offset(size.width * 0.72f, size.height * 0.35f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.62f, size.height * 0.23f), Offset(size.width * 0.75f, size.height * 0.35f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.62f, size.height * 0.47f), Offset(size.width * 0.75f, size.height * 0.35f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.72f, size.height * 0.66f), Offset(size.width * 0.28f, size.height * 0.66f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.38f, size.height * 0.54f), Offset(size.width * 0.25f, size.height * 0.66f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.38f, size.height * 0.78f), Offset(size.width * 0.25f, size.height * 0.66f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
}

private fun DrawScope.drawConnect(color: Color) {
    drawCircle(color, radius = size.minDimension * 0.14f, center = Offset(size.width * 0.30f, size.height * 0.50f), style = stroke(color))
    drawCircle(color, radius = size.minDimension * 0.14f, center = Offset(size.width * 0.70f, size.height * 0.50f), style = stroke(color))
    drawLine(color, Offset(size.width * 0.43f, size.height * 0.50f), Offset(size.width * 0.57f, size.height * 0.50f), strokeWidth = size.minDimension * 0.1f, cap = StrokeCap.Round)
}

private fun DrawScope.drawScan(color: Color) {
    drawCircle(color, radius = size.minDimension * 0.24f, center = Offset(size.width * 0.43f, size.height * 0.43f), style = stroke(color))
    drawLine(color, Offset(size.width * 0.60f, size.height * 0.60f), Offset(size.width * 0.78f, size.height * 0.78f), strokeWidth = size.minDimension * 0.1f, cap = StrokeCap.Round)
    drawCircle(color, radius = size.minDimension * 0.04f, center = Offset(size.width * 0.43f, size.height * 0.43f))
}

private fun DrawScope.drawMathMode(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.24f, size.height * 0.34f)
        lineTo(size.width * 0.44f, size.height * 0.66f)
        lineTo(size.width * 0.78f, size.height * 0.24f)
    }
    drawPath(path, color, style = stroke(color))
    drawLine(color, Offset(size.width * 0.55f, size.height * 0.66f), Offset(size.width * 0.78f, size.height * 0.66f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
}

private fun DrawScope.drawSketchMode(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.22f, size.height * 0.62f)
        cubicTo(size.width * 0.34f, size.height * 0.25f, size.width * 0.50f, size.height * 0.82f, size.width * 0.64f, size.height * 0.40f)
        cubicTo(size.width * 0.70f, size.height * 0.24f, size.width * 0.77f, size.height * 0.34f, size.width * 0.80f, size.height * 0.48f)
    }
    drawPath(path, color, style = stroke(color))
}

private fun DrawScope.drawPen(color: Color) {
    drawLine(color, Offset(size.width * 0.27f, size.height * 0.74f), Offset(size.width * 0.68f, size.height * 0.26f), strokeWidth = size.minDimension * 0.13f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.22f, size.height * 0.79f), Offset(size.width * 0.35f, size.height * 0.72f), strokeWidth = size.minDimension * 0.08f, cap = StrokeCap.Round)
}

private fun DrawScope.drawStylusOnly(color: Color) {
    drawPen(color)
    drawCircle(color, radius = size.minDimension * 0.08f, center = Offset(size.width * 0.74f, size.height * 0.24f), style = stroke(color, size.minDimension * 0.07f))
}

private fun DrawScope.drawUndo(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.32f, size.height * 0.42f)
        cubicTo(size.width * 0.45f, size.height * 0.22f, size.width * 0.78f, size.height * 0.38f, size.width * 0.66f, size.height * 0.66f)
    }
    drawPath(path, color, style = stroke(color))
    drawLine(color, Offset(size.width * 0.33f, size.height * 0.42f), Offset(size.width * 0.50f, size.height * 0.32f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.33f, size.height * 0.42f), Offset(size.width * 0.43f, size.height * 0.58f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
}

private fun DrawScope.drawClear(color: Color) {
    drawRoundRect(color, topLeft = Offset(size.width * 0.32f, size.height * 0.35f), size = Size(size.width * 0.36f, size.height * 0.42f), cornerRadius = CornerRadius(size.minDimension * 0.06f), style = stroke(color))
    drawLine(color, Offset(size.width * 0.28f, size.height * 0.30f), Offset(size.width * 0.72f, size.height * 0.30f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.42f, size.height * 0.22f), Offset(size.width * 0.58f, size.height * 0.22f), strokeWidth = size.minDimension * 0.08f, cap = StrokeCap.Round)
}

private fun DrawScope.drawSendLatex(color: Color) {
    val plane = Path().apply {
        moveTo(size.width * 0.18f, size.height * 0.50f)
        lineTo(size.width * 0.82f, size.height * 0.22f)
        lineTo(size.width * 0.62f, size.height * 0.78f)
        lineTo(size.width * 0.48f, size.height * 0.57f)
        close()
    }
    drawPath(plane, color, style = stroke(color))
}

private fun DrawScope.drawSendSketch(color: Color) {
    drawRoundRect(color, topLeft = Offset(size.width * 0.20f, size.height * 0.28f), size = Size(size.width * 0.45f, size.height * 0.42f), cornerRadius = CornerRadius(size.minDimension * 0.06f), style = stroke(color))
    drawLine(color, Offset(size.width * 0.58f, size.height * 0.50f), Offset(size.width * 0.82f, size.height * 0.50f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.72f, size.height * 0.38f), Offset(size.width * 0.84f, size.height * 0.50f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.72f, size.height * 0.62f), Offset(size.width * 0.84f, size.height * 0.50f), strokeWidth = size.minDimension * 0.09f, cap = StrokeCap.Round)
}

private fun DrawScope.drawMore(color: Color) {
    drawCircle(color, radius = size.minDimension * 0.07f, center = Offset(size.width * 0.50f, size.height * 0.28f))
    drawCircle(color, radius = size.minDimension * 0.07f, center = Offset(size.width * 0.50f, size.height * 0.50f))
    drawCircle(color, radius = size.minDimension * 0.07f, center = Offset(size.width * 0.50f, size.height * 0.72f))
}

private fun DrawScope.drawHighlighter(color: Color) {
    drawLine(color, Offset(size.width * 0.30f, size.height * 0.68f), Offset(size.width * 0.66f, size.height * 0.30f), strokeWidth = size.minDimension * 0.18f, cap = StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.22f, size.height * 0.78f), Offset(size.width * 0.58f, size.height * 0.78f), strokeWidth = size.minDimension * 0.08f, cap = StrokeCap.Round)
}

private fun DrawScope.drawEraser(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.25f, size.height * 0.63f)
        lineTo(size.width * 0.52f, size.height * 0.30f)
        lineTo(size.width * 0.75f, size.height * 0.48f)
        lineTo(size.width * 0.49f, size.height * 0.78f)
        close()
    }
    drawPath(path, color, style = stroke(color))
}

private fun DrawScope.drawFill(color: Color) {
    val bucket = Path().apply {
        moveTo(size.width * 0.30f, size.height * 0.38f)
        lineTo(size.width * 0.54f, size.height * 0.22f)
        lineTo(size.width * 0.76f, size.height * 0.46f)
        lineTo(size.width * 0.46f, size.height * 0.70f)
        close()
    }
    drawPath(bucket, color, style = stroke(color))
    drawCircle(color, radius = size.minDimension * 0.07f, center = Offset(size.width * 0.72f, size.height * 0.72f))
}
