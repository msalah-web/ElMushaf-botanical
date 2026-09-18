package com.elmushaf.app

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import androidx.compose.ui.test.*
import java.io.File

class QuranReaderDesignTest {
    @get:org.junit.Rule val compose = androidx.compose.ui.test.junit4.createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun actualReaderScreens() {
        val page = androidx.compose.runtime.mutableStateOf(305)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        compose.setContent { androidx.compose.material3.MaterialTheme { androidx.compose.runtime.key(page.value) { QuranReaderScreen(19, {}, page.value) } } }
        for (number in listOf(2, 3, 305, 4)) {
            compose.runOnIdle { page.value = number }
            compose.waitForIdle()
            compose.onNodeWithTag("font-slider").assertDoesNotExist()
            compose.onNodeWithTag("reader-controls").assertDoesNotExist()
            val pageNode = compose.onNodeWithTag("mushaf-page-$number")
            val fullBounds = pageNode.fetchSemanticsNode().boundsInRoot
            pageNode.performTouchInput { click(center) }
            compose.onNodeWithText("تغيير الخط").assertDoesNotExist()
            compose.onNodeWithContentDescription("تصغير خط المصحف").assertDoesNotExist()
            compose.onNodeWithContentDescription("تكبير خط المصحف").assertDoesNotExist()
            compose.onNodeWithText("التفسير").performClick()
            compose.onNodeWithText("التفسير الميسّر").assertIsDisplayed()
            compose.onNodeWithText("إغلاق").performClick()
            pageNode.performTouchInput { click(center) }
            check(pageNode.fetchSemanticsNode().boundsInRoot == fullBounds)
            compose.onNodeWithText("إخفاء").performClick()
            compose.onNodeWithTag("reader-controls").assertDoesNotExist()
            compose.waitForIdle()
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            File(context.getExternalFilesDir(null), "screen-page-$number.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun dedicationImageAndActions() {
        compose.setContent { androidx.compose.material3.MaterialTheme { DedicationScreen({}, {}, {}, {}) } }
        compose.onNodeWithText("بدء القراءة").assertIsDisplayed()
        compose.onNodeWithText("متابعة القراءة").assertIsDisplayed()
        compose.onNodeWithText("فهرس السور").assertIsDisplayed()
        compose.onNodeWithTag("dedication-back").assertDoesNotExist()
        compose.onNodeWithTag("dedication-image").performClick()
        compose.onNodeWithTag("dedication-back").assertIsDisplayed()
        compose.onNodeWithTag("dedication-image").performClick()
        compose.onNodeWithTag("dedication-back").assertDoesNotExist()
        val imageBounds = compose.onNodeWithTag("dedication-image").fetchSemanticsNode().boundsInRoot
        val screenBounds = compose.onRoot().fetchSemanticsNode().boundsInRoot
        check(imageBounds.height >= screenBounds.height * .70f)
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "new-dedication.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun nativePagesFitAndPreserveSource() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val pages = readQuran(context).groupBy { it.page }
        check(pages.size == 604)
        val sizes = mutableMapOf<Int, Int>()
        lateinit var allPagesView: MushafPageView
        instrumentation.runOnMainSync {
            allPagesView = MushafPageView(context)
            allPagesView.layout(0, 0, 720, 1100)
        }
        for ((number, rows) in pages) {
            instrumentation.runOnMainSync {
                allPagesView.show(readerBlocks(rows), number, "", "", "")
                check(allPagesView.contentFits) { "Page $number overflows" }
                check(!allPagesView.canScrollVertically(1) && !allPagesView.canScrollVertically(-1))
                sizes[number] = allPagesView.displayedFontPx
            }
        }
        instrumentation.runOnMainSync {
            check(sizes.getValue(1) > sizes.getValue(3)) { "Sparse page should use larger text" }
            for (number in listOf(1, 2, 3, 4, 8, 305, 520, 604)) {
                val rows = pages.getValue(number)
                val blocks = readerBlocks(rows)
                // After removing only presentation numbers and the user-requested star,
                // each source verse must remain intact, including its marks and spaces.
                val combined = blocks.joinToString(" ") { listOfNotNull(it.basmala, it.text).joinToString("") }
                rows.forEach { check(combined.contains(it.text.replace("\u06DE", ""))) }
                val view = MushafPageView(context)
                view.show(blocks, number, "الجزء ${arabicNumber(rows.first().juz)}", surahs[rows.first().surah - 1], "الحزب ${arabicNumber(rows.first().hizb)}")
                view.layout(0, 0, 720, 1100)
                check(view.contentFits) { "Page $number does not fit" }
                val portraitSize = view.displayedFontPx
                view.layout(0, 0, 720, 700)
                check(view.contentFits && view.displayedFontPx <= portraitSize)
                view.layout(0, 0, 720, 1100)
                check(view.contentFits && view.displayedFontPx == portraitSize)
                android.util.Log.i("QuranFit", "page=$number fontPx=$portraitSize fits=${view.contentFits}")
                check(!view.renderedText.contains('\u06DE'))
                val bitmap = Bitmap.createBitmap(720, 1100, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                File(context.getExternalFilesDir(null), "rebuilt-page-$number.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }
}
