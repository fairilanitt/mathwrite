package com.mathwrite.app.bridge

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CleartextManifestTest {
    @Test
    fun manifestAllowsLocalCleartextBridgeTraffic() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(
            "The USB bridge uses http://127.0.0.1:18765, so the manifest must allow cleartext traffic.",
            manifest.contains("""android:usesCleartextTraffic="true""""),
        )
    }
}
