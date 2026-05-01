package com.mathwrite.app.ink

enum class StylusToolKind {
    Stylus,
    Eraser,
    Touch,
    Mouse,
    Unknown,
}

object StylusButtonPolicy {
    const val AndroidButtonSecondary = 0x00000002
    const val AndroidButtonTertiary = 0x00000004
    const val AndroidButtonStylusPrimary = 0x00000020
    const val AndroidButtonStylusSecondary = 0x00000040

    fun isSendButtonPressed(toolKind: StylusToolKind, buttonState: Int): Boolean {
        if (toolKind != StylusToolKind.Stylus && toolKind != StylusToolKind.Eraser) {
            return false
        }

        val sendButtons = AndroidButtonStylusPrimary or
            AndroidButtonStylusSecondary or
            AndroidButtonSecondary or
            AndroidButtonTertiary

        return buttonState and sendButtons != 0
    }
}
