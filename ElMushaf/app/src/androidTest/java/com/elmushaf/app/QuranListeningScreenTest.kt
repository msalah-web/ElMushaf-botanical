package com.elmushaf.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.unit.dp

class QuranListeningScreenTest {
    @get:Rule val screen = createAndroidComposeRule<MainActivity>()
    @Test fun versesVisibleBeforePlaybackAndControlsAreIcons() {
        screen.onNodeWithText("الاستماع للقرآن").performClick()
        screen.onNodeWithText("الاستماع للقرآن").assertDoesNotExist()
        screen.onNodeWithText("مستوى الصوت", substring = true).assertDoesNotExist()
        screen.onNodeWithText("إيقاف", substring = true).assertDoesNotExist()
        screen.onNodeWithText("بِسمِ", substring = true).assertExists()
        screen.onNodeWithText("الحَمدُ", substring = true).assertExists()
        screen.onNodeWithContentDescription("تشغيل").assertExists()
        screen.onNodeWithContentDescription("السابق").assertExists()
        screen.onNodeWithContentDescription("التالي").assertExists()
        screen.onNodeWithText("المصدر: Al Quran Cloud / Islamic Network", substring = true).assertExists()
        screen.onNodeWithTag("current_ayah_marker").assertWidthIsEqualTo(4.dp).assertHeightIsEqualTo(4.dp)
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        screen.waitForIdle()
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        val output = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "audio-screen.png")
        output.outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
        screen.onNodeWithContentDescription("تشغيل").performClick()
        screen.onNodeWithContentDescription("التالي").performClick()
        screen.onNodeWithContentDescription("إيقاف مؤقت").performClick()
        screen.onNodeWithText("مشاري العفاسي").performClick()
        screen.onNodeWithText("محمود خليل الحصري").performClick()
        screen.onNodeWithText("سورة الفاتحة").performClick()
        screen.onNodeWithText("٢ — البقرة").performClick()
        screen.onNodeWithText("سورة البقرة").assertExists()
        screen.onNodeWithText("محمود خليل الحصري").performClick()
        screen.onNodeWithText("مشاري العفاسي").performClick()
        screen.onNodeWithContentDescription("تشغيل").performClick()
        screen.onNodeWithContentDescription("إيقاف مؤقت").performClick()
    }
}
