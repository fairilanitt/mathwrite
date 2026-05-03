package com.mathwrite.app.bridge

import com.mathwrite.app.format.LatexPasteMode
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class CompanionBridgeClient(
    private val endpoint: CompanionEndpoint,
) {
    fun paste(sequenceId: Long, latex: String, mode: LatexPasteMode, sessionId: String): BridgeResult {
        val body = PasteRequest(sequenceId, sessionId, latex, mode).toJson().toString()

        return postJson(endpoint.url("/paste"), body)
    }

    fun pasteSketch(sequenceId: Long, pngBytes: ByteArray, sessionId: String, tabletName: String): BridgeResult {
        val body = SketchPasteRequest(sequenceId, sessionId, pngBytes, tabletName).toJson().toString()

        return postJson(endpoint.url("/sketch"), body)
    }

    fun announceTablet(sessionId: String, tabletName: String): BridgeResult {
        val body = JSONObject()
            .put("sessionId", sessionId)
            .put("tabletName", tabletName)
            .put("source", "mathwrite-android")
            .toString()

        return postJson(endpoint.url("/tablet/hello"), body)
    }

    private fun postJson(url: String, body: String): BridgeResult {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
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
