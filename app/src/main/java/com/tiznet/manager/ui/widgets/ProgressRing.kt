package com.tiznet.manager.ui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tiznet.manager.R
import kotlin.math.min

/** Circular progress indicator with a centered percentage label. */
class ProgressRing @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.brand_100)
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.brand_500)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 44f
        textAlign = Paint.Align.CENTER
        color = ContextCompat.getColor(context, R.color.brand_700)
        isFakeBoldText = true
    }
    private val rect = RectF()
    private var animated = 0f
    private var target = 0f
    private var animator: ValueAnimator? = null

    var label: String = ""
        set(value) {
            field = value
            invalidate()
        }

    fun setProgress(percent: Double, colorRes: Int? = null) {
        target = (percent.coerceIn(0.0, 100.0)).toFloat()
        colorRes?.let { progressPaint.color = ContextCompat.getColor(context, it) }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(animated, target).apply {
            duration = 700
            addUpdateListener {
                animated = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val inset = trackPaint.strokeWidth
        rect.set(inset, inset, width - inset, height - inset)
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)
        canvas.drawArc(rect, -90f, 360f * animated / 100f, false, progressPaint)
        val fm = textPaint.fontMetrics
        val textY = height / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(label.ifBlank { "${animated.toInt()}%" }, width / 2f, textY, textPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }
}
