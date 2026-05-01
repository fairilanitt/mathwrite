package com.mathwrite.app.bridge

import com.mathwrite.app.format.LatexPasteMode
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CompanionBridgeClient(
    private val endpoint: String = "http://127.0.0.1:18765/paste",
) {
    fun paste(sequenceId: Long, latex: String, mode: LatexPasteMode, sessionId: String): BridgeResult {
        val body = JSONObject()
            .put("sequenceId", sequenceId)
            .put("sessionId", sessionId)
            .put("latex", latex)
            .put("mode", mode.wireName)
            .put("source", "mathwrite-android")
            .toString()

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 2_500
            readTimeout = 5_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        return try {
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }

            val responseText = readResponse(connection)
            val responseJson = JSONObject(responseText)
            BridgeResult(
                ok = responseJson.optBoolean("ok", false),
                message = responseJson.optString("message").ifBlank { null },
            )
        } catch (exception: Exception) {
            BridgeResult(false, exception.message ?: "Could not reach Mathwrite Companion.")
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

data class BridgeResult(
    val ok: Boolean,
    val message: String?,
)
