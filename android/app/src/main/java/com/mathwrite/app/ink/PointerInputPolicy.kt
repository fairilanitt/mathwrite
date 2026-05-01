package com.mathwrite.app.ink

enum class InkPointerKind {
    Mouse,
    Touch,
    Stylus,
    Eraser,
    Unknown,
}

object PointerInputPolicy {
    fun accepts(kind: InkPointerKind): Boolean {
        return kind == InkPointerKind.Stylus || kind == InkPointerKind.Eraser
    }
}
