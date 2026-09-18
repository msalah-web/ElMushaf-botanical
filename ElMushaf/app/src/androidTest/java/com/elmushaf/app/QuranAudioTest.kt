package com.elmushaf.app

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class QuranAudioTest {
    @get:org.junit.Rule val activity = androidx.compose.ui.test.junit4.createAndroidComposeRule<MainActivity>()
    @Test fun nextVerseAppearsDuringLastSecondWithoutSeekingAudio() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var audio: QuranAudioPlayer
        instrumentation.runOnMainSync {
            audio = QuranAudioPlayer(instrumentation.targetContext)
            audio.select(audioReciters.first(), (1..7).toList())
            audio.play()
        }
        try {
            val deadline = android.os.SystemClock.elapsedRealtime() + 20000
            var ready = false
            while (!ready && android.os.SystemClock.elapsedRealtime() < deadline) {
                instrumentation.runOnMainSync { ready = audio.playing && audio.player.duration > 2000 }
                Thread.sleep(50)
            }
            assertTrue("Recitation did not become ready", ready)
            instrumentation.runOnMainSync {
                audio.player.seekTo(audio.player.duration - 1800)
                assertEquals(1, audio.displayedVerseId())
            }
            var previewed = false
            val previewDeadline = android.os.SystemClock.elapsedRealtime() + 3000
            while (!previewed && android.os.SystemClock.elapsedRealtime() < previewDeadline) {
                instrumentation.runOnMainSync {
                    if (audio.displayedVerseId() == 2 && audio.player.currentMediaItem?.mediaId == "1") {
                        assertTrue(audio.player.duration - audio.player.currentPosition <= 1000)
                        audio.pause()
                        assertEquals(1, audio.displayedVerseId())
                        previewed = true
                    }
                }
                Thread.sleep(25)
            }
            assertTrue("Next verse was not displayed before its audio", previewed)
        } finally { instrumentation.runOnMainSync { audio.close() } }
    }
    @Test fun queueStaysSmallDuringNavigationAndReciterChanges() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val audio = QuranAudioPlayer(instrumentation.targetContext)
            try {
                audio.select(audioReciters.first(), (1..7).toList())
                audio.play()
                repeat(30) {
                    audio.move(1)
                    assertEquals((it + 2).toString(), audio.player.currentMediaItem?.mediaId)
                    assertTrue(audio.player.mediaItemCount <= 12)
                }
                repeat(30) {
                    audio.move(-1)
                    assertEquals((30 - it).toString(), audio.player.currentMediaItem?.mediaId)
                    assertTrue(audio.player.mediaItemCount <= 12)
                }
                audioReciters.forEach { reciter ->
                    audio.select(reciter, listOf(8))
                    audio.play()
                    assertEquals("8", audio.player.currentMediaItem?.mediaId)
                    assertEquals(2, audio.currentSurah)
                    assertTrue(audio.player.currentMediaItem!!.mediaMetadata.title.toString().contains(reciter.name))
                    assertTrue(audio.player.mediaItemCount <= 12)
                }
                audio.select(audioReciters.first(), listOf(6234))
                audio.play()
                assertEquals(3, audio.player.mediaItemCount)
                audio.move(1); audio.move(1); audio.move(1)
                assertEquals("6236", audio.player.currentMediaItem?.mediaId)
                assertTrue(audio.player.mediaItemCount <= 12)
            } finally { audio.close() }
        }
    }
    @Test fun sourcesMatchOriginalVerseOrder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val verses = readQuran(context)
        assertEquals(6236, verses.size)
        assertEquals(10, audioReciters.size)
        assertEquals(10, audioReciters.map { it.edition }.distinct().size)
        assertEquals(1, verses.first().surah)
        assertEquals(1, verses.first().ayah)
        assertEquals(114, verses.last().surah)
        assertEquals(6, verses.last().ayah)
        audioReciters.forEach { value ->
            assertTrue(audioUrl(value, 6236).endsWith("/${value.edition}/6236.mp3"))
        }
    }
    @Test fun downloadedFatihaPlaysFromStorageAndPauses() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val reciter = audioReciters.first()
        for (id in 1..7) {
            downloadAudio(context, reciter, id)
            assertTrue(audioFile(context, reciter, id).length() > 0)
            assertFalse(java.io.File(audioFile(context, reciter, id).path + ".part").exists())
        }
        lateinit var player: QuranAudioPlayer
        instrumentation.runOnMainSync {
            player = QuranAudioPlayer(context)
            player.select(reciter, (1..7).toList()); player.play()
        }
        try {
            val deadline = android.os.SystemClock.elapsedRealtime() + 20000
            var playing = false
            while (!playing && android.os.SystemClock.elapsedRealtime() < deadline) {
                instrumentation.runOnMainSync { playing = player.playing }
                if (!playing) Thread.sleep(100)
            }
            instrumentation.runOnMainSync {
                assertTrue("Saved recitation did not play: ${player.error}", player.playing)
                assertTrue("Must never request network when this ayah is saved", player.sourceIsLocal)
                player.pause(); assertFalse(player.playing)
                player.play()
            }
            val transitionDeadline = android.os.SystemClock.elapsedRealtime() + 45000
            var transitioned = false
            while (!transitioned && android.os.SystemClock.elapsedRealtime() < transitionDeadline) {
                instrumentation.runOnMainSync { transitioned = player.index >= 2 }
                if (!transitioned) Thread.sleep(100)
            }
            instrumentation.runOnMainSync {
                assertTrue("Ayahs must advance automatically", transitioned)
                assertEquals("Transitions and resume must reuse one prepared playlist", 1, player.preparedSessions)
                assertTrue(player.sourceIsLocal)
                assertNull(player.error)
                player.stop(); assertFalse(player.playing); assertFalse(player.loading)
            }
        } finally { instrumentation.runOnMainSync { player.close() } }
    }
    @Test fun playbackContinuesWhenActivityIsInBackground() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val reciter = audioReciters.first()
        for (id in 1..7) downloadAudio(context, reciter, id)
        lateinit var player: QuranAudioPlayer
        instrumentation.runOnMainSync {
            player = QuranAudioPlayer.shared(context)
            player.select(reciter, (1..7).toList()); player.play()
        }
        try {
            activity.waitUntil(20000) { player.playing }
            activity.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            val deadline = android.os.SystemClock.elapsedRealtime() + 45000
            var continued = false
            while (!continued && android.os.SystemClock.elapsedRealtime() < deadline) {
                instrumentation.runOnMainSync { continued = player.index >= 2 && player.playing }
                if (!continued) Thread.sleep(100)
            }
            instrumentation.runOnMainSync {
                assertTrue("Playback must continue and advance after the activity stops", continued)
                assertTrue(player.sourceIsLocal)
                assertNull(player.error)
                player.stop()
            }
        } finally {
            activity.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            instrumentation.runOnMainSync { player.stop() }
        }
    }

    @Test fun fatihaAutomaticallyContinuesIntoBaqara() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        for (id in 1..8) downloadAudio(context, audioReciters.first(), id)
        lateinit var audio: QuranAudioPlayer
        instrumentation.runOnMainSync {
            audio = QuranAudioPlayer(context)
            audio.select(audioReciters.first(), (1..7).toList())
            audio.play()
            assertEquals(12, audio.player.mediaItemCount)
            audio.player.seekTo(6, 0)
        }
        try {
            activity.waitUntil(20000) { audio.playing && audio.index == 6 }
            instrumentation.runOnMainSync { audio.player.seekTo(audio.player.currentMediaItemIndex, audio.player.duration - 100) }
            activity.waitUntil(20000) { audio.currentSurah == 2 && audio.playing }
            instrumentation.runOnMainSync {
                assertEquals(0, audio.index)
                assertEquals("8", audio.player.currentMediaItem?.mediaId)
                assertEquals(1, audio.preparedSessions)
                assertTrue(audio.player.mediaItemCount <= 12)
                audio.pause(); assertFalse(audio.player.playWhenReady)
                audio.play(); assertTrue(audio.player.playWhenReady)
                audio.move(-1); assertEquals(1, audio.currentSurah); assertEquals(6, audio.index)
                audio.move(1); assertEquals(2, audio.currentSurah); assertEquals(0, audio.index)
                assertNull(audio.error)
            }
        } finally { instrumentation.runOnMainSync { audio.close() } }
    }

}
