package com.elmushaf.app

import android.content.Context
import android.graphics.text.LineBreaker
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun arabicNumber(value: Int): String = value.toString().map {
    if (it in '0'..'9') "٠١٢٣٤٥٦٧٨٩"[it - '0'] else it
}.joinToString("")

internal data class QuranVerse(val surah: Int, val ayah: Int, val text: String,
    val page: Int, val quarter: Int, val juz: Int) {
    val hizb: Int get() = (quarter + 3) / 4
}

internal fun readQuran(context: Context): List<QuranVerse> {
    val positions = context.assets.open("quran-pagination.txt").bufferedReader().useLines { lines ->
        lines.filter { it.isNotBlank() }.map { it.split('|').map(String::toInt) }
            .associateBy { it[0] to it[1] }
    }
    return context.assets.open("quran-uthmani.txt").bufferedReader().useLines { lines ->
        lines.mapNotNull { line ->
            val p = line.split('|', limit = 3)
            val s = p.getOrNull(0)?.toIntOrNull()
            val a = p.getOrNull(1)?.toIntOrNull()
            if (s == null || a == null || p.size != 3) null else {
                val m = positions.getValue(s to a)
                QuranVerse(s, a, p[2], m[2], m[3], m[4])
            }
        }.toList()
    }
}

private val pageInk = Color(0xFF172019)
private val ayahInk = Color(0xFF0F6B5D)
private val pagePaper = Color.White
private val pageGold = MushafGold
private val quranFont = androidx.compose.ui.text.font.FontFamily.Default
private fun verseText(verse: QuranVerse) = "${verse.text.replace("\u06DE", "")} ﴿${arabicNumber(verse.ayah)}﴾"
private fun decoratedVerses(text: String) = androidx.compose.ui.text.AnnotatedString(text)

internal data class ReaderBlock(val title: String?, val basmala: String?, val text: String)

// Split only at source boundaries. No normalization, mark removal, or replacement basmala.
internal fun readerBlocks(verses: List<QuranVerse>): List<ReaderBlock> = verses.groupBy { it.surah }.map { (surah, rows) ->
    val first = rows.first()
    val opening = if (first.ayah == 1 && surah != 1 && surah != 9)
        Regex("\\S+").findAll(first.text).filter { it.value != "۞" }.take(4).lastOrNull()?.range?.last?.plus(1) else null
    ReaderBlock(if (first.ayah == 1) "سورة ${surahs[surah - 1]}" else null,
        opening?.let { first.text.substring(0, it).replace("\u06DE", "") },
        rows.joinToString(" ") { row ->
            val text = if (row === first && opening != null) row.text.substring(opening) else row.text
            "${text.replace("\u06DE", "")} ﴿${arabicNumber(row.ayah)}﴾"
        })
}

// Native Android text layout: measure and draw each exact same layout with its own paint.
// Fit the original text without scaling glyphs; justify words on supported Android versions.
internal class MushafPageView(context: Context) : android.view.View(context) {
    private var blocks = emptyList<ReaderBlock>()
    private var layouts = emptyList<Pair<ReaderBlock, List<android.text.StaticLayout>>>()
    private val density = resources.displayMetrics.density
    private val mushafTypeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.amiri_quran)
    private val surahFrame by lazy {
        android.graphics.BitmapFactory.decodeResource(resources, R.drawable.surah_frame_new)
    }
    private val framePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
    private var extraLineSpace = 0f
    private var topRight = ""
    private var topLeft = ""
    private var bottomRight = ""
    private var page = 1
    private var fontFraction = 1f
    private var fittedSizePx = 0
    internal var displayedFontPx = 0; private set
    fun setFontFraction(value: Float) {
        val fraction = value.coerceIn(.7f, 1.35f)
        if (fontFraction == fraction) return
        fontFraction = fraction
        rebuild(false); invalidate()
    }
    var onCenterTap: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    internal var contentFits = false; private set
    internal var renderedText = ""; private set
    private var touchX = 0f
    private var touchY = 0f
    private var lastTouchY = 0f
    private var scrollOffset = 0f
    private var dragging = false
    private val slop = android.view.ViewConfiguration.get(context).scaledTouchSlop
    private fun dp(value: Float) = value * density
    private fun paint(size: Float, color: Int = android.graphics.Color.rgb(23, 32, 25)) = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
    }
    fun show(blocks: List<ReaderBlock>, page: Int, right: String, left: String, bottom: String) {
        if (this.blocks == blocks && this.page == page && topRight == right && topLeft == left && bottomRight == bottom) return
        this.blocks = blocks; this.page = page; topRight = right; topLeft = left; bottomRight = bottom
        renderedText = blocks.joinToString("\n") { listOfNotNull(it.basmala, it.text).joinToString("") }
        contentDescription = "$left، $right، $bottom، صفحة ${arabicNumber(page)}\n$renderedText"
        rebuild(); invalidate()
    }
    private fun layout(text: String, size: Int, centered: Boolean): android.text.StaticLayout {
        val p = paint(size.toFloat(), android.graphics.Color.BLACK).apply { typeface = mushafTypeface }
        val styled = android.text.SpannableString(text)
        Regex("﴿([٠-٩]+)﴾").findAll(text).forEach { match ->
            styled.setSpan(AyahMedallionSpan(match.groupValues[1]), match.range.first,
                match.range.last + 1, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return android.text.StaticLayout.Builder.obtain(styled, 0, styled.length, p, (width - dp(24f)).toInt().coerceAtLeast(1))
            .setAlignment(if (centered) android.text.Layout.Alignment.ALIGN_CENTER else android.text.Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(android.text.TextDirectionHeuristics.RTL)
            .setIncludePad(true).setLineSpacing(if (centered) 0f else extraLineSpace, if (centered) 1f else .88f)
            .apply {
                if (android.os.Build.VERSION.SDK_INT >= 26 && !centered)
                    setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD)
            }
            .setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE)
            .setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE).build()
    }
    private fun rebuild(remeasure: Boolean = true) {
        if (width == 0 || height == 0) return
        val available = (height - dp(94f)).coerceAtLeast(0f)
        extraLineSpace = 0f
        fun buildAt(candidate: Int) {
            layouts = blocks.map { block -> block to buildList {
                block.title?.let { add(layout(it, candidate, true)) }
                block.basmala?.let { add(layout(it, candidate, true)) }
                add(layout(block.text, candidate, false))
            } }
            val used = layouts.sumOf { (_, lines) -> lines.sumOf { it.height }.toDouble() + dp(8f) * lines.size }
            contentFits = used <= available
        }
        if (remeasure || fittedSizePx == 0) {
            var low = 1
            var high = dp(52f).toInt().coerceAtLeast(1)
            var best = 1
            while (low <= high) {
                val candidate = (low + high) / 2
                buildAt(candidate)
                if (contentFits) { best = candidate; low = candidate + 1 }
                else high = candidate - 1
            }
            fittedSizePx = best
        }
        displayedFontPx = (fittedSizePx * fontFraction).toInt().coerceAtLeast(1)
        buildAt(displayedFontPx)
        // Spread the small remaining height between verse lines, preserving glyph proportions.
        val gaps = layouts.sumOf { (_, lines) -> (lines.last().lineCount - 1).coerceAtLeast(0) }
        val used = layouts.sumOf { (_, lines) -> lines.sumOf { it.height }.toDouble() + dp(8f) * lines.size }
        if (gaps > 0 && used < available) {
            extraLineSpace = ((available - used) / gaps).toFloat().coerceAtMost(dp(10f))
            buildAt(displayedFontPx)
            while (!contentFits && extraLineSpace > 0f) {
                extraLineSpace = (extraLineSpace - 1f).coerceAtLeast(0f)
                buildAt(displayedFontPx)
            }
        }
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { rebuild() }
    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(android.graphics.Color.WHITE)
        val label = paint(dp(12f), android.graphics.Color.rgb(75, 87, 73))
        label.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText(topRight, width - dp(18f), dp(24f), label)
        canvas.drawText(bottomRight, width - dp(18f), height - dp(17f), label)
        label.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText(topLeft, dp(18f), dp(24f), label)
        label.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText(arabicNumber(page), width / 2f, height - dp(17f), label)
        canvas.drawText("‹", width / 2f - dp(48f), height - dp(16f), paint(dp(24f)))
        canvas.drawText("›", width / 2f + dp(40f), height - dp(16f), paint(dp(24f)))
        var y = dp(48f)
        val contentTop = dp(36f)
        val contentBottom = height - dp(46f)
        val contentHeight = layouts.sumOf { (_, lines) -> lines.sumOf { it.height }.toDouble() + dp(8f) * lines.size }.toFloat()
        val maxScroll = (contentHeight - (contentBottom - dp(48f))).coerceAtLeast(0f)
        scrollOffset = scrollOffset.coerceIn(0f, maxScroll)
        canvas.save()
        canvas.clipRect(0f, contentTop, width.toFloat(), contentBottom)
        y -= scrollOffset
        for ((block, lines) in layouts) {
            for ((index, line) in lines.withIndex()) {
                if (index == 0 && block.title != null) {
                    val frame = surahFrame

                    if (frame != null) {
                        val frameRect = android.graphics.RectF(
                            dp(20f),
                            y - dp(6f),
                            width - dp(20f),
                            y + line.height + dp(6f)
                        )

                        canvas.drawBitmap(
                            frame,
                            null,
                            frameRect,
                            framePaint
                        )
                    }
                }

                canvas.save(); canvas.translate(dp(12f), y); line.draw(canvas); canvas.restore()
                y += line.height + dp(8f)
            }
        }
        canvas.restore()
    }
    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> { touchX = event.x; touchY = event.y; lastTouchY = event.y; dragging = false; return true }
            android.view.MotionEvent.ACTION_MOVE -> {
                val delta = lastTouchY - event.y
                if (kotlin.math.abs(event.y - touchY) > slop) dragging = true
                if (dragging && event.y < height - dp(46f)) {
                    val contentHeight = layouts.sumOf { (_, lines) -> lines.sumOf { it.height }.toDouble() + dp(8f) * lines.size }.toFloat()
                    val maxScroll = (contentHeight - (height - dp(94f))).coerceAtLeast(0f)
                    val nextOffset = (scrollOffset + delta).coerceIn(0f, maxScroll)
                    if (nextOffset != scrollOffset) {
                        scrollOffset = nextOffset
                        invalidate()
                    }
                }
                lastTouchY = event.y
                return true
            }
            android.view.MotionEvent.ACTION_UP -> {
                if (!dragging && kotlin.math.abs(event.x - touchX) <= slop && kotlin.math.abs(event.y - touchY) <= slop) {
                    performClick()
                    if (event.y >= height - dp(46f)) {
                        if (event.x < width / 2f - dp(20f)) onNext?.invoke()
                        else if (event.x > width / 2f + dp(20f)) onPrevious?.invoke()
                    } else onCenterTap?.invoke()
                }
                return true
            }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}

@Composable
fun QuranReaderScreen(surahNumber: Int, onBack: () -> Unit, initialPage: Int? = null) {
    val context = LocalContext.current
    val verses = remember { readQuran(context) }
    val pages = remember(verses) { verses.groupBy { it.page } }
    val preferences = remember { context.getSharedPreferences("reading_progress", Context.MODE_PRIVATE) }
    val pager = rememberPagerState(initialPage = ((initialPage ?: verses.first { it.surah == surahNumber }.page).coerceIn(1, pages.size) - 1)) { pages.size }
    val scope = rememberCoroutineScope()
    var fontFraction by remember { mutableStateOf(preferences.getFloat("reader_font_fraction", 1f).coerceIn(.7f, 1.35f)) }
    fun changeFont(value: Float) { fontFraction = value.coerceIn(.7f, 1.35f) }
    fun saveFont() { preferences.edit().putFloat("reader_font_fraction", fontFraction).apply() }
    var controls by remember { mutableStateOf(false) }
    var tafsirPage by remember { mutableStateOf(0) }
    var saveHintVisible by remember { mutableStateOf(true) }
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { preferences.edit().putInt("last_page", it + 1).apply(); controls = false } }
    LaunchedEffect(controls) { if (controls) { kotlinx.coroutines.delay(4000); controls = false } }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        saveHintVisible = false
    }
    BackHandler { if (tafsirPage != 0) tafsirPage = 0 else if (controls) controls = false else onBack() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()) {
            Column(Modifier.fillMaxSize()) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(), key = { it }, overscrollEffect = null) { index ->
                val current = pages.getValue(index + 1)
                val blocks = remember(current) { readerBlocks(current) }
                androidx.compose.ui.viewinterop.AndroidView(factory = { MushafPageView(it) }, modifier = Modifier.fillMaxSize().testTag("mushaf-page-${index + 1}"), update = { view ->
                    view.show(blocks, index + 1,
                        "الجزء ${current.map { it.juz }.distinct().joinToString("–") { arabicNumber(it) }}",
                        current.map { surahs[it.surah - 1] }.distinct().joinToString(" • "),
                        "الحزب ${current.map { it.hizb }.distinct().joinToString("–") { arabicNumber(it) }}")
                    view.setFontFraction(fontFraction)
                    view.onCenterTap = { controls = !controls }
                    view.onPrevious = { if (index > 0) scope.launch { pager.animateScrollToPage(index - 1) } }
                    view.onNext = { if (index < pages.size - 1) scope.launch { pager.animateScrollToPage(index + 1) } }
                })
            }
            }
            if (controls) Surface(modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter), color = MushafCream, shadowElevation = 4.dp) {
                Row {
                    TextButton(onClick = onBack) { Text("رجوع") }
                    TextButton(onClick = { tafsirPage = pager.settledPage + 1; controls = false }) { Text("التفسير") }
                    TextButton(onClick = { changeFont(fontFraction - .05f); saveFont() }, enabled = fontFraction > .7f) { Text("أ−") }
                    TextButton(onClick = { changeFont(fontFraction + .05f); saveFont() }, enabled = fontFraction < 1.35f) { Text("أ+") }
                    TextButton(onClick = { controls = false }) { Text("إخفاء") }
                }
            }
            if (saveHintVisible) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 28.dp)
                        .testTag("auto-save-hint"),
                    shape = RoundedCornerShape(18.dp),
                    color = MushafCream,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MushafGold.copy(alpha = .45f))
                ) {
                    Text(
                        "تُحفظ الصفحة تلقائياً",
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                        color = MushafGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                }
            }
        }
        if (tafsirPage != 0) TafsirDialog(pages.getValue(tafsirPage), tafsirPage) { tafsirPage = 0 }
    }
}

@Composable
private fun TafsirDialog(verses: List<QuranVerse>, page: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val tafsir = remember {
        context.assets.open("tafsir-muyassar.txt").bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val p = line.split('|', limit = 3)
                val s = p.getOrNull(0)?.toIntOrNull()
                val a = p.getOrNull(1)?.toIntOrNull()
                if (s != null && a != null && p.size == 3) (s to a) to p[2] else null
            }.toMap()
        }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp),
            shape = RoundedCornerShape(16.dp), color = pagePaper) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("التفسير الميسّر", color = pageInk, fontSize = 23.sp)
                    TextButton(onClick = onClose) { Text("إغلاق") }
                }
                Text("صفحة ${arabicNumber(page)}", color = pageInk, fontSize = 14.sp)
                LazyColumn(Modifier.weight(1f).padding(top = 12.dp)) {
                    items(verses, key = { "${it.surah}:${it.ayah}" }) { verse ->
                        Text("${surahs[verse.surah - 1]} — الآية ${arabicNumber(verse.ayah)}",
                            fontSize = 16.sp, color = pageInk)
                        Text(decoratedVerses(verseText(verse)), fontFamily = quranFont, fontSize = 20.sp, lineHeight = 36.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = ayahInk)
                        Text(tafsir.getValue(verse.surah to verse.ayah), fontSize = 19.sp, lineHeight = 31.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                        HorizontalDivider(Modifier.padding(vertical = 16.dp), color = pageGold)
                    }
                }
                Text("التفسير الميسّر: مجمع الملك فهد — نسخة تنزيل (tanzil.net/trans)\nتقسيم صفحات مصحف المدينة: تنزيل (CC BY)",
                    fontSize = 11.sp, color = pageInk, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
