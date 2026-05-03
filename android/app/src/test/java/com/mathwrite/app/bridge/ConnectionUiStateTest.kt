package com.mathwrite.app.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionUiStateTest {
    @Test
    fun hidesSetupWhenConnectionIsActive() {
        val state = ConnectionUiState(
            endpoint = CompanionEndpoint("192.168.0.132", 18765),
            isActive = true,
            setupManuallyExpanded = false,
        )

        assertFalse(state.shouldShowSetup)
        assertEquals("Active: 192.168.0.132:18765", state.statusText)
    }

    @Test
    fun showsSetupWhenConnectionIsInactive() {
        val state = ConnectionUiState(
            endpoint = CompanionEndpoint("192.168.0.132", 18765),
            isActive = false,
            setupManuallyExpanded = false,
        )

        assertTrue(state.shouldShowSetup)
        assertEquals("Inactive: 192.168.0.132:18765", state.statusText)
    }

    @Test
    fun showsSetupWhenUserExpandsItEvenIfActive() {
        val state = ConnectionUiState(
            endpoint = CompanionEndpoint("192.168.0.132", 18765),
            isActive = true,
            setupManuallyExpanded = true,
        )

        assertTrue(state.shouldShowSetup)
    }
}
