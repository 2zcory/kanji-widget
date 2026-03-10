package com.example.kanjiwidget.stats

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.kanjiwidget.MainActivity
import com.example.kanjiwidget.R
import com.example.kanjiwidget.home.HomeSummary

class StudyStatsBottomSheet(
    private val activity: MainActivity,
    private val summary: HomeSummary,
    private val repository: StudyStatsRepository,
) {
    private lateinit var chartView: StudyTimeChartView
    private lateinit var btnRange7: Button
    private lateinit var btnRange30: Button
    private lateinit var rangeLabel: TextView
    private lateinit var totalView: TextView
    private lateinit var averageView: TextView
    private lateinit var bestDayView: TextView
    private lateinit var latestView: TextView

    fun show() {
        val dialog = Dialog(activity)
        dialog.setContentView(R.layout.view_study_stats_bottom_sheet)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
        }

        chartView = dialog.findViewById(R.id.studyTimeChartView)
        btnRange7 = dialog.findViewById(R.id.btnChartRange7)
        btnRange30 = dialog.findViewById(R.id.btnChartRange30)
        rangeLabel = dialog.findViewById(R.id.tvChartRangeLabel)
        totalView = dialog.findViewById(R.id.tvChartTotal)
        averageView = dialog.findViewById(R.id.tvChartAverage)
        bestDayView = dialog.findViewById(R.id.tvChartBestDay)
        latestView = dialog.findViewById(R.id.tvChartLatestKanji)

        btnRange7.setOnClickListener { bindRange(7) }
        btnRange30.setOnClickListener { bindRange(30) }

        if (summary.latestKanji.isNullOrBlank()) {
            latestView.visibility = View.GONE
        } else {
            latestView.visibility = View.VISIBLE
            latestView.text = activity.getString(R.string.chart_latest_kanji_value, summary.latestKanji)
        }

        bindRange(7)
        dialog.show()
    }

    private fun bindRange(days: Int) {
        val chartSummary = repository.getDailyChart(days)
        chartView.points = chartSummary.points
        rangeLabel.text = activity.getString(R.string.chart_range_value, days)
        totalView.text = activity.getString(
            R.string.chart_total_value,
            activity.formatDurationForUi(chartSummary.totalMs)
        )
        averageView.text = activity.getString(
            R.string.chart_average_value,
            activity.formatDurationForUi(chartSummary.averageMs)
        )
        bestDayView.text = chartSummary.bestDay?.let {
            activity.getString(
                R.string.chart_best_day_value,
                it.date.format(java.time.format.DateTimeFormatter.ofPattern("d/M")),
                activity.formatDurationForUi(it.totalMs)
            )
        } ?: activity.getString(R.string.chart_best_day_empty)

        updateRangeButtons(days)
    }

    private fun updateRangeButtons(days: Int) {
        val selected = R.drawable.bg_chart_range_selected
        val idle = R.drawable.bg_chart_range_idle
        btnRange7.setBackgroundResource(if (days == 7) selected else idle)
        btnRange30.setBackgroundResource(if (days == 30) selected else idle)
    }
}
