package com.mathwrite.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimedNoticeTest {
    @Test
    fun noticeExpiresAfterFiveSeconds() {
        val notice = TimedNotice(id = 1, message = "Connected", createdAtMillis = 10_000)

        assertTrue(notice.isVisibleAt(14_999))
        assertFalse(notice.isVisibleAt(15_000))
        assertEquals(5_000L, TimedNotice.VisibleMillis)
    }
}
