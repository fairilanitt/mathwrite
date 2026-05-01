package com.mathwrite.app.mathpix

import com.mathwrite.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class MathpixCredentialsTest {
    @Test
    fun credentialsComeFromBuildConfig() {
        assertEquals(BuildConfig.MATHPIX_APP_ID, MathpixCredentials.AppId)
        assertEquals(BuildConfig.MATHPIX_APP_KEY, MathpixCredentials.AppKey)
    }
}
