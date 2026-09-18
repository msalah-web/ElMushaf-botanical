package com.elmushaf.app

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class FridayReminderTest {
    private val zone = TimeZone.getTimeZone("Asia/Qatar")
    private fun moment(day: Int, hour: Int, minute: Int, second: Int): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(2026, Calendar.SEPTEMBER, day, hour, minute, second)
        }.timeInMillis

    @Test fun onlyFridayBetweenLocalMidnights() {
        assertFalse(isFriday(moment(17, 23, 59, 59), zone))
        assertTrue(isFriday(moment(18, 0, 0, 0), zone))
        assertTrue(isFriday(moment(18, 23, 59, 59), zone))
        assertFalse(isFriday(moment(19, 0, 0, 0), zone))
        assertFalse(isFriday(moment(20, 12, 0, 0), zone))
        val instant = moment(18, 0, 0, 0)
        assertFalse(isFriday(instant, TimeZone.getTimeZone("UTC")))
    }
}
