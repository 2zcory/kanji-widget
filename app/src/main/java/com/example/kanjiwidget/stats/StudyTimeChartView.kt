package com.example.kanjiwidget.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.util.AttributeSet
import android.view.View
import com.example.kanjiwidget.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val zeroBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val focusBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val focusDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sp(11f)
        textAlign = Paint.Align.CENTER
    }
    private val barRect = RectF()

    init {
        refreshThemeColors()
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
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
            .withLocale(resources.configuration.locales[0])

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
            val isFocusedToday = index == points.lastIndex && point.totalMs > 0L
            val paint = when {
                point.totalMs <= 0L -> zeroBarPaint
                isFocusedToday -> focusBarPaint
                else -> barPaint
            }
            barRect.set(left, top, right, chartBottom)
            canvas.drawRoundRect(barRect, dp(6f), dp(6f), paint)
            if (isFocusedToday) {
                canvas.drawCircle(
                    left + barWidth / 2f,
                    top - dp(6f),
                    dp(3f),
                    focusDotPaint
                )
            }

            if (shouldDrawLabel(index, points.size)) {
                canvas.drawText(
                    point.date.format(formatter),
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

    private fun refreshThemeColors() {
        barPaint.color = resolveColor(R.attr.colorChartBar)
        zeroBarPaint.color = resolveColor(R.attr.colorChartBarFaint)
        focusBarPaint.color = resolveColor(R.attr.colorAccentWarm)
        focusDotPaint.color = resolveColor(R.attr.colorAccentWarm)
        axisPaint.color = resolveColor(R.attr.colorChartAxis)
        textPaint.color = resolveColor(R.attr.colorTextMuted)
    }

    private fun resolveColor(attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}
