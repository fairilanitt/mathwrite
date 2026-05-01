package com.mathwrite.app.ink

import org.junit.Assert.assertEquals
import org.junit.Test

class StrokeSerializationTest {
    @Test
    fun serializesStrokeCoordinatesForMathpix() {
        val stroke = InkStroke(
            listOf(
                InkPoint(10f, 30f),
                InkPoint(20f, 40f),
            )
        )

        val json = StrokeStore.toMathpixRequestJson(listOf(stroke))
        val strokes = json.getJSONObject("strokes").getJSONObject("strokes")

        assertEquals(10, strokes.getJSONArray("x").getJSONArray(0).getInt(0))
        assertEquals(20, strokes.getJSONArray("x").getJSONArray(0).getInt(1))
        assertEquals(30, strokes.getJSONArray("y").getJSONArray(0).getInt(0))
        assertEquals(40, strokes.getJSONArray("y").getJSONArray(0).getInt(1))
    }

    @Test
    fun serializesTapOnlyMarkAsSmallStroke() {
        val stroke = InkStroke.dotAt(InkPoint(15f, 25f))

        val json = StrokeStore.toMathpixRequestJson(listOf(stroke))
        val strokes = json.getJSONObject("strokes").getJSONObject("strokes")

        assertEquals(1, strokes.getJSONArray("x").length())
        assertEquals(5, strokes.getJSONArray("x").getJSONArray(0).length())
        assertEquals(13, strokes.getJSONArray("x").getJSONArray(0).getInt(0))
        assertEquals(17, strokes.getJSONArray("x").getJSONArray(0).getInt(1))
        assertEquals(23, strokes.getJSONArray("y").getJSONArray(0).getInt(0))
        assertEquals(27, strokes.getJSONArray("y").getJSONArray(0).getInt(2))
    }
}
