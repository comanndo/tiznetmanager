package com.tiznet.manager.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tiznet.manager.R

/** Lightweight bar + line chart for daily usage and online-user counts. */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.brand_400)
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.status_active)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.status_active)
    }
    private val linePath = Path()

    var usage: List<Long> = emptyList()
        set(value) {
            field = value
            invalidate()
        }
    var online: List<Int> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        if (usage.isEmpty()) return
        val pad = 8f
        val w = width - pad * 2
        val h = height - pad * 2
        val maxBytes = (usage.maxOrNull() ?: 1L).coerceAtLeast(1L)
        val maxOnline = (online.maxOrNull() ?: 1).coerceAtLeast(1)
        val slot = w / usage.size
        val barWidth = slot * 0.55f

        usage.forEachIndexed { index, value ->
            val barHeight = h * (value.toFloat() / maxBytes)
            val left = pad + slot * index + (slot - barWidth) / 2f
            val top = pad + h - barHeight
            val radius = barWidth / 3f
            canvas.drawRoundRect(left, top, left + barWidth, pad + h, radius, radius, barPaint)
        }

        linePath.rewind()
        online.forEachIndexed { index, value ->
            val x = pad + slot * index + slot / 2f
            val y = pad + h - h * (value.toFloat() / maxOnline)
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        canvas.drawPath(linePath, linePaint)
        online.forEachIndexed { index, value ->
            val x = pad + slot * index + slot / 2f
            val y = pad + h - h * (value.toFloat() / maxOnline)
            canvas.drawCircle(x, y, 4f, dotPaint)
        }
    }
}
