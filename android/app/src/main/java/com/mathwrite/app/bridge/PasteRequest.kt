package com.mathwrite.app.bridge

import com.mathwrite.app.format.LatexPasteMode
import org.json.JSONObject

data class PasteRequest(
    val sequenceId: Long,
    val sessionId: String,
    val latex: String,
    val mode: LatexPasteMode,
    val source: String = "mathwrite-android",
) {
    fun toJson(): JSONObject =
        JSONObject()
            .put("sequenceId", sequenceId)
            .put("sessionId", sessionId)
            .put("latex", latex)
            .put("mode", mode.wireName)
            .put("source", source)
}
