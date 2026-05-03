package com.mathwrite.app.bridge

import org.json.JSONObject
import java.util.Base64

data class SketchPasteRequest(
    val sequenceId: Long,
    val sessionId: String,
    val pngBytes: ByteArray,
    val tabletName: String,
    val source: String = "mathwrite-android",
) {
    fun toJson(): JSONObject =
        JSONObject()
            .put("sequenceId", sequenceId)
            .put("sessionId", sessionId)
            .put("pngBase64", Base64.getEncoder().encodeToString(pngBytes))
            .put("tabletName", tabletName)
            .put("source", source)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SketchPasteRequest

        if (sequenceId != other.sequenceId) return false
        if (sessionId != other.sessionId) return false
        if (!pngBytes.contentEquals(other.pngBytes)) return false
        if (tabletName != other.tabletName) return false
        if (source != other.source) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sequenceId.hashCode()
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + pngBytes.contentHashCode()
        result = 31 * result + tabletName.hashCode()
        result = 31 * result + source.hashCode()
        return result
    }
}
