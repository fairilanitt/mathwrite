package com.mathwrite.app.bridge

data class CompanionDevice(
    val name: String,
    val host: String,
    val port: Int,
) {
    val endpoint: CompanionEndpoint
        get() = CompanionEndpoint(host, port)

    val displayName: String
        get() = "$name ($host:$port)"
}
