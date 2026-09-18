package com.elmushaf.app

import org.junit.Assert.*
import org.junit.Test

class UmrahTest {
    private val prayers = List(15) { "اللهم اغفر لي $it" }

    @Test fun completingLapsIsSequentialAndCannotDoubleCount() {
        var completed = 0
        assertEquals(0, completedUmrahLap(completed, 2))
        for (lap in 0..6) {
            completed = completedUmrahLap(completed, lap)
            assertEquals(lap + 1, completed)
            assertEquals(completed, completedUmrahLap(completed, lap))
        }
        assertEquals(7, completedUmrahLap(completed, 6))
        assertEquals(6, undoUmrahLap(completed))
        assertEquals(0, undoUmrahLap(0))
    }

    @Test fun saiStartsAtSafaAndSeventhEndsAtMarwah() {
        assertEquals("الصفا إلى المروة", saiRoute(0))
        assertEquals("المروة إلى الصفا", saiRoute(1))
        assertEquals("الصفا إلى المروة", saiRoute(6))
    }

    @Test fun allPrayersRemainAvailableAndBeginningDhikrIsFirstLapOnly() {
        val first = umrahPages(UmrahMode.SAI, 0, prayers)
        val second = umrahPages(UmrahMode.SAI, 1, prayers)
        assertTrue(first.first().title.contains("أول مرة"))
        assertTrue(first[1].title.contains("على الصفا"))
        assertFalse(second.any { it.title.contains("أول مرة") })
        assertEquals("عند الوصول إلى المروة", first.last().title)
        assertEquals("عند الوصول إلى الصفا", second.last().title)
        for (mode in UmrahMode.entries) for (lap in 0..6) {
            val pages = umrahPages(mode, lap, prayers)
            assertEquals(prayers, pages.filter { it.title.startsWith("أدعية أثناء") }.flatMap { it.prayers })
        }
    }

    @Test fun tawafLocationRemindersSurroundTheSuggestedPrayers() {
        val pages = umrahPages(UmrahMode.TAWAF, 0, prayers)
        assertEquals("عند محاذاة الحجر الأسود", pages.first().title)
        assertEquals("الله أكبر", pages.first().prayers.single())
        assertEquals("بين الركن اليماني والحجر الأسود", pages.last().title)
    }
}
