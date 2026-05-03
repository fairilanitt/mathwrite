package com.mathwrite.app.sketch

data class SketchStyle(
    val colorArgb: Int,
    val width: Float,
    val tool: SketchTool,
) {
    fun effectiveColorArgb(backgroundArgb: Int): Int =
        when (tool) {
            SketchTool.Pen -> colorArgb
            SketchTool.Highlighter -> (colorArgb and 0x00FFFFFF) or 0x66000000
            SketchTool.Eraser -> backgroundArgb
        }
}
