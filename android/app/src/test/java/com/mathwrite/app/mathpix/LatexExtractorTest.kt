package com.mathwrite.app.mathpix

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexExtractorTest {
    @Test
    fun prefersLatexStyled() {
        val result = LatexExtractor.extract(
            JSONObject("""{"latex_styled":"3 x^{2}","confidence":0.9,"confidence_rate":0.8}""")
        )

        assertEquals("3 x^{2}", result.latex)
        assertEquals(0.9, result.confidence!!, 0.001)
        assertEquals(0.8, result.confidenceRate!!, 0.001)
    }

    @Test
    fun fallsBackToLatexDataItem() {
        val result = LatexExtractor.extract(
            JSONObject("""{"data":[{"type":"latex","value":"x^{2}+1"}]}""")
        )

        assertEquals("x^{2}+1", result.latex)
    }

    @Test
    fun stripsInlineDelimitersFromTextFallback() {
        val result = LatexExtractor.extract(
            JSONObject("""{"text":"\\( y^{2} \\)"}""")
        )

        assertEquals("y^{2}", result.latex)
    }

    @Test
    fun returnsErrorWhenNoLatexExists() {
        val result = LatexExtractor.extract(JSONObject("""{"text":""}"""))

        assertTrue(result.error!!.contains("LaTeX"))
    }
}
