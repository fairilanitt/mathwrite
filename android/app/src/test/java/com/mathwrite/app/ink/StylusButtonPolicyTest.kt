package com.mathwrite.app.ink

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StylusButtonPolicyTest {
    @Test
    fun acceptsStylusButtonPressesOnlyFromStylusLikeTools() {
        assertTrue(
            StylusButtonPolicy.isSendButtonPressed(
                StylusToolKind.Stylus,
                StylusButtonPolicy.AndroidButtonStylusPrimary,
            )
        )
        assertTrue(
            StylusButtonPolicy.isSendButtonPressed(
                StylusToolKind.Eraser,
                StylusButtonPolicy.AndroidButtonStylusSecondary,
            )
        )
        assertTrue(
            StylusButtonPolicy.isSendButtonPressed(
                StylusToolKind.Stylus,
                StylusButtonPolicy.AndroidButtonSecondary,
            )
        )
        assertFalse(
            StylusButtonPolicy.isSendButtonPressed(
                StylusToolKind.Touch,
                StylusButtonPolicy.AndroidButtonStylusPrimary,
            )
        )
        assertFalse(
            StylusButtonPolicy.isSendButtonPressed(
                StylusToolKind.Mouse,
                StylusButtonPolicy.AndroidButtonSecondary,
            )
        )
        assertFalse(
            StylusButtonPolicy.isSendButtonPressed(
                StylusToolKind.Stylus,
                0,
            )
        )
    }
}
