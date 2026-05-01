package com.mathwrite.app.mathpix

data class MathpixRecognitionResult(
    val latex: String?,
    val confidence: Double?,
    val confidenceRate: Double?,
    val error: String?,
)
