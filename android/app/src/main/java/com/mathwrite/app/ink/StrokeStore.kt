package com.mathwrite.app.ink

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

object StrokeStore {
    fun toMathpixRequestJson(strokes: List<InkStroke>): JSONObject {
        val x = JSONArray()
        val y = JSONArray()

        strokes.filter { it.isUsable }.forEach { stroke ->
            val strokeX = JSONArray()
            val strokeY = JSONArray()

            stroke.points.forEach { point ->
                strokeX.put(point.x.roundToInt())
                strokeY.put(point.y.roundToInt())
            }

            x.put(strokeX)
            y.put(strokeY)
        }

        val inner = JSONObject()
            .put("x", x)
            .put("y", y)

        return JSONObject()
            .put("strokes", JSONObject().put("strokes", inner))
    }
}
