package com.elmushaf.app

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

internal enum class UmrahMode(val key: String, val title: String) {
    TAWAF("tawaf", "الطواف"), SAI("sai", "السعي")
}
internal fun completedUmrahLap(count: Int, lap: Int): Int =
    if (count in 0..6 && lap == count) count + 1 else count.coerceIn(0, 7)
internal fun undoUmrahLap(count: Int): Int = (count - 1).coerceIn(0, 7)
internal fun saiRoute(lap: Int): String = if (lap % 2 == 0) "الصفا إلى المروة" else "المروة إلى الصفا"
private val lapNames = listOf("الأول", "الثاني", "الثالث", "الرابع", "الخامس", "السادس", "السابع")
internal data class UmrahPage(val title: String, val prayers: List<String>, val note: String = "")
private const val mountainDhikr = "الله أكبر، الله أكبر، الله أكبر.\nلا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير.\nلا إله إلا الله وحده، أنجز وعده، ونصر عبده، وهزم الأحزاب وحده."
internal fun umrahPages(mode: UmrahMode, lap: Int, prayers: List<String>): List<UmrahPage> {
    require(lap in 0..6 && prayers.size == 15)
    val result = mutableListOf<UmrahPage>()
    if (mode == UmrahMode.TAWAF) {
        result += UmrahPage("عند محاذاة الحجر الأسود", listOf("الله أكبر"),
            "عند بداية الشوط ومحاذاة الحجر الأسود كبّر، وأشر إليه إن لم تتمكن من استلامه، دون مزاحمة أو إيذاء.")
    } else if (lap == 0) {
        result += UmrahPage("عند الاقتراب من الصفا — أول مرة", listOf(
            "﴿إِنَّ الصَّفَا وَالْمَرْوَةَ مِنْ شَعَائِرِ اللَّهِ﴾", "أبدأ بما بدأ الله به"),
            "تبدأ السعي من الصفا. هذا الذكر عند بداية السعي أول مرة.")
        result += UmrahPage("على الصفا قبل بدء السعي", listOf(mountainDhikr),
            "استقبل القبلة، وكبّر وقل هذا الذكر، ثم ادعُ بما شئت. كرّر الذكر ثلاث مرات، مع الدعاء بينه.")
    }
    prayers.chunked(15).forEach { values ->
        result += UmrahPage("أدعية أثناء ${if (mode == UmrahMode.TAWAF) "الطواف" else "السعي"}", values,
            "١٥ دعاءً مقترحًا، وليست أدعية مخصوصة لهذا الشوط")
    }
    if (mode == UmrahMode.TAWAF) {
        result += UmrahPage("بين الركن اليماني والحجر الأسود", listOf(
            "ربنا آتنا في الدنيا حسنة وفي الآخرة حسنة وقنا عذاب النار"),
            "يُستحب هذا الدعاء بين الركن اليماني والحجر الأسود. لا يرتبط عدّ الشوط بإتمام الأدعية.")
    } else {
        val destination = if (lap % 2 == 0) "المروة" else "الصفا"
        result += UmrahPage("عند الوصول إلى $destination", listOf(mountainDhikr),
            "استقبل القبلة، وكرّر الذكر ثلاث مرات، مع الدعاء بينه. ثم علّم الشوط مكتملًا بعد وصولك.")
    }
    return result
}

@Composable
fun UmrahScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("umrah_progress", Context.MODE_PRIVATE) }
    val allPrayers = remember { JSONObject(context.assets.open("umrah-prayers.json").bufferedReader().use { it.readText() }) }
    var modeName by rememberSaveable { mutableStateOf(prefs.getString("mode", UmrahMode.TAWAF.name) ?: UmrahMode.TAWAF.name) }
    val mode = UmrahMode.entries.firstOrNull { it.name == modeName } ?: UmrahMode.TAWAF
    var tawafCount by remember { mutableIntStateOf(prefs.getInt("tawaf_completed", 0).coerceIn(0, 7)) }
    var saiCount by remember { mutableIntStateOf(prefs.getInt("sai_completed", 0).coerceIn(0, 7)) }
    val completed = if (mode == UmrahMode.TAWAF) tawafCount else saiCount
    var lap by rememberSaveable { mutableIntStateOf(-1) }
    var confirm by remember { mutableStateOf<String?>(null) }
    fun setCount(value: Int) {
        if (mode == UmrahMode.TAWAF) tawafCount = value else saiCount = value
        prefs.edit().putInt("${mode.key}_completed", value).apply()
    }
    fun changeMode(value: UmrahMode) {
        modeName = value.name
        prefs.edit().putString("mode", value.name).apply()
    }
    BackHandler { if (lap >= 0) lap = -1 else onBack() }
    if (lap >= 0) {
        val selectedLap = lap.coerceIn(0, 6)
        val values = allPrayers.getJSONArray(mode.key).getJSONArray(selectedLap)
        val prayers = List(values.length()) { values.getString(it) }
        val pages = remember(mode, selectedLap) { umrahPages(mode, selectedLap, prayers) }
        val scroll = key(mode, selectedLap) { rememberScrollState() }
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = { lap = -1 }) { Text("العودة للأشواط") }
            Text("${mode.title} — الشوط ${lapNames[selectedLap]}", fontSize = 24.sp,
                fontWeight = FontWeight.Bold, color = MushafGreen, textAlign = TextAlign.Center)
            if (mode == UmrahMode.SAI) Text(saiRoute(selectedLap), fontSize = 15.sp, color = MushafGreen)
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                pages.forEach { data ->
                    Text(data.title, fontSize = 17.sp, color = MushafGreen,
                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp), textAlign = TextAlign.Center)
                    data.prayers.forEach { text ->
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                            color = MushafCream.copy(alpha = .96f), border = BorderStroke(1.dp, MushafSage)) {
                            Text(text, fontSize = 21.sp, lineHeight = 34.sp, color = MushafGreen,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                        }
                    }
                    if (data.note.isNotBlank()) Text(data.note, fontSize = 15.sp, lineHeight = 24.sp,
                        color = MushafGreen, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(8.dp))
                }
            Button(enabled = selectedLap == completed && completed < 7,
                onClick = { confirm = "complete" }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("umrah-complete"),
                shape = RoundedCornerShape(22.dp)) {
                Text(if (selectedLap < completed) "هذا الشوط مكتمل ✓" else "تمّ الشوط ${lapNames[selectedLap]} ✓", fontSize = 18.sp)
            }
            Spacer(Modifier.height(16.dp))
            }
        }
    } else {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("العمرة", modifier = Modifier.weight(1f), fontSize = 30.sp,
                    fontWeight = FontWeight.Bold, color = MushafGreen)
                TextButton(onClick = onBack) { Text("الأدعية") }
            }
            DedicationDivider(Modifier.width(120.dp).height(20.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UmrahMode.entries.forEach { value ->
                    Button(onClick = { changeMode(value) }, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mode == value) MushafGreen else MushafCream,
                            contentColor = if (mode == value) MushafCream else MushafGreen),
                        border = BorderStroke(1.dp, MushafSage), shape = RoundedCornerShape(22.dp)) {
                        Text(value.title, fontSize = 18.sp)
                    }
                }
            }
            Text("أتممت ${arabicNumber(completed)} من ٧", fontSize = 21.sp,
                fontWeight = FontWeight.Bold, color = MushafGreen)
            Text(if (completed == 7) "أتممت ${mode.title} ✓" else "الشوط الحالي: ${arabicNumber(completed + 1)}",
                fontSize = 15.sp, color = MushafGreen, modifier = Modifier.padding(bottom = 8.dp))
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(7) { index ->
                    val done = index < completed
                    val current = index == completed
                    Card(onClick = {
                        lap = index
                    }, modifier = Modifier.fillMaxWidth().weight(1f).testTag("${mode.key}-lap-${index + 1}"),
                        colors = CardDefaults.cardColors(containerColor = if (done) MushafGreen else MushafCream),
                        border = BorderStroke(if (current) 2.dp else 1.dp, if (current) MushafGold else MushafSage),
                        shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(arabicNumber(index + 1), fontSize = 19.sp, fontWeight = FontWeight.Bold,
                                color = if (done) MushafCream else MushafGreen)
                            Text(if (mode == UmrahMode.TAWAF) "الشوط ${lapNames[index]}" else saiRoute(index),
                                modifier = Modifier.weight(1f), fontSize = if (mode == UmrahMode.TAWAF) 17.sp else 15.sp,
                                color = if (done) MushafCream else MushafGreen)
                            Text(if (done) "تم ✓" else if (current) "الحالي" else "", fontSize = 14.sp,
                                color = if (done) MushafCream else MushafGold)
                        }
                    }
                }
            }
            if (mode == UmrahMode.SAI) Text("تبدأ من الصفا وتنتهي بالمروة", fontSize = 13.sp,
                color = MushafGreen, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(enabled = completed > 0, onClick = { confirm = "undo" }, modifier = Modifier.testTag("umrah-undo")) {
                    Text("التراجع عن آخر شوط", fontSize = 13.sp)
                }
                TextButton(onClick = { confirm = "reset" }, modifier = Modifier.testTag("umrah-reset")) { Text("إعادة الأشواط من البداية", fontSize = 13.sp) }
            }
            Text("التقدّم محفوظ • أدعية مقترحة، وليست أدعية مخصوصة لكل شوط", fontSize = 11.sp,
                color = MushafGreen.copy(alpha = .75f), textAlign = TextAlign.Center)
        }
    }
    if (confirm != null) AlertDialog(onDismissRequest = { confirm = null },
        title = { Text(when (confirm) { "complete" -> "أكملت الشوط؟"; "undo" -> "التراجع عن آخر شوط؟"; else -> "إعادة كل الأشواط من البداية؟" }) },
        text = { Text(when (confirm) {
            "complete" -> "علّم الشوط مكتملًا بعد إتمام اللفة${if (mode == UmrahMode.SAI) " والوصول إلى ${if (lap % 2 == 0) "المروة" else "الصفا"}" else ""}."
            "undo" -> "سيعود آخر شوط إلى غير مكتمل، ويمكنك إكماله مرة أخرى."
            else -> "ستُمسح علامات إتمام الطواف والسعي، وتعود كل الأشواط إلى غير مكتملة لتبدأ من الشوط الأول."
        }) },
        confirmButton = { TextButton(onClick = {
            when (confirm) {
                "complete" -> { setCount(completedUmrahLap(completed, lap)); lap = -1 }
                "undo" -> setCount(undoUmrahLap(completed))
                "reset" -> {
                    prefs.edit().clear().apply(); tawafCount = 0; saiCount = 0
                    modeName = UmrahMode.TAWAF.name; lap = -1
                }
            }
            confirm = null
        }, modifier = Modifier.testTag("umrah-confirm")) { Text(if (confirm == "complete") "نعم، أكملت الشوط" else if (confirm == "reset") "نعم، أعد الأشواط" else "تأكيد") } },
        dismissButton = { TextButton(onClick = { confirm = null }) { Text("إلغاء") } })
}

