package com.mathwrite.app.bridge

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CleartextManifestTest {
    @Test
    fun manifestAllowsLocalCleartextBridgeTraffic() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(
            "The LAN bridge uses cleartext HTTP on the local network, so the manifest must allow cleartext traffic.",
            manifest.contains("""android:usesCleartextTraffic="true""""),
        )
    }
}
