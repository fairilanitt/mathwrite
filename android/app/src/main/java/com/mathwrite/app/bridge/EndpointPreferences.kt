package com.mathwrite.app.bridge

import android.content.Context

class EndpointPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("mathwrite-endpoint", Context.MODE_PRIVATE)

    fun load(): CompanionEndpoint? {
        val host = preferences.getString("host", null)?.trim().orEmpty()
        val port = preferences.getInt("port", 18765)
        return if (host.isBlank()) null else CompanionEndpoint(host, port)
    }

    fun save(endpoint: CompanionEndpoint) {
        preferences.edit()
            .putString("host", endpoint.host.trim())
            .putInt("port", endpoint.port)
            .apply()
    }
}
