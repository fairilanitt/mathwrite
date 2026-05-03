package com.mathwrite.app.bridge

import com.mathwrite.app.format.LatexPasteMode
import org.junit.Assert.assertEquals
import org.junit.Test

class CompanionEndpointTest {
    @Test
    fun buildsLanUrlsFromHostAndPort() {
        val endpoint = CompanionEndpoint("192.168.1.20", 18765)

        assertEquals("http://192.168.1.20:18765/paste", endpoint.url("/paste"))
        assertEquals("http://192.168.1.20:18765/sketch", endpoint.url("sketch"))
        assertEquals("192.168.1.20:18765", endpoint.displayName)
    }

    @Test
    fun serializesTextPasteRequestForCompanion() {
        val request = PasteRequest(
            sequenceId = 4,
            sessionId = "tablet-session",
            latex = "x^2",
            mode = LatexPasteMode.Raw,
        )

        val json = request.toJson()

        assertEquals(4, json.getLong("sequenceId"))
        assertEquals("tablet-session", json.getString("sessionId"))
        assertEquals("x^2", json.getString("latex"))
        assertEquals("raw", json.getString("mode"))
        assertEquals("mathwrite-android", json.getString("source"))
    }

    @Test
    fun serializesSketchPasteRequestForCompanion() {
        val request = SketchPasteRequest(
            sequenceId = 5,
            sessionId = "tablet-session",
            pngBytes = byteArrayOf(1, 2, 3),
            tabletName = "Galaxy Tab S8+",
        )

        val json = request.toJson()

        assertEquals(5, json.getLong("sequenceId"))
        assertEquals("tablet-session", json.getString("sessionId"))
        assertEquals("AQID", json.getString("pngBase64"))
        assertEquals("Galaxy Tab S8+", json.getString("tabletName"))
        assertEquals("mathwrite-android", json.getString("source"))
    }
}
