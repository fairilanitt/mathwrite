package com.mathwrite.app.mathpix

import org.json.JSONObject

object LatexExtractor {
    fun extract(json: JSONObject): MathpixRecognitionResult {
        val latexStyled = json.optString("latex_styled").trim()
        val latex = latexStyled.ifBlank { latexFromData(json) ?: latexFromText(json) }

        if (latex.isNullOrBlank()) {
            return MathpixRecognitionResult(
                latex = null,
                confidence = json.optionalDouble("confidence"),
                confidenceRate = json.optionalDouble("confidence_rate"),
                error = "Mathpix did not return LaTeX.",
            )
        }

        return MathpixRecognitionResult(
            latex = latex.trim(),
            confidence = json.optionalDouble("confidence"),
            confidenceRate = json.optionalDouble("confidence_rate"),
            error = null,
        )
    }

    private fun latexFromData(json: JSONObject): String? {
        val data = json.optJSONArray("data") ?: return null

        for (index in 0 until data.length()) {
            val item = data.optJSONObject(index) ?: continue
            if (item.optString("type") == "latex") {
                return item.optString("value").trim().ifBlank { null }
            }
        }

        return null
    }

    private fun latexFromText(json: JSONObject): String? {
        val text = json.optString("text").trim()
        if (text.startsWith("\\(") && text.endsWith("\\)")) {
            return text.removePrefix("\\(").removeSuffix("\\)").trim()
        }

        if (text.startsWith("\\[") && text.endsWith("\\]")) {
            return text.removePrefix("\\[").removeSuffix("\\]").trim()
        }

        return text.ifBlank { null }
    }

    private fun JSONObject.optionalDouble(name: String): Double? {
        return if (has(name) && !isNull(name)) optDouble(name) else null
    }
}
