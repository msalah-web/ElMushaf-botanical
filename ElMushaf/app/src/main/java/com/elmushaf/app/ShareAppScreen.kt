package com.elmushaf.app

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShareAppScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val downloadUrl = stringResource(R.string.app_share_url).trim()
    Column(
        Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("مشاركة التطبيق", fontSize = 30.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(32.dp))
        OutlinedCard(Modifier.fillMaxWidth()) {
            Text("الدال على الخير كفاعله", modifier = Modifier.fillMaxWidth().padding(24.dp),
                fontSize = 26.sp, lineHeight = 38.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(24.dp))
        Text("شارك تطبيق المصحف الشريف مع أهلك وأصدقائك",
            fontSize = 20.sp, lineHeight = 30.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            enabled = downloadUrl.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "المصحف الشريف")
                    putExtra(Intent.EXTRA_TEXT,
                        "الدال على الخير كفاعله\n\nشارك الخير مع من تحب. حمّل تطبيق المصحف الشريف للقرآن الكريم والأذكار والأدعية والقبلة:\n$downloadUrl")
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة التطبيق عبر"))
            }
        ) { Text("مشاركة التطبيق", fontSize = 20.sp) }
        if (downloadUrl.isEmpty()) {
            Text("رابط التحميل سيكون متاحًا قريبًا", modifier = Modifier.padding(top = 12.dp),
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBack) { Text("العودة للرئيسية") }
    }
}
