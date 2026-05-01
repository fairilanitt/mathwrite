package com.mathwrite.app.ink

data class InkPoint(
    val x: Float,
    val y: Float,
)

data class InkStroke(
    val points: List<InkPoint>,
) {
    val isUsable: Boolean
        get() = points.size >= 2

    companion object {
        fun dotAt(center: InkPoint, radius: Float = 2f): InkStroke {
            return InkStroke(
                listOf(
                    InkPoint(center.x - radius, center.y - radius),
                    InkPoint(center.x + radius, center.y - radius),
                    InkPoint(center.x + radius, center.y + radius),
                    InkPoint(center.x - radius, center.y + radius),
                    InkPoint(center.x - radius, center.y - radius),
                )
            )
        }
    }
}
