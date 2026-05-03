package com.mathwrite.app.sketch

import com.mathwrite.app.ink.InkPoint

data class SketchStroke(
    val points: List<InkPoint>,
    val style: SketchStyle,
) {
    val isUsable: Boolean
        get() = points.isNotEmpty()
}
