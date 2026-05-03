package com.mathwrite.app.bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

class CompanionDiscoveryClient(
    private val port: Int = 18765,
    private val timeoutMillis: Int = 220,
) {
    suspend fun scan(): List<CompanionDevice> = coroutineScope {
        candidateHosts()
            .map { host -> async(Dispatchers.IO) { probe(host) } }
            .awaitAll()
            .filterNotNull()
            .distinctBy { "${it.host}:${it.port}" }
            .sortedBy { it.host }
    }

    private fun probe(host: String): CompanionDevice? {
        val connection = (URL("http://$host:$port/hello").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
        }

        return try {
            if (connection.responseCode !in 200..299) {
                return null
            }

            val text = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it.readText() }
            val json = JSONObject(text)
            CompanionDevice(
                name = json.optString("name").ifBlank { "Mathwrite Companion" },
                host = host,
                port = json.optInt("port", port),
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun candidateHosts(): List<String> {
        val ownAddresses = localIPv4Addresses()
        return ownAddresses.flatMap { address ->
            val prefix = address.substringBeforeLast('.', missingDelimiterValue = "")
            if (prefix.isBlank()) {
                emptyList()
            } else {
                (1..254).map { "$prefix.$it" }.filterNot { it == address }
            }
        }.distinct()
    }

    private fun localIPv4Addresses(): List<String> =
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { networkInterface -> networkInterface.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .mapNotNull { it.hostAddress }
            .filterNot { it.startsWith("127.") }
            .toList()
}
