package com.elmushaf.app

import android.content.Context
import android.media.AudioManager
import androidx.compose.ui.test.*
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*

class QuranVolumeTest {
    @get:org.junit.Rule val compose = androidx.compose.ui.test.junit4.createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun buttonsChangeActualMediaVolume() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = AudioManager.STREAM_MUSIC
        val original = audio.getStreamVolume(stream)
        val maximum = audio.getStreamMaxVolume(stream)
        if (audio.isVolumeFixed || maximum < 2) return
        try {
            val baseline = maximum / 2
            audio.setStreamVolume(stream, baseline, 0)
            compose.setContent {
                androidx.compose.material3.MaterialTheme {
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
                    ) { QuranAudioScreen {} }
                }
            }
            compose.waitForIdle()
            assertEquals(stream, compose.activity.volumeControlStream)
            compose.onNodeWithText("▶ تشغيل").assertExists()
            compose.onNodeWithText("رفع +").performScrollTo().performClick()
            compose.waitUntil(5000) { audio.getStreamVolume(stream) == baseline + 1 }
            compose.onNodeWithText("خفض −").performScrollTo().performClick()
            compose.waitUntil(5000) { audio.getStreamVolume(stream) == baseline }
            assertEquals(baseline, audio.getStreamVolume(stream))
        } finally { audio.setStreamVolume(stream, original, 0) }
    }
}
