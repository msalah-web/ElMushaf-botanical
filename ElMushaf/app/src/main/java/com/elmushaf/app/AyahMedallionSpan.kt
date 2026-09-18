package com.elmushaf.app

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.style.ReplacementSpan
import kotlin.math.cos
import kotlin.math.sin

/** Decoration only: the original verse marker remains in the text for accessibility. */
internal class AyahMedallionSpan(private val number: String) : ReplacementSpan() {
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int,
                         fm: Paint.FontMetricsInt?): Int = kotlin.math.ceil(paint.textSize * 1.30f).toInt()

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int,
                      x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val size = paint.textSize
        val cx = x + size * .65f
        val cy = y + (paint.fontMetrics.ascent + paint.fontMetrics.descent) * .5f
        val radius = size * .55f
        val ornament = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(143, 101, 52)
            style = Paint.Style.STROKE
            strokeWidth = (size * .014f).coerceAtLeast(1f)
        }
        val path = Path()
        for (step in 0..160) {
            val angle = step * 2.0 * Math.PI / 160
            val r = radius * (1.0 + .085 * cos(10 * angle))
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            if (step == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, ornament)
        canvas.drawCircle(cx, cy, radius * .88f, ornament)
        val digits = Paint(paint).apply {
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textSize = size * .65f
            // Keep three-digit verse numbers inside the inner ring.
            val availableWidth = radius * 1.55f
            val measuredWidth = measureText(number)
            if (measuredWidth > availableWidth) textSize *= availableWidth / measuredWidth
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.rgb(23, 32, 25)
        }
        canvas.drawText(number, cx, cy - (digits.fontMetrics.ascent + digits.fontMetrics.descent) / 2, digits)
    }
}
