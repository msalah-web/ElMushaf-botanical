package com.elmushaf.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

val MushafGreen = Color(0xFF143F30)
val MushafGold = Color(0xFFB38C46)
val MushafCream = Color(0xFFFAF7EF)
val MushafSage = Color(0xFF9AAB94)

@Composable
fun Ornament(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cy = size.height / 2
        drawLine(MushafGold, Offset(0f, cy), Offset(size.width, cy), 1.dp.toPx())
        for (i in -2..2) {
            val x = size.width / 2 + i * 14.dp.toPx()
            val radius = if (i == 0) 5.dp.toPx() else 2.5.dp.toPx()
            drawCircle(MushafCream, radius + 2.dp.toPx(), Offset(x, cy))
            drawCircle(MushafGold, radius, Offset(x, cy), style = Stroke(1.dp.toPx()))
        }
    }
}

@Composable
fun OrnateAppFrame(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(MushafCream)) {
        Image(painter = androidx.compose.ui.res.painterResource(R.drawable.dedication_botanical),
            contentDescription = null, modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop, alpha = .28f)
        content()
    }
}

@Composable
private fun MenuTile(title: String, symbol: String, modifier: Modifier, onClick: () -> Unit, enabled: Boolean = true, subtitle: String? = null) {
    Card(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MushafSage),
        colors = CardDefaults.cardColors(containerColor = MushafCream, disabledContainerColor = MushafCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = MushafGreen, border = BorderStroke(1.dp, MushafSage)) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    if (title == "متابعة القراءة") {
                        Image(painter = androidx.compose.ui.res.painterResource(R.drawable.mushaf_cover),
                            contentDescription = null, modifier = Modifier.size(26.dp, 32.dp))
                    } else if (title == "أذكار الصباح والمساء") {
                        Canvas(Modifier.size(23.dp)) {
                            val center = Offset(size.width / 2, size.height / 2)
                            val tint = Color(0xFFE4C784)
                            drawCircle(tint, size.minDimension * .22f, center)
                            for (i in 0..7) {
                                val angle = i * Math.PI / 4
                                val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                                drawLine(tint, center + direction * (size.minDimension * .32f),
                                    center + direction * (size.minDimension * .46f), 1.5.dp.toPx())
                            }
                        }
                    } else if (title == "الاستماع للقرآن") {
                        Canvas(Modifier.size(23.dp)) {
                            val speaker = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * .12f, size.height * .36f)
                                lineTo(size.width * .32f, size.height * .36f)
                                lineTo(size.width * .57f, size.height * .16f)
                                lineTo(size.width * .57f, size.height * .84f)
                                lineTo(size.width * .32f, size.height * .64f)
                                lineTo(size.width * .12f, size.height * .64f); close()
                            }
                            drawPath(speaker, Color(0xFFE4C784))
                            drawArc(Color(0xFFE4C784), -60f, 120f, false,
                                topLeft = Offset(size.width * .46f, size.height * .23f),
                                size = Size(size.width * .42f, size.height * .54f), style = Stroke(1.5.dp.toPx()))
                        }
                    } else Text(symbol, color = Color(0xFFE4C784), fontSize = 24.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(title, color = MushafGreen, fontSize = if (title == "أذكار الصباح والمساء") 13.sp else 15.sp, fontWeight = FontWeight.Bold,
                lineHeight = if (subtitle != null) 20.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        subtitle?.let {
            Text(it, color = MushafGreen.copy(alpha = .8f), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        }
        }
    }
}

@Composable
private fun HomeAction(title: String, detail: String, symbol: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MushafSage),
        colors = CardDefaults.cardColors(containerColor = MushafCream)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = MushafGreen) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Text(symbol, color = Color(0xFFE4C784), fontSize = 25.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = MushafGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = MushafGreen.copy(alpha = .65f), fontSize = 12.sp)
            }
            Text("‹", color = MushafGold, fontSize = 28.sp)
        }
    }
}

@Composable
fun MushafHome(lastPage: Int, onContinue: () -> Unit, onQuran: () -> Unit, onAdhkar: () -> Unit,
    onDuas: () -> Unit, onRuqyah: () -> Unit, onQibla: () -> Unit, onSources: () -> Unit, onShare: () -> Unit, onAudio: () -> Unit = {}) {
    val dailyReminder = rememberDailyDhikr()
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("المصحف الشريف", color = MushafGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        DedicationDivider(Modifier.width(120.dp).height(20.dp))
        Spacer(Modifier.height(12.dp))
        Card(onClick = onQuran, modifier = Modifier.fillMaxWidth().weight(1.15f),
            shape = RoundedCornerShape(28.dp), border = BorderStroke(1.dp, MushafGreen),
            colors = CardDefaults.cardColors(containerColor = MushafGreen)) {
            Row(Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).padding(top = 10.dp)) {
                    Text("القرآن الكريم", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MushafCream)
                    Spacer(Modifier.height(16.dp))
                    Text("افتح المصحف  ‹", fontSize = 14.sp, color = MushafCream)
                }
                Image(painter = androidx.compose.ui.res.painterResource(R.drawable.mushaf_cover),
                    contentDescription = null, modifier = Modifier.size(74.dp, 104.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        HomeAction("متابعة القراءة", if (lastPage in 1..604) "العودة إلى صفحة ${arabicNumber(lastPage)}" else "ابدأ رحلتك مع القرآن", "▤", onContinue)
        Spacer(Modifier.height(8.dp))
        HomeAction("الاستماع للقرآن", "اختر القارئ والسورة", "▷", onAudio)
        Spacer(Modifier.height(16.dp))
        val tiles = listOf(Triple("أذكار الصباح والمساء", "", onAdhkar), Triple("الأدعية", "✧", onDuas),
            Triple("الرقية الشرعية", "❈", onRuqyah), Triple("القبلة", "⌂", onQibla))
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().weight(.7f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (title, symbol, action) ->
                    MenuTile(title, symbol, Modifier.weight(1f).fillMaxHeight(), action)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = onShare) { Text("مشاركة التطبيق", color = MushafGreen, fontSize = 13.sp) }
            TextButton(onClick = onSources) { Text("المصادر والحقوق", color = MushafGreen, fontSize = 13.sp) }
        }
        run {
            Text(dailyReminder, fontSize = 13.sp,
                lineHeight = 20.sp, color = MushafGreen, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}
