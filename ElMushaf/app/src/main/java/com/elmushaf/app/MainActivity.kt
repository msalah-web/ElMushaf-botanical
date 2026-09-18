package com.elmushaf.app

import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(shapes = Shapes(
                small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp)
            ), colorScheme = lightColorScheme(
                primary = MushafGreen, onPrimary = MushafCream,
                background = MushafCream, surface = MushafCream,
                onBackground = Color(0xFF243D34), onSurface = Color(0xFF243D34),
                secondary = Color(0xFF667B65), surfaceVariant = Color(0xFFEDF0E6),
                outline = MushafSage, primaryContainer = MushafGreen, onPrimaryContainer = MushafCream
            )) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize()) { OrnateAppFrame { ElMushafApp() } }
                }
            }
        }
    }
}

@Composable
fun ElMushafApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("reading_progress", Context.MODE_PRIVATE) }
    var showDedication by rememberSaveable { mutableStateOf(false) }
    var showSurahs by rememberSaveable { mutableStateOf(false) }
    var showRuqyah by rememberSaveable { mutableStateOf(false) }
    var showAdhkar by rememberSaveable { mutableStateOf(false) }
    var showDuas by rememberSaveable { mutableStateOf(false) }
    var showSources by rememberSaveable { mutableStateOf(false) }
    var showQibla by rememberSaveable { mutableStateOf(false) }
    var showAudio by rememberSaveable { mutableStateOf(false) }
    var showShare by rememberSaveable { mutableStateOf(false) }
    var resumePage by rememberSaveable { mutableStateOf(0) }
    val lastPage = preferences.getInt("last_page", 0)
    if (resumePage in 1..604) {
        QuranReaderScreen(surahNumber = 1, initialPage = resumePage, onBack = { resumePage = 0 })
        return
    }
    if (showDedication) {
        BackHandler { showDedication = false }
        DedicationScreen(
            onStart = { showDedication = false; resumePage = 1 },
            onContinue = { showDedication = false; resumePage = lastPage.takeIf { it in 1..604 } ?: 1 },
            onSurahs = { showDedication = false; showSurahs = true },
            onBack = { showDedication = false }
        )
        return
    }
    if (showSurahs) {
        SurahListScreen(onBack = { showSurahs = false })
        return
    }
    if (showRuqyah) {
        BackHandler { showRuqyah = false }
        RuqyahScreen { showRuqyah = false }
        return
    }
    if (showAdhkar) {
        AdhkarScreen(onBack = { showAdhkar = false })
        return
    }
    if (showDuas) {
        DuasScreen(onBack = { showDuas = false })
        return
    }
    if (showSources) {
        BackHandler { showSources = false }
        SourcesScreen { showSources = false }
        return
    }
    if (showAudio) {
        QuranAudioScreen { showAudio = false }
        return
    }
    if (showShare) {
        BackHandler { showShare = false }
        ShareAppScreen { showShare = false }
        return
    }
    if (showQibla) {
        BackHandler { showQibla = false }
        QiblaCompassScreen { showQibla = false }
        return
    }
    MushafHome(
        lastPage = lastPage,
        onContinue = { if (lastPage in 1..604) resumePage = lastPage else showDedication = true },
        onQuran = { showDedication = true }, onAdhkar = { showAdhkar = true },
        onDuas = { showDuas = true }, onRuqyah = { showRuqyah = true },
        onQibla = { showQibla = true }, onSources = { showSources = true },
        onShare = { showShare = true }, onAudio = { showAudio = true }
    )
}

@Composable
fun QiblaScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("اتجاه القبلة", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(36.dp))
        Text("🕋", fontSize = 86.sp)
        Spacer(Modifier.height(22.dp))
        Text("وجّه الهاتف نحو الكعبة المشرفة", fontSize = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Text("سيتم تحديد الاتجاه باستخدام موقع الهاتف وحساس البوصلة.", fontSize = 16.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = onBack) { Text("العودة للرئيسية") }
    }
}

@Composable
fun SourcesScreen(onBack: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("رجوع") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(2.dp))
            Text("مصادر وحقوق المصحف", fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(22.dp))
            OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFFAA884A))) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(4.dp))
                    Text("نص القرآن الكريم المستخدم في هذا التطبيق\nمن مشروع Tanzil Quran Text\nنسخة عثمانية Uthmani — الإصدار 1.1", fontSize = 18.sp, lineHeight = 30.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    Text("حقوق النشر © 2007–2026 Tanzil Project\nالرخصة: Creative Commons Attribution 3.0", fontSize = 16.sp, lineHeight = 28.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)

                }
            }
            Spacer(Modifier.height(20.dp))
            Text("التلاوات: Al Quran Cloud / Islamic Network\nحقوق التسجيلات محفوظة للقرّاء وأصحابها. الاستخدام في التطبيق مجاني وتعليمي غير تجاري.", textAlign = TextAlign.Center)
            TextButton(onClick = { uriHandler.openUri("https://alquran.cloud/terms-and-conditions") }) { Text("شروط وحقوق التلاوات") }
            TextButton(onClick = onBack) { Text("العودة للرئيسية") }
        }
        TextButton(
            onClick = { uriHandler.openUri("https://tanzil.net/") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("Tanzil — tanzil.net", fontSize = 18.sp,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
        }
    }
}

@Composable
fun DedicationScreen(
    onStart: () -> Unit,
    onContinue: () -> Unit,
    onSurahs: () -> Unit,
    onBack: () -> Unit
) {
    BotanicalDedication(onStart, onContinue, onSurahs, onBack)
}

@Composable
fun MushafButton(title: String, onClick: () -> Unit = {}) {

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MushafGold),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp
        )
    }
}
fun loadSurahFromAssets(context: Context, surahNumber: Int): String {
    val lines = context.assets.open("quran-uthmani.txt")
        .bufferedReader()
        .readLines()
    val builder = StringBuilder()

    fun toArabicDigits(n: String): String {
        val map = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return n.map { ch ->
            if (ch in '0'..'9') map[ch - '0'] else ch
        }.joinToString("")
    }

    fun ayahMarker(num: String): String {
        val n = toArabicDigits(num)
        return "﴿$n﴾ "
    }

    fun isBasmala(text: String): Boolean {
        val lettersOnly = text.filter { it.isLetter() }
        return lettersOnly.contains("بسم الله") && lettersOnly.contains("الرحيم")
    }

    for (line in lines) {
        if (line.startsWith("#") || line.isBlank()) continue

        val parts = line.split("|")
        if (parts.size < 3) continue
        if (parts[0].toIntOrNull() != surahNumber) continue

        val ayahNumber = parts[1]
        var ayahText = parts[2].trim()

        if (surahNumber != 1 && surahNumber != 9 && ayahNumber == "1") {
            val idx = ayahText.indexOf("حيم")
            if (idx >= 0 && idx < 80) {
                val cut = idx + 3
                val basmala = ayahText.substring(0, cut).trim()
                ayahText = ayahText.substring(cut).trim()
                //builder.append(basmala)
                //builder.append("\n\n")
            }
        }

        if (ayahText.isNotEmpty()) {
            builder.append(ayahText)
            builder.append(ayahMarker(ayahNumber))
        }
    }

    return builder.toString().trim()
}
val surahs = listOf(
    "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف",
    "الأنفال", "التوبة", "يونس", "هود", "يوسف", "الرعد", "إبراهيم", "الحجر",
    "النحل", "الإسراء", "الكهف", "مريم", "طه", "الأنبياء", "الحج", "المؤمنون",
    "النور", "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم",
    "لقمان", "السجدة", "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص",
    "الزمر", "غافر", "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية",
    "الأحقاف", "محمد", "الفتح", "الحجرات", "ق", "الذاريات", "الطور",
    "النجم", "القمر", "الرحمن", "الواقعة", "الحديد", "المجادلة", "الحشر",
    "الممتحنة", "الصف", "الجمعة", "المنافقون", "التغابن", "الطلاق",
    "التحريم", "الملك", "القلم", "الحاقة", "المعارج", "نوح", "الجن",
    "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ",
    "النازعات", "عبس", "التكوير", "الانفطار", "المطففين", "الانشقاق",
    "البروج", "الطارق", "الأعلى", "الغاشية", "الفجر", "البلد", "الشمس",
    "الليل", "الضحى", "الشرح", "التين", "العلق", "القدر", "البينة",
    "الزلزلة", "العاديات", "القارعة", "التكاثر", "العصر", "الهمزة",
    "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر", "المسد",
    "الإخلاص", "الفلق", "الناس"
)

@Composable
fun SurahListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedSurah by rememberSaveable { mutableStateOf(0) }
    if (selectedSurah != 0) {
        QuranReaderScreen(
            surahNumber = selectedSurah,
            onBack = { selectedSurah = 0 }
        )
        return
    }
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(all = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onBack) { Text("الرئيسية") }
        Text(
            text = "القرآن الكريم",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))



        surahs.forEachIndexed { index, name ->
            MushafButton(title = "${arabicNumber(index + 1)} - $name") {
                selectedSurah = index + 1
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
@Composable
fun RuqyahScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(all = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) { Text("رجوع") }
        }
        Text(
            text = "الرقية الشرعية",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "بسم الله أرقيك، من كل شيء يؤذيك، من شر كل نفس أو عين حاسد، الله يشفيك، بسم الله أرقيك",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "المصدر: صحيح مسلم", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "أذهب الباس، رب الناس، اشف أنت الشافي، لا شفاء إلا شفاؤك، شفاء لا يغادر سقما",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "المصدر: صحيح البخاري وصحيح مسلم", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "أعوذ بكلمات الله التامات من شر ما خلق",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "المصدر: صحيح مسلم", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "بسم الله، تربة أرضنا، بريقة بعضنا، يشفى سقيمنا، بإذن ربنا",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "المصدر: صحيح البخاري وصحيح مسلم", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "المصدر: سنن أبي داود والترمذي", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "أعوذ بكلمات الله التامة من كل شيطان وهامة ومن كل عين لامة",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "المصدر: صحيح البخاري", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "الله لا إله إلا هو الحي القيوم لا تأخذه سنة ولا نوم له ما في السماوات وما في الأرض من ذا الذي يشفع عنده إلا بإذنه يعلم ما بين أيديهم وما خلفهم ولا يحيطون بشيء من علمه إلا بما شاء وسع كرسيه السماوات والأرض ولا يئوده حفظهما وهو العلي العظيم",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "المصدر: البقرة ٢٥٥ — آية الكرسي", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "قل هو الله أحد، الله الصمد، لم يلد ولم يولد، ولم يكن له كفوا أحد",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "المصدر: سورة الإخلاص", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "قل أعوذ برب الفلق، من شر ما خلق، ومن شر غاسق إذا وقب، ومن شر النفاثات في العقد، ومن شر حاسد إذا حسد",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "المصدر: سورة الفلق", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "قل أعوذ برب الناس، ملك الناس، إله الناس، من شر الوسواس الخناس، الذي يوسوس في صدور الناس، من الجنة والناس",
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Right
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "المصدر: سورة الناس", fontSize = 12.sp)

        Spacer(modifier = Modifier.height(40.dp))
    }
}
