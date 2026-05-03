package com.mathwrite.app.bridge

data class ConnectionUiState(
    val endpoint: CompanionEndpoint?,
    val isActive: Boolean,
    val setupManuallyExpanded: Boolean,
) {
    val shouldShowSetup: Boolean
        get() = setupManuallyExpanded || !isActive

    val statusText: String
        get() = when {
            endpoint == null -> "Inactive: no laptop selected"
            isActive -> "Active: ${endpoint.displayName}"
            else -> "Inactive: ${endpoint.displayName}"
        }
}
