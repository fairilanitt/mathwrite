package com.mathwrite.app.ui

data class TimedNotice(
    val id: Long,
    val message: String,
    val createdAtMillis: Long,
) {
    fun isVisibleAt(nowMillis: Long): Boolean = nowMillis - createdAtMillis < VisibleMillis

    companion object {
        const val VisibleMillis = 5_000L
    }
}
