package com.mathwrite.app.sketch

import org.junit.Assert.assertEquals
import org.junit.Test

class SketchStyleTest {
    @Test
    fun penUsesSelectedColor() {
        val style = SketchStyle(colorArgb = 0xFFDC2626.toInt(), width = 12f, tool = SketchTool.Pen)

        assertEquals(0xFFDC2626.toInt(), style.effectiveColorArgb(0xFFFFFFFF.toInt()))
    }

    @Test
    fun highlighterUsesTransparentSelectedColor() {
        val style = SketchStyle(colorArgb = 0xFF2563EB.toInt(), width = 18f, tool = SketchTool.Highlighter)

        assertEquals(0x662563EB, style.effectiveColorArgb(0xFFFFFFFF.toInt()))
    }

    @Test
    fun eraserUsesCanvasBackgroundColor() {
        val style = SketchStyle(colorArgb = 0xFF111827.toInt(), width = 24f, tool = SketchTool.Eraser)

        assertEquals(0xFFF8FAFC.toInt(), style.effectiveColorArgb(0xFFF8FAFC.toInt()))
    }
}
