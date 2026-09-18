package com.elmushaf.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Dua(val title: String, val text: String, val source: String, val url: String)

@Composable
fun DuasScreen(onBack: () -> Unit) {
    var showUmrah by rememberSaveable { mutableStateOf(false) }
    if (showUmrah) {
        UmrahScreen(onBack = { showUmrah = false })
        return
    }
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val duas = remember {
        // Preserve the complete verses from the app's attributed Tanzil text.
        val verses = context.assets.open("quran-uthmani.txt").bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val fields = line.split('|', limit = 3)
                if (fields.size == 3 && fields[0].toIntOrNull() != null)
                    "${fields[0]}:${fields[1]}" to fields[2] else null
            }.toMap()
        }
        fun quran(title: String, surah: Int, ayah: Int, name: String) = Dua(
            title, verses.getValue("$surah:$ayah"),
            "القرآن الكريم — سورة $name، الآية ${arabicNumber(ayah)}",
            "https://quran.com/$surah/$ayah"
        )
        listOf(
            quran("خير الدنيا والآخرة", 2, 201, "البقرة"),
            quran("الثبات والهداية", 3, 8, "آل عمران"),
            quran("التوبة والمغفرة", 7, 23, "الأعراف"),
            quran("الدعاء للوالدين والمؤمنين", 14, 41, "إبراهيم"),
            Dua("شرح الصدر وتيسير الأمر", verses.getValue("20:25") + "\n" + verses.getValue("20:26"),
                "القرآن الكريم — سورة طه، الآيتان ٢٥–٢٦", "https://quran.com/20/25-26"),
            quran("المغفرة والرحمة", 23, 118, "المؤمنون"),
            quran("صلاح الأسرة والذرية", 25, 74, "الفرقان"),
            quran("قبول العمل", 2, 127, "البقرة"),
            quran("الإسلام والتوبة", 2, 128, "البقرة"),
            quran("الصبر والثبات", 2, 250, "البقرة"),
            quran("العفو ورفع المشقة", 2, 286, "البقرة"),
            quran("الذرية الطيبة", 3, 38, "آل عمران"),
            quran("المغفرة وحسن الخاتمة", 3, 193, "آل عمران"),
            quran("إقامة الصلاة", 14, 40, "إبراهيم"),
            quran("الرحمة للوالدين", 17, 24, "الإسراء"),
            quran("التوفيق في الدخول والخروج", 17, 80, "الإسراء"),
            quran("الرحمة والرشد", 18, 10, "الكهف"),
            quran("زيادة العلم", 20, 114, "طه"),
            quran("دعاء أيوب عليه السلام", 21, 83, "الأنبياء"),
            quran("دعاء يونس عليه السلام", 21, 87, "الأنبياء"),
            quran("المنزل المبارك", 23, 29, "المؤمنون"),
            quran("شكر النعمة والعمل الصالح", 27, 19, "النمل"),
            quran("الافتقار إلى الله وسؤال الخير", 28, 24, "القصص"),
            quran("سلامة القلب للمؤمنين", 59, 10, "الحشر"),
            Dua("الهدى والتقى والعفاف والغنى",
                "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى وَالتُّقَى وَالْعَفَافَ وَالْغِنَى",
                "صحيح مسلم — حديث ٢٧٢١ (الرواية أ) • صحيح", "https://sunnah.com/muslim:2721a"),
            Dua("صلاح الدين والدنيا والآخرة",
                "اللَّهُمَّ أَصْلِحْ لِي دِينِيَ الَّذِي هُوَ عِصْمَةُ أَمْرِي، وَأَصْلِحْ لِي دُنْيَايَ الَّتِي فِيهَا مَعَاشِي، وَأَصْلِحْ لِي آخِرَتِي الَّتِي فِيهَا مَعَادِي، وَاجْعَلِ الْحَيَاةَ زِيَادَةً لِي فِي كُلِّ خَيْرٍ، وَاجْعَلِ الْمَوْتَ رَاحَةً لِي مِنْ كُلِّ شَرٍّ",
                "صحيح مسلم — حديث ٢٧٢٠ • صحيح", "https://sunnah.com/muslim:2720"),
            Dua("تزكية النفس وخشوع القلب",
                "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْعَجْزِ وَالْكَسَلِ وَالْجُبْنِ وَالْبُخْلِ وَالْهَرَمِ وَعَذَابِ الْقَبْرِ، اللَّهُمَّ آتِ نَفْسِي تَقْوَاهَا وَزَكِّهَا أَنْتَ خَيْرُ مَنْ زَكَّاهَا أَنْتَ وَلِيُّهَا وَمَوْلَاهَا، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ عِلْمٍ لَا يَنْفَعُ وَمِنْ قَلْبٍ لَا يَخْشَعُ وَمِنْ نَفْسٍ لَا تَشْبَعُ وَمِنْ دَعْوَةٍ لَا يُسْتَجَابُ لَهَا",
                "صحيح مسلم — حديث ٢٧٢٢ • صحيح", "https://sunnah.com/muslim:2722"),
            Dua("الاستعاذة من الهم والحزن والدَّين",
                "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْجُبْنِ وَالْبُخْلِ، وَضَلَعِ الدَّيْنِ، وَغَلَبَةِ الرِّجَالِ",
                "صحيح البخاري — حديث ٦٣٦٩ • صحيح", "https://sunnah.com/bukhari:6369"),
            Dua("المغفرة والهداية والرزق",
                "اللَّهُمَّ اغْفِرْ لِي وَارْحَمْنِي وَاهْدِنِي وَارْزُقْنِي",
                "صحيح مسلم — حديث ٢٦٩٧ (الرواية أ) • صحيح", "https://sunnah.com/muslim:2697a"),
            Dua("دعاء في الصلاة",
                "اللَّهُمَّ إِنِّي ظَلَمْتُ نَفْسِي ظُلْمًا كَثِيرًا، وَلَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ، فَاغْفِرْ لِي مَغْفِرَةً مِنْ عِنْدِكَ، وَارْحَمْنِي، إِنَّكَ أَنْتَ الْغَفُورُ الرَّحِيمُ",
                "صحيح البخاري — حديث ٦٣٢٦ • صحيح", "https://sunnah.com/bukhari:6326")
        )
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)) {
        TextButton(onClick = onBack) { Text("الرئيسية") }
        Text("الأدعية", fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text("من القرآن الكريم والسنة الصحيحة", fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.secondary)
        Button(onClick = { showUmrah = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).heightIn(min = 52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)) {
            Text("العمرة — أدعية ومتابعة الأشواط", fontSize = 18.sp)
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)) {
            items(duas, key = { it.url }) { dua ->
                Card(Modifier.fillMaxWidth(), border = androidx.compose.foundation.BorderStroke(1.dp, MushafSage), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(dua.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(dua.text, fontSize = 23.sp, lineHeight = 40.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth())
                        Text(dua.source, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                        TextButton(onClick = { uriHandler.openUri(dua.url) }) { Text("عرض المصدر") }
                    }
                }
            }

        }
    }
}
