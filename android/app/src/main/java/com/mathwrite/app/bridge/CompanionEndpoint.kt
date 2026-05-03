package com.mathwrite.app.bridge

data class CompanionEndpoint(
    val host: String,
    val port: Int = 18765,
) {
    val displayName: String
        get() = "${host.trim()}:$port"

    fun url(path: String): String {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "http://${host.trim()}:$port$normalizedPath"
    }
}
