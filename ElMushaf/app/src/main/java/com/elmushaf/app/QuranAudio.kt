package com.elmushaf.app

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal data class AudioReciter(val name: String, val edition: String, val bitrate: Int = 128)
internal val audioReciters = listOf(
    AudioReciter("مشاري العفاسي", "ar.alafasy"), AudioReciter("محمود خليل الحصري", "ar.husary"),
    AudioReciter("عبدالباسط عبدالصمد", "ar.abdulsamad", 64), AudioReciter("ماهر المعيقلي", "ar.mahermuaiqly"),
    AudioReciter("عبدالرحمن السديس", "ar.abdurrahmaansudais", 192), AudioReciter("سعود الشريم", "ar.saoodshuraym", 64),
    AudioReciter("علي الحذيفي", "ar.hudhaify"), AudioReciter("أحمد العجمي", "ar.ahmedajamy"),
    AudioReciter("أبو بكر الشاطري", "ar.shaatree"), AudioReciter("محمد أيوب", "ar.muhammadayyoub"),
    AudioReciter("عبدالله بصفر", "ar.abdullahbasfar", 192),
    AudioReciter("هاني الرفاعي", "ar.hanirifai", 192),
    AudioReciter("إبراهيم الأخضر", "ar.ibrahimakhbar", 32),
    AudioReciter("محمد جبريل", "ar.muhammadjibreel"),
    AudioReciter("شهريار برهيزكار", "ar.parhizgar", 48),
    AudioReciter("أيمن سويد", "ar.aymanswoaid", 64),
    AudioReciter("محمود خليل الحصري — المجوّد", "ar.husarymujawwad")
)
internal fun audioUrl(reciter: AudioReciter, verse: Int): String {
    require(reciter in audioReciters && verse in 1..6236)
    return "https://cdn.islamic.network/quran/audio/${reciter.bitrate}/${reciter.edition}/$verse.mp3"
}
internal fun audioFile(context: Context, reciter: AudioReciter, verse: Int) =
    File(context.filesDir, "recitations/${reciter.edition}/$verse.mp3")

// Commit only complete responses. An interrupted download can never become a playable cache entry.
internal suspend fun downloadAudio(context: Context, reciter: AudioReciter, verse: Int) = withContext(Dispatchers.IO) {
    val target = audioFile(context, reciter, verse)
    if (target.isFile && target.length() > 0) return@withContext
    target.parentFile!!.mkdirs()
    val temporary = File(target.path + ".part")
    val connection = URL(audioUrl(reciter, verse)).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15000; connection.readTimeout = 15000
        require(connection.responseCode == 200) { "Audio unavailable" }
        require(connection.contentType?.substringBefore(';') in listOf("audio/mpeg", "audio/mp3", "application/octet-stream"))
        val expected = connection.contentLengthLong
        var total = 0L
        connection.inputStream.use { input -> temporary.outputStream().use { output ->
            val buffer = ByteArray(16384)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= 30_000_000L)
                output.write(buffer, 0, count)
            }
        } }
        currentCoroutineContext().ensureActive()
        require(total > 0 && (expected < 0 || total == expected))
        check(temporary.renameTo(target))
    } finally { connection.disconnect(); temporary.delete() }
}

internal class QuranAudioPlayer(private val context: Context) {
    var playing by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var wantsPlayback by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var index by mutableIntStateOf(0); private set
    var currentSurah by mutableIntStateOf(1); private set
    private var playlistIndex = 0
    private val quran by lazy { readQuran(context) }
    internal var sourceIsLocal = false; private set
    internal var preparedSessions = 0; private set
    private var verseIds = emptyList<Int>()
    private var reciter = audioReciters.first()
    internal val selectedReciterIndex get() = audioReciters.indexOf(reciter)
    internal val firstVerseId get() = verseIds.firstOrNull()
    private var prepared = false
    private val queueSize = 12
    internal val player = ExoPlayer.Builder(context).setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK).build().apply {
        setAudioAttributes(androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH).build(), true)
        setHandleAudioBecomingNoisy(true)
        volume = 1f
        addListener(object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { wantsPlayback = playWhenReady }
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) {
                loading = state == Player.STATE_BUFFERING
            }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                if (item != null) {
                    playlistIndex = item.mediaId.toInt() - verseIds.first()
                    val verse = quran[verseIds[playlistIndex] - 1]
                    currentSurah = verse.surah
                    index = verse.ayah - 1
                    sourceIsLocal = item.localConfiguration?.uri?.scheme == "file"
                    // Retain one previous ayah and a small look-ahead instead of thousands of sources.
                    val removeCount = (currentMediaItemIndex - 1).coerceAtLeast(0)
                    if (removeCount > 0) removeMediaItems(0, removeCount)
                    val next = getMediaItemAt(mediaItemCount - 1).mediaId.toInt() - verseIds.first() + 1
                    val end = (next + queueSize - mediaItemCount).coerceAtMost(verseIds.size)
                    if (next < end) addMediaItems((next until end).map { mediaItem(verseIds[it]) })
                }
            }
            override fun onPlayerError(failure: PlaybackException) {
                error = "تعذّر تحميل التلاوة؛ تحقق من الإنترنت أو نزّل السورة مسبقًا"
                playing = false; loading = false; prepared = false
            }
        })
    }
    fun select(value: AudioReciter, verses: List<Int>) {
        if (reciter == value && firstVerseId == verses.firstOrNull()) return
        stop(); reciter = value
        verseIds = verses.firstOrNull()?.let { first -> (first..quran.size).toList() } ?: emptyList()
        playlistIndex = 0; index = 0; currentSurah = verses.firstOrNull()?.let { quran[it - 1].surah } ?: 1; error = null
    }
    fun stop() { player.pause(); player.stop(); player.clearMediaItems(); prepared = false; playing = false; loading = false }
    fun close() { player.release(); playing = false; loading = false }
    fun pause() { player.pause(); playing = false }
    internal fun displayedVerseId(): Int? {
        val current = player.currentMediaItem?.mediaId?.toIntOrNull() ?: return null
        val duration = player.duration
        if (player.isPlaying && duration > 0 && player.currentPosition >= duration - 1_000L &&
            player.currentMediaItemIndex + 1 < player.mediaItemCount) {
            return player.getMediaItemAt(player.currentMediaItemIndex + 1).mediaId.toIntOrNull() ?: current
        }
        return current
    }
    private fun mediaItem(id: Int): MediaItem {
        val verse = quran[id - 1]
        val saved = audioFile(context, reciter, id)
        val uri = if (saved.isFile && saved.length() > 0) android.net.Uri.fromFile(saved)
            else android.net.Uri.parse(audioUrl(reciter, id))
        return MediaItem.Builder().setMediaId(id.toString()).setUri(uri)
            .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder()
                .setTitle("سورة ${surahs[verse.surah - 1]} — ${reciter.name}")
                .setArtist("الآية ${arabicNumber(verse.ayah)}").build()).build()
    }
    fun play() {
        if (verseIds.isEmpty()) return
        error = null
        if (this === shared) androidx.core.content.ContextCompat.startForegroundService(context,
            android.content.Intent(context, QuranPlaybackService::class.java))
        if (player.playbackState == Player.STATE_ENDED) { playlistIndex = 0; prepared = false }
        if (!prepared) {
            // One playlist and decoder session. The next ayah buffers before the current one ends.
            
            val end = (playlistIndex + queueSize).coerceAtMost(verseIds.size)
            val items = (playlistIndex until end).map { mediaItem(verseIds[it]) }
            player.setMediaItems(items, 0, 0L)
            player.prepare(); prepared = true; preparedSessions++
        }
        player.play()
    }
    companion object {
        private var shared: QuranAudioPlayer? = null
        fun shared(context: Context): QuranAudioPlayer = shared ?: QuranAudioPlayer(context.applicationContext).also { shared = it }
        fun releaseShared() { shared?.close(); shared = null }
    }
    fun move(delta: Int) {
        if (verseIds.isEmpty()) return
        playlistIndex = (playlistIndex + delta).coerceIn(0, verseIds.lastIndex)
        val verse = quran[verseIds[playlistIndex] - 1]
        currentSurah = verse.surah; index = verse.ayah - 1
        if (prepared) {
            val queueIndex = (0 until player.mediaItemCount).firstOrNull {
                player.getMediaItemAt(it).mediaId == verseIds[playlistIndex].toString()
            }
            if (queueIndex != null) player.seekTo(queueIndex, 0L) else prepared = false
        }
        play()
    }
}

@Composable
fun QuranAudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uri = LocalUriHandler.current
    
    val scope = rememberCoroutineScope()
    val player = remember { QuranAudioPlayer.shared(context) }
    val verses = remember { readQuran(context).withIndex().groupBy { it.value.surah } }
    var reciterIndex by rememberSaveable { mutableIntStateOf(player.selectedReciterIndex) }
    var surah by rememberSaveable { mutableIntStateOf(verses.entries.firstOrNull { entry -> entry.value.first().index + 1 == player.firstVerseId }?.key ?: 1) }
    var chooseReciter by remember { mutableStateOf(false) }
    var chooseSurah by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var download by remember { mutableStateOf<Job?>(null) }
    var downloaded by remember { mutableIntStateOf(0) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var version by remember { mutableIntStateOf(0) }
    val reciter = audioReciters[reciterIndex]
    val quranFont = remember { FontFamily(Font(R.font.amiri_quran)) }
    val visibleSurah = player.currentSurah
    val ids = remember(visibleSurah) { verses.getValue(visibleSurah).map { it.index + 1 } }
    var displayedId by remember { mutableIntStateOf(ids[player.index.coerceIn(0, ids.lastIndex)]) }
    LaunchedEffect(visibleSurah, player.index, player.playing) {
        displayedId = ids[player.index.coerceIn(0, ids.lastIndex)]
        while (player.playing) {
            displayedId = player.displayedVerseId() ?: displayedId
            delay(100)
        }
    }
    val saved = remember(reciterIndex, visibleSurah, version) { ids.all { audioFile(context, reciter, it).let { f -> f.isFile && f.length() > 0 } } }
    LaunchedEffect(reciterIndex, surah) { player.select(reciter, verses.getValue(surah).map { it.index + 1 }) }
    LaunchedEffect(visibleSurah, player.index) { listState.scrollToItem(0) }
    val startDownload: () -> Unit = {
        player.stop(); downloaded = 0; downloadError = null
        download = scope.launch {
            try {
                ids.forEachIndexed { i, id -> downloadAudio(context, reciter, id); downloaded = i + 1 }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { downloadError = "تعذّر إكمال التنزيل. حاول مجددًا؛ الملفات المكتملة محفوظة" }
            finally { version++; download = null }
        }
    }
    BackHandler { download?.cancel(); onBack() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MushafCream).safeDrawingPadding().padding(horizontal = 20.dp)) {
    val versePanelHeight = maxHeight / 3
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { download?.cancel(); onBack() }) { Text("الرئيسية") }
            TextButton(onClick = { options = true }) { Text("⋯", fontSize = 26.sp) }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { chooseReciter = true }, enabled = download == null,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MushafSage),
                colors = ButtonDefaults.buttonColors(containerColor = MushafGreen, contentColor = MushafCream)) {
                Text(reciter.name, fontSize = 18.sp, textAlign = TextAlign.Center)
            }
            Button(onClick = { chooseSurah = true }, enabled = download == null,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MushafSage),
                colors = ButtonDefaults.buttonColors(containerColor = MushafGreen, contentColor = MushafCream)) {
                Text("سورة ${surahs[visibleSurah - 1]}", fontSize = 18.sp, textAlign = TextAlign.Center)
            }
        }
        Text(if (saved) "هذه السورة متاحة بدون إنترنت" else "استمع عبر الإنترنت أو احفظ السورة للاستماع لاحقًا",
            fontSize = 12.sp, color = MushafGreen.copy(alpha = .7f), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
        Spacer(Modifier.weight(1f))
        Surface(Modifier.fillMaxWidth().height(versePanelHeight).testTag("audio-verse-panel"), shape = RoundedCornerShape(16.dp), color = MushafCream,
            border = BorderStroke(1.dp, MushafGold.copy(alpha = .3f))) {
            LazyColumn(state = listState, overscrollEffect = null,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp)) {
                item {
                    val verse = verses.values.asSequence().flatten().first { it.index + 1 == displayedId }.value
                    Text("${verse.text}  ﴿${arabicNumber(verse.ayah)}﴾", modifier = Modifier.fillMaxWidth().testTag("current_ayah"),
                        textAlign = TextAlign.Center, fontFamily = quranFont, fontSize = 20.sp, lineHeight = 32.sp,
                        style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl,
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)),
                        color = MushafGreen)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        player.error?.let { Text(it, textAlign = TextAlign.Center, color = MushafGreen, modifier = Modifier.padding(8.dp)) }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("audio-controls"),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AudioIconButton("السابق", "previous", enabled = player.index > 0 || visibleSurah > surah) { player.move(-1) }
                Text("السابق", fontSize = 11.sp, color = MushafGreen, modifier = Modifier.padding(top = 4.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    AudioIconButton(if (player.wantsPlayback) "إيقاف مؤقت" else "تشغيل", if (player.wantsPlayback) "pause" else "play") {
                        if (player.wantsPlayback) player.pause() else player.play()
                    }
                    if (player.loading) CircularProgressIndicator(Modifier.size(64.dp), color = MushafGold, strokeWidth = 2.dp)
                }
                Text(if (player.wantsPlayback) "إيقاف مؤقت" else "تشغيل", fontSize = 11.sp,
                    color = MushafGreen, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AudioIconButton("التالي", "next", enabled = visibleSurah < 114 || player.index < ids.lastIndex) { player.move(1) }
                Text("التالي", fontSize = 11.sp, color = MushafGreen, modifier = Modifier.padding(top = 4.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AudioIconButton(if (saved) "السورة محفوظة" else "تنزيل السورة", "download",
                    enabled = !saved && download == null) { options = true; startDownload() }
                Text(if (saved) "محفوظة" else "تنزيل", fontSize = 11.sp, color = MushafGreen,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
        Text("التلاوة: ${reciter.name}\nالمصدر: Al Quran Cloud / Islamic Network",
            textAlign = TextAlign.Center, fontSize = 10.sp, lineHeight = 14.sp,
            color = MushafGreen.copy(alpha = .65f), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
    }
    }
    }
    if (options) AlertDialog(onDismissRequest = { options = false }, title = { Text("خيارات التلاوة") }, text = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (saved && download == null) Text("السورة محفوظة وجاهزة بدون إنترنت", textAlign = TextAlign.Center)
        if (download != null) {
            Text("التنزيل: ${arabicNumber(downloaded)} من ${arabicNumber(ids.size)}")
            LinearProgressIndicator(progress = { downloaded.toFloat() / ids.size }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { download?.cancel() }) { Text("إلغاء التنزيل") }
        } else if (!saved) {
            OutlinedButton(onClick = startDownload) { Text("تنزيل السورة للاستماع بدون إنترنت") }
        }
        downloadError?.let { Text(it, textAlign = TextAlign.Center) }
        TextButton(onClick = { uri.openUri("https://alquran.cloud/terms-and-conditions") }) { Text("مصدر التلاوات وحقوق الاستخدام") }
        }
    }, confirmButton = { TextButton(onClick = { options = false }) { Text("إغلاق") } })
    if (chooseReciter || chooseSurah) AlertDialog(
        onDismissRequest = { chooseReciter = false; chooseSurah = false },
        containerColor = MushafCream, titleContentColor = MushafGreen, textContentColor = MushafGreen,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, MushafGold, RoundedCornerShape(24.dp)),
        title = { Text(if (chooseReciter) "اختر القارئ" else "اختر السورة", fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (chooseReciter) itemsIndexed(audioReciters) { i, value ->
                    Button(onClick = { player.stop(); surah = visibleSurah; reciterIndex = i; chooseReciter = false },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MushafSage),
                        colors = ButtonDefaults.buttonColors(containerColor = MushafGreen, contentColor = MushafCream)) {
                        Text(value.name, fontSize = 20.sp, textAlign = TextAlign.Center)
                    }
                }
                else itemsIndexed(surahs) { i, value ->
                    Button(onClick = { player.stop(); surah = i + 1; chooseSurah = false },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MushafSage),
                        colors = ButtonDefaults.buttonColors(containerColor = MushafGreen, contentColor = MushafCream)) {
                        Text("${arabicNumber(i + 1)} — $value", fontSize = 20.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { chooseReciter = false; chooseSurah = false },
                colors = ButtonDefaults.textButtonColors(contentColor = MushafGreen)) { Text("إغلاق", fontSize = 18.sp) }
        })

}


@Composable
private fun AudioIconButton(label: String, kind: String, enabled: Boolean = true, onClick: () -> Unit) {
    FilledIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(64.dp).border(1.dp, MushafGold, CircleShape),
        shape = CircleShape, colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MushafGreen, contentColor = MushafCream)) {
        val tint = LocalContentColor.current
        Canvas(Modifier.size(30.dp).then(Modifier.semantics { contentDescription = label })) {
            val w = size.width; val h = size.height
            if (kind == "pause") {
                drawRect(tint, androidx.compose.ui.geometry.Offset(w * .2f, h * .12f), androidx.compose.ui.geometry.Size(w * .2f, h * .76f))
                drawRect(tint, androidx.compose.ui.geometry.Offset(w * .6f, h * .12f), androidx.compose.ui.geometry.Size(w * .2f, h * .76f))
            } else if (kind == "download") {
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .5f, h * .1f), androidx.compose.ui.geometry.Offset(w * .5f, h * .65f), 2.dp.toPx())
                val arrow = Path().apply {
                    moveTo(w * .25f, h * .45f); lineTo(w * .5f, h * .7f); lineTo(w * .75f, h * .45f)
                }
                drawPath(arrow, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                drawLine(tint, androidx.compose.ui.geometry.Offset(w * .15f, h * .88f), androidx.compose.ui.geometry.Offset(w * .85f, h * .88f), 2.dp.toPx())
            } else {
                val backwards = kind == "previous"
                val path = Path().apply {
                    moveTo(if (backwards) w * .8f else w * .2f, h * .12f)
                    lineTo(if (backwards) w * .2f else w * .8f, h * .5f)
                    lineTo(if (backwards) w * .8f else w * .2f, h * .88f); close()
                }
                drawPath(path, tint)
                if (kind != "play") drawLine(tint,
                    androidx.compose.ui.geometry.Offset(if (backwards) w * .1f else w * .9f, h * .12f),
                    androidx.compose.ui.geometry.Offset(if (backwards) w * .1f else w * .9f, h * .88f), 3.dp.toPx())
            }
        }
    }
}
