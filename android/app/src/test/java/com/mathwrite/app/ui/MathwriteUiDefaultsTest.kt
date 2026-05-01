package com.mathwrite.app.ui

import com.mathwrite.app.format.LatexPasteMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MathwriteUiDefaultsTest {
    @Test
    fun defaultPasteModeIsRaw() {
        assertEquals(LatexPasteMode.Raw, MathwriteUiDefaults.DefaultPasteMode)
    }
}
