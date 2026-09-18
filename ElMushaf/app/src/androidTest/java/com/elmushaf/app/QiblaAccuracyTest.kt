package com.elmushaf.app

import android.location.Location
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*

class QiblaAccuracyTest {
    @Test fun bearingsAndArrowReference() {
        assertEquals(252.6f, calculateQibla(25.2854, 51.5310), .2f)
        assertEquals(136.1f, calculateQibla(30.0444, 31.2357), .2f)
        assertEquals(119.0f, calculateQibla(51.5074, -.1278), .2f)
        assertEquals(295.2f, calculateQibla(-6.2088, 106.8456), .2f)
        // Up is aligned, east is right, west is left. Declination is ADDED to heading.
        assertEquals(0f, qiblaArrowRotation(252f, 249f, 3f), .001f)
        assertEquals(90f, qiblaArrowRotation(90f, 0f, 0f), .001f)
        assertEquals(-90f, qiblaArrowRotation(270f, 0f, 0f), .001f)
        assertEquals(2f, qiblaArrowRotation(1f, 359f, 0f), .001f)
        assertEquals(-2f, qiblaArrowRotation(359f, 1f, 0f), .001f)
    }
    @Test fun staleAndInvalidLocationsAreRejected() {
        val value = Location("test").apply {
            latitude = 25.2854; longitude = 51.5310; accuracy = 10f
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        assertTrue(freshQiblaLocation(value))
        value.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos() - 121_000_000_000L
        assertFalse(freshQiblaLocation(value))
        value.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        value.accuracy = 20000f
        assertFalse(freshQiblaLocation(value))
        value.accuracy = 10f; value.latitude = Double.NaN
        assertFalse(freshQiblaLocation(value))
    }
}
