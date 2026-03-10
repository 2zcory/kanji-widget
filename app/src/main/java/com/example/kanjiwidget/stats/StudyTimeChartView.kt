package com.example.kanjiwidget.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.util.AttributeSet
import android.view.View
import java.time.format.DateTimeFormatter
import kotlin.math.max

class StudyTimeChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var points: List<StudyChartPoint> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val accentColor = 0xFF2F5D50.toInt()
    private val faintColor = 0x332F5D50
    private val axisColor = 0x55A1482E
    private val textColor = 0xFF6D6050.toInt()
    private val labelFormatter = DateTimeFormatter.ofPattern("d/M")

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }
    private val zeroBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = faintColor
        style = Paint.Style.FILL
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = axisColor
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = sp(11f)
        textAlign = Paint.Align.CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = dp(240f).toInt()
        val resolvedHeight = resolveSize(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(resolveSize(suggestedMinimumWidth, widthMeasureSpec), resolvedHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val chartLeft = paddingLeft + dp(8f)
        val chartTop = paddingTop + dp(12f)
        val chartRight = width - paddingRight - dp(8f)
        val chartBottom = height - paddingBottom - dp(26f)

        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint)

        if (points.isEmpty()) return

        val maxValue = max(points.maxOf { it.totalMs }, 60_000L)
        val slotWidth = (chartRight - chartLeft) / points.size.toFloat()
        val barWidth = slotWidth * 0.58f
        val zeroBarHeight = dp(3f)

        points.forEachIndexed { index, point ->
            val left = chartLeft + slotWidth * index + (slotWidth - barWidth) / 2f
            val right = left + barWidth
            val normalized = if (maxValue <= 0L) 0f else point.totalMs.toFloat() / maxValue.toFloat()
            val barHeight = if (point.totalMs <= 0L) {
                zeroBarHeight
            } else {
                (chartBottom - chartTop) * normalized
            }
            val top = chartBottom - barHeight
            val paint = if (point.totalMs <= 0L) zeroBarPaint else barPaint
            canvas.drawRoundRect(RectF(left, top, right, chartBottom), dp(6f), dp(6f), paint)

            if (shouldDrawLabel(index, points.size)) {
                canvas.drawText(
                    point.date.format(labelFormatter),
                    left + barWidth / 2f,
                    height - paddingBottom - dp(6f),
                    textPaint
                )
            }
        }
    }

    private fun shouldDrawLabel(index: Int, count: Int): Boolean {
        return when {
            count <= 7 -> true
            count <= 30 -> index == 0 || index == count - 1 || index % 7 == 0
            else -> index == 0 || index == count - 1 || index % 10 == 0
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}
