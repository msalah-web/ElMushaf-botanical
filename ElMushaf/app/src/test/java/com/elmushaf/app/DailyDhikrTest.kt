package com.elmushaf.app

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class DailyDhikrTest {
    @Test fun eachDayShowsItsRequestedReminder() {
        val expected = listOf("أستغفر الله وأتوب إليه", "سبحان الله وبحمده، سبحان الله العظيم",
            "لا إله إلا أنت سبحانك إني كنت من الظالمين", "سبحان الله وبحمده",
            "لا حول ولا قوة إلا بالله", "اللهم صلِّ وسلِّم وبارك على سيدنا محمد", "الحمد لله رب العالمين")
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) assertEquals(expected[day - 1], dailyDhikr(day))
    }
    @Test fun reminderChangesAtMidnightInPhoneTimezone() {
        val zone = TimeZone.getTimeZone("Asia/Qatar")
        val time = Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.SEPTEMBER, 18, 23, 59, 59) }
        assertEquals(Calendar.FRIDAY, localDayOfWeek(time.timeInMillis, zone))
        assertEquals(Calendar.SATURDAY, localDayOfWeek(time.timeInMillis + 1000, zone))
    }
}
