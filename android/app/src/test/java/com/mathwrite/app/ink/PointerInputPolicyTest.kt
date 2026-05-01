package com.mathwrite.app.ink

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointerInputPolicyTest {
    @Test
    fun acceptsOnlyStylusLikePointers() {
        assertTrue(PointerInputPolicy.accepts(InkPointerKind.Stylus))
        assertTrue(PointerInputPolicy.accepts(InkPointerKind.Eraser))
        assertFalse(PointerInputPolicy.accepts(InkPointerKind.Touch))
        assertFalse(PointerInputPolicy.accepts(InkPointerKind.Mouse))
        assertFalse(PointerInputPolicy.accepts(InkPointerKind.Unknown))
    }
}
