package com.mathwrite.app.sketch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import java.io.ByteArrayOutputStream

object SketchBitmapRenderer {
    fun renderPng(
        strokes: List<SketchStroke>,
        width: Int,
        height: Int,
        backgroundArgb: Int,
    ): ByteArray {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundArgb)

        strokes.forEach { stroke ->
            drawStroke(canvas, stroke, backgroundArgb)
        }

        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
    }

    private fun drawStroke(canvas: Canvas, stroke: SketchStroke, backgroundArgb: Int) {
        if (stroke.points.isEmpty()) {
            return
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.style.effectiveColorArgb(backgroundArgb)
            strokeWidth = stroke.style.width
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val path = Path()
        val first = stroke.points.first()
        path.moveTo(first.x, first.y)
        stroke.points.drop(1).forEach { point ->
            path.lineTo(point.x, point.y)
        }

        if (stroke.points.size == 1) {
            canvas.drawPoint(first.x, first.y, paint)
        } else {
            canvas.drawPath(path, paint)
        }
    }
}
