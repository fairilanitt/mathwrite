package com.mathwrite.app.bridge

import com.mathwrite.app.format.LatexPasteMode

data class PasteRequest(
    val sequenceId: Long,
    val sessionId: String,
    val latex: String,
    val mode: LatexPasteMode,
    val source: String = "mathwrite-android",
)
