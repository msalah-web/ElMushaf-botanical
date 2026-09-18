package com.elmushaf.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BotanicalDedication(onStart: () -> Unit, onContinue: () -> Unit, onSurahs: () -> Unit, onBack: () -> Unit) {
    var backVisible by remember { mutableStateOf(false) }
    LaunchedEffect(backVisible) {
        if (backVisible) { kotlinx.coroutines.delay(4000); backVisible = false }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFFFAF7EF))) {
        Image(painterResource(R.drawable.dedication_botanical), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val dedicationHeight = maxHeight * .62f
        val actionsHeight = maxHeight * .28f
        FitDedicationContent(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Column(Modifier.fillMaxWidth().heightIn(min = dedicationHeight).clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { backVisible = !backVisible }.testTag("dedication-image"), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly) {
                    Text("إهداء", fontSize = 32.sp, color = MushafGreen, textAlign = TextAlign.Center)
                    DedicationDivider(Modifier.width(110.dp).height(24.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("تم إنشاء هذا التطبيق صدقة جارية عن زوجتي", fontSize = 16.sp, lineHeight = 26.sp,
                        color = MushafGreen, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("شهيرة بنت محمد الجويني", fontSize = 23.sp, lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold, color = MushafGreen, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("نسأل الله أن يغفر لها ويرحمها ويجعلها في ميزان حسناتها،\nوعن موتى المسلمين جميعًا.",
                        fontSize = 16.sp, lineHeight = 25.sp, color = MushafGreen, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("اللهم اغفر لهم وارحمهم وعافهم واعف عنهم،\nوأكرم نزلهم، ووسع مدخلهم،\nواكتب الأجر والثواب لكل من قرأ وساهم في نشره.",
                        fontSize = 16.sp, lineHeight = 25.sp, color = MushafGreen, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    DedicationDivider(Modifier.width(120.dp).height(18.dp))
                    Text("محمد صلاح", fontSize = 21.sp, color = MushafGreen, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().heightIn(min = actionsHeight),
                    verticalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MushafGreen, contentColor = MushafCream),
                    shape = RoundedCornerShape(22.dp), contentPadding = PaddingValues(12.dp)) {
                    DedicationBookIcon(Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("بدء القراءة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("متابعة القراءة" to onContinue, "فهرس السور" to onSurahs).forEach { (label, action) ->
                        OutlinedButton(onClick = action, modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF9F7EE).copy(alpha = .9f), contentColor = MushafGreen),
                            border = BorderStroke(1.dp, Color(0xFF9AAB94)), shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) {
                            Text(if (label == "متابعة القراءة") "▶" else "☰", fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        }
        }
        if (backVisible) Surface(Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp),
            shape = RoundedCornerShape(12.dp), color = MushafCream) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("dedication-back")) { Text("العودة للرئيسية") }
        }
    }
}

@Composable
fun DedicationDivider(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = 5.dp.toPx()
        drawLine(MushafGold, androidx.compose.ui.geometry.Offset(0f, cy), androidx.compose.ui.geometry.Offset(cx - radius * 2, cy), .6.dp.toPx())
        drawLine(MushafGold, androidx.compose.ui.geometry.Offset(cx + radius * 2, cy), androidx.compose.ui.geometry.Offset(size.width, cy), .6.dp.toPx())
        for (i in 0..3) {
            val angle = i * Math.PI / 2
            val x = cx + kotlin.math.cos(angle).toFloat() * radius
            val y = cy + kotlin.math.sin(angle).toFloat() * radius
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(x, y - radius * .6f); lineTo(x + radius * .6f, y)
                lineTo(x, y + radius * .6f); lineTo(x - radius * .6f, y); close()
            }
            drawPath(path, MushafGold, style = androidx.compose.ui.graphics.drawscope.Stroke(.7.dp.toPx()))
        }
    }
}

@Composable
private fun DedicationBookIcon(modifier: Modifier) {
    Canvas(modifier) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * .5f, size.height * .24f)
            cubicTo(size.width * .35f, size.height * .08f, size.width * .16f, size.height * .1f, size.width * .08f, size.height * .16f)
            lineTo(size.width * .08f, size.height * .84f)
            cubicTo(size.width * .22f, size.height * .76f, size.width * .35f, size.height * .78f, size.width * .5f, size.height * .92f)
            cubicTo(size.width * .65f, size.height * .78f, size.width * .78f, size.height * .76f, size.width * .92f, size.height * .84f)
            lineTo(size.width * .92f, size.height * .16f)
            cubicTo(size.width * .84f, size.height * .1f, size.width * .65f, size.height * .08f, size.width * .5f, size.height * .24f)
            close()
        }
        drawPath(path, MushafCream, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        drawLine(MushafCream, androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .24f), androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .92f), 1.dp.toPx())
    }
}

// Measure the complete page before placing it so every item fits without clipping or scrolling.
@Composable
private fun FitDedicationContent(modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content = content, modifier = modifier) { measurables, constraints ->
        val contentWidth = minOf(constraints.maxWidth, 400.dp.roundToPx())
        val page = measurables.single().measure(
            androidx.compose.ui.unit.Constraints(minWidth = contentWidth, maxWidth = contentWidth)
        )
        val scale = minOf(1f, (constraints.maxHeight - 24.dp.roundToPx()).coerceAtLeast(1).toFloat() / page.height.coerceAtLeast(1))
        layout(constraints.maxWidth, constraints.maxHeight) {
            page.placeWithLayer(
                x = ((constraints.maxWidth - page.width * scale) / 2).toInt(),
                y = 12.dp.roundToPx()
            ) {
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
            }
        }
    }
}
