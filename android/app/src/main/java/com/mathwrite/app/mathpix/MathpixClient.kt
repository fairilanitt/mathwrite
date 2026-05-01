package com.mathwrite.app.mathpix

import com.mathwrite.app.ink.InkStroke
import com.mathwrite.app.ink.StrokeStore
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MathpixClient(
    private val appId: String,
    private val appKey: String,
    private val endpoint: String = "https://api.mathpix.com/v3/strokes",
) {
    fun recognize(strokes: List<InkStroke>): MathpixRecognitionResult {
        if (appId.isBlank() || appKey.isBlank()) {
            return MathpixRecognitionResult(null, null, null, "Enter Mathpix app_id and app_key.")
        }

        val body = StrokeStore.toMathpixRequestJson(strokes).toString()
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("app_id", appId.trim())
            setRequestProperty("app_key", appKey.trim())
            setRequestProperty("Content-Type", "application/json")
        }

        return try {
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }

            val responseText = readResponse(connection)
            if (connection.responseCode !in 200..299) {
                return MathpixRecognitionResult(
                    latex = null,
                    confidence = null,
                    confidenceRate = null,
                    error = "Mathpix returned HTTP ${connection.responseCode}: $responseText",
                )
            }

            LatexExtractor.extract(JSONObject(responseText))
        } catch (exception: Exception) {
            MathpixRecognitionResult(null, null, null, exception.message ?: "Mathpix request failed.")
        } finally {
            connection.disconnect()
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }
}
