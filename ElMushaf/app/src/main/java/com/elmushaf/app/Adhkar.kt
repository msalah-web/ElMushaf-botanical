package com.elmushaf.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign

private data class Dhikr(val title: String, val text: String, val count: Int = 1, val centerBasmala: Boolean = false)

// Traditional Quran/hadith texts, following the morning/evening chapter of Hisn al-Muslim.
// https://www.e-quran.com/pages/hisnmuslim/athkar_alsbah_walmsaa%27.html
// The prose says seven repetitions for "Hasbi Allah"; the page's numeric label is a typo.
private fun dailyAdhkar(evening: Boolean): List<Dhikr> {
    val start = if (evening) "أمسينا وأمسى" else "أصبحنا وأصبح"
    val period = if (evening) "هذه الليلة" else "هذا اليوم"
    val after = if (evening) "بعدها" else "بعده"
    return buildList {
        add(Dhikr("الملك لله", "$start الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير، رب أسألك خير ما في $period وخير ما $after، وأعوذ بك من شر ما في $period وشر ما $after، رب أعوذ بك من الكسل وسوء الكبر، رب أعوذ بك من عذاب في النار وعذاب في القبر."))
        add(Dhikr("بك نحيا", if (evening) "اللهم بك أمسينا، وبك أصبحنا، وبك نحيا، وبك نموت، وإليك المصير." else "اللهم بك أصبحنا، وبك أمسينا، وبك نحيا، وبك نموت، وإليك النشور."))
        add(Dhikr("سيد الاستغفار", "اللهم أنت ربي لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت، أعوذ بك من شر ما صنعت، أبوء لك بنعمتك علي، وأبوء بذنبي فاغفر لي فإنه لا يغفر الذنوب إلا أنت."))
        add(Dhikr("الإشهاد بالتوحيد", "اللهم إني ${if (evening) "أمسيت" else "أصبحت"} أشهدك، وأشهد حملة عرشك، وملائكتك، وجميع خلقك، أنك أنت الله لا إله إلا أنت وحدك لا شريك لك، وأن محمدًا عبدك ورسولك.", 4))
        add(Dhikr("شكر النعمة", "اللهم ما ${if (evening) "أمسى" else "أصبح"} بي من نعمة أو بأحد من خلقك فمنك وحدك لا شريك لك، فلك الحمد ولك الشكر."))
        add(Dhikr("العافية", "اللهم عافني في بدني، اللهم عافني في سمعي، اللهم عافني في بصري، لا إله إلا أنت. اللهم إني أعوذ بك من الكفر والفقر، وأعوذ بك من عذاب القبر، لا إله إلا أنت.", 3))
        add(Dhikr("التوكل", "حسبي الله لا إله إلا هو عليه توكلت وهو رب العرش العظيم.", 7))
        add(Dhikr("العفو والعافية", "اللهم إني أسألك العفو والعافية في الدنيا والآخرة، اللهم إني أسألك العفو والعافية في ديني ودنياي وأهلي ومالي، اللهم استر عوراتي، وآمن روعاتي، اللهم احفظني من بين يدي، ومن خلفي، وعن يميني، وعن شمالي، ومن فوقي، وأعوذ بعظمتك أن أغتال من تحتي."))
        add(Dhikr("الاستعاذة من الشر", "اللهم عالم الغيب والشهادة، فاطر السماوات والأرض، رب كل شيء ومليكه، أشهد أن لا إله إلا أنت، أعوذ بك من شر نفسي، ومن شر الشيطان وشركه، وأن أقترف على نفسي سوءًا أو أجره إلى مسلم."))
        add(Dhikr("بسم الله", "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم.", 3))
        add(Dhikr("الرضا", "رضيت بالله ربًا، وبالإسلام دينًا، وبمحمد صلى الله عليه وسلم نبيًا.", 3))
        add(Dhikr("يا حي يا قيوم", "يا حي يا قيوم برحمتك أستغيث، أصلح لي شأني كله ولا تكلني إلى نفسي طرفة عين."))
        add(Dhikr("سؤال الخير", if (evening) "أمسينا وأمسى الملك لله رب العالمين، اللهم إني أسألك خير هذه الليلة: فتحها ونصرها ونورها وبركتها وهداها، وأعوذ بك من شر ما فيها وشر ما بعدها." else "أصبحنا وأصبح الملك لله رب العالمين، اللهم إني أسألك خير هذا اليوم: فتحه ونصره ونوره وبركته وهداه، وأعوذ بك من شر ما فيه وشر ما بعده."))
        add(Dhikr("فطرة الإسلام", "${if (evening) "أمسينا" else "أصبحنا"} على فطرة الإسلام، وعلى كلمة الإخلاص، وعلى دين نبينا محمد صلى الله عليه وسلم، وعلى ملة أبينا إبراهيم حنيفًا مسلمًا وما كان من المشركين."))
        add(Dhikr("التسبيح", "سبحان الله وبحمده.", 100))
        add(Dhikr("التهليل — عشر مرات أو مرة واحدة", "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير.", 10))
        if (!evening) {
            add(Dhikr("التهليل — مائة مرة صباحًا", "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير.", 100))
            add(Dhikr("عدد خلقه", "سبحان الله وبحمده، عدد خلقه، ورضا نفسه، وزنة عرشه، ومداد كلماته.", 3))
            add(Dhikr("بعد صلاة الصبح", "اللهم إني أسألك علمًا نافعًا، ورزقًا طيبًا، وعملًا متقبلًا."))
        }
        add(Dhikr("الاستغفار — مائة مرة في اليوم", "أستغفر الله وأتوب إليه.", 100))
        if (evening) add(Dhikr("الاستعاذة", "أعوذ بكلمات الله التامات من شر ما خلق.", 3))
        add(Dhikr("الصلاة على النبي", "اللهم صل وسلم على نبينا محمد.", 10))
    }
}

@Composable
fun AdhkarScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var evening by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val quranAdhkar = remember {
        val verses = context.assets.open("quran-uthmani.txt").bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val fields = line.split('|', limit = 3)
                val surah = fields.getOrNull(0)?.toIntOrNull()
                val ayah = fields.getOrNull(1)?.toIntOrNull()
                if (surah != null && ayah != null && fields.size == 3) Triple(surah, ayah, fields[2]) else null
            }.toList()
        }
        listOf(Dhikr("آية الكرسي — البقرة ٢٥٥", verses.first { it.first == 2 && it.second == 255 }.third)) +
            (112..114).map { number ->
                Dhikr("سورة ${surahs[number - 1]}", verses.filter { it.first == number }
                    .joinToString("\n") { "${it.third} (${arabicNumber(it.second)})" }, 3, centerBasmala = true)
            }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)) {
        TextButton(onClick = onBack) { Text("الرئيسية") }
        Text("أذكار الصباح والمساء", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = !evening, onClick = { evening = false }, label = { Text("الصباح") }, colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MushafGreen, selectedLabelColor = MushafCream,
                labelColor = MushafGreen))
            FilterChip(selected = evening, onClick = { evening = true }, label = { Text("المساء") }, colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MushafGreen, selectedLabelColor = MushafCream,
                labelColor = MushafGreen))
        }
        key(evening) {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)) {
                itemsIndexed(quranAdhkar + dailyAdhkar(evening)) { _, dhikr ->
                    Card(Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, MushafSage), colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(dhikr.title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            DhikrText(dhikr.text, dhikr.centerBasmala)
                            Text("التكرار: ${arabicNumber(dhikr.count)}", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                item { Text("المصدر: حصن المسلم — باب أذكار الصباح والمساء", fontSize = 13.sp) }
            }
        }
    }
}

// Use exact source boundaries, so different Quran diacritics cannot break centering.
internal fun splitQuranBasmala(text: String): Pair<String, String> {
    val end = Regex("\\S+").findAll(text).take(4).last().range.last + 1
    return text.substring(0, end) to text.substring(end)
}

@Composable
private fun DhikrText(text: String, centerBasmala: Boolean = false) {
    if (centerBasmala) {
        val (basmala, verses) = splitQuranBasmala(text)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(basmala, fontSize = 21.sp, lineHeight = 36.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Text(verses, fontSize = 21.sp, lineHeight = 36.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }
    } else {
        Text(text, fontSize = 21.sp, lineHeight = 36.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
    }
}
