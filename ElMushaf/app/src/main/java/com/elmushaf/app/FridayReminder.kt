package com.elmushaf.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Calendar
import java.util.TimeZone

internal fun isFriday(nowMillis: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Boolean =
    Calendar.getInstance(zone).apply { timeInMillis = nowMillis }.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

internal fun localDayOfWeek(nowMillis: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Int =
    Calendar.getInstance(zone).apply { timeInMillis = nowMillis }.get(Calendar.DAY_OF_WEEK)

internal fun dailyDhikr(day: Int): String = when (day) {
    Calendar.SUNDAY -> "أستغفر الله وأتوب إليه"
    Calendar.MONDAY -> "سبحان الله وبحمده، سبحان الله العظيم"
    Calendar.TUESDAY -> "لا إله إلا أنت سبحانك إني كنت من الظالمين"
    Calendar.WEDNESDAY -> "سبحان الله وبحمده"
    Calendar.THURSDAY -> "لا حول ولا قوة إلا بالله"
    Calendar.FRIDAY -> "اللهم صلِّ وسلِّم وبارك على سيدنا محمد"
    else -> "الحمد لله رب العالمين"
}

@Composable
fun rememberDailyDhikr(): String = dailyDhikr(rememberLocalDay())

@Composable
fun rememberFriday(): Boolean = rememberLocalDay() == Calendar.FRIDAY

@Composable
private fun rememberLocalDay(): Int {
    var day by remember { mutableIntStateOf(localDayOfWeek()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) day = localDayOfWeek()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            day = localDayOfWeek()
            val nextDay = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            kotlinx.coroutines.delay((nextDay.timeInMillis - System.currentTimeMillis()).coerceIn(1L, 30_000L))
        }
    }
    return day
}

@Composable
fun FridayReminder(modifier: Modifier = Modifier, plain: Boolean = false) {
    val context = LocalContext.current
    val verse = remember(context) {
        context.assets.open("quran-uthmani.txt").bufferedReader().useLines { lines ->
            lines.first { it.startsWith("33|56|") }.substringAfter("33|56|")
        }
    }
    Surface(modifier.testTag("friday-reminder"), shape = RoundedCornerShape(18.dp),
        color = if (plain) Color.Transparent else MushafCream, border = if (plain) null else BorderStroke(1.dp, MushafGold.copy(alpha = .5f))) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (plain) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.foundation.Canvas(Modifier.weight(1f).height(1.dp)) {
                        drawLine(MushafGold.copy(alpha = .4f), androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width, 0f), .5.dp.toPx())
                    }
                    Text("تذكير يوم الجمعة", fontSize = 14.sp, color = MushafGold)
                    androidx.compose.foundation.Canvas(Modifier.weight(1f).height(1.dp)) {
                        drawLine(MushafGold.copy(alpha = .4f), androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width, 0f), .5.dp.toPx())
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Text("﴿$verse﴾", fontSize = 16.sp, lineHeight = 27.sp,
                color = MushafGreen, textAlign = TextAlign.Center)
            Text("الأحزاب: ٥٦", fontSize = 12.sp, color = MushafGreen.copy(alpha = .7f))
            if (plain) DedicationDivider(Modifier.width(110.dp).height(20.dp))
            Spacer(Modifier.height(6.dp))
            Text("اللهم صلِّ وسلِّم وبارك على سيدنا محمد", fontSize = 15.sp,
                lineHeight = 24.sp, color = MushafGreen, textAlign = TextAlign.Center)
        }
    }
}
