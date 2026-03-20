package com.example.kanjiwidget.stats

import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.example.kanjiwidget.KanjiDetailNavigator
import com.example.kanjiwidget.MainActivity
import com.example.kanjiwidget.R
import com.example.kanjiwidget.home.HomeSummary
import com.example.kanjiwidget.theme.ThemeController
import com.example.kanjiwidget.widget.KanjiWidgetPrefs
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class StudyStatsBottomSheet(
    private val activity: MainActivity,
    private val summary: HomeSummary,
    private val repository: StudyStatsRepository,
) {
    private val rankingRepository = KanjiRankingRepository(activity)
    private lateinit var scrollView: androidx.core.widget.NestedScrollView
    private lateinit var chartView: StudyTimeChartView
    private lateinit var guidanceCardView: View
    private lateinit var guidanceTitleView: TextView
    private lateinit var guidanceBodyView: TextView
    private lateinit var guidanceBadgeView: TextView
    private lateinit var chartCardView: View
    private lateinit var btnRange7: Button
    private lateinit var btnRange30: Button
    private lateinit var btnRankingAll: Button
    private lateinit var btnRanking30: Button
    private lateinit var btnRanking7: Button
    private lateinit var btnMetricTime: Button
    private lateinit var btnMetricOpen: Button
    private lateinit var sectionRankingLeastView: View
    private lateinit var rankingLeastPreviewRow: View
    private lateinit var rankingLeastPreviewText: TextView
    private lateinit var sheetGuidanceTitleView: TextView
    private lateinit var sheetGuidanceBodyView: TextView
    private lateinit var rangeLabel: TextView
    private lateinit var summaryHintView: TextView
    private lateinit var totalView: TextView
    private lateinit var averageSummaryView: TextView
    private lateinit var activeDaysView: TextView
    private lateinit var currentStreakView: TextView
    private lateinit var bestDayView: TextView
    private lateinit var rankingMostTitle: TextView
    private lateinit var rankingLeastTitle: TextView
    private lateinit var rankingMostContainer: LinearLayout
    private lateinit var rankingLeastContainer: LinearLayout
    private lateinit var rankingEmptyView: TextView

    private var currentRankingScope = RankingScope.ALL_TIME
    private var currentRankingMetric = RankingMetric.STUDY_TIME
    private var rankingLeastItemCount = 0
    private var rankingLeastItems: List<KanjiStudyRankItem> = emptyList()

    fun show() {
        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(R.layout.view_study_stats_bottom_sheet)
        scrollView = dialog.findViewById(R.id.statsSheetScroll)!!
        chartView = dialog.findViewById(R.id.studyTimeChartView)!!
        guidanceCardView = dialog.findViewById(R.id.cardGuidance)!!
        guidanceTitleView = dialog.findViewById(R.id.tvGuidanceTitle)!!
        guidanceBodyView = dialog.findViewById(R.id.tvGuidanceBody)!!
        guidanceBadgeView = dialog.findViewById(R.id.tvGuidanceBadge)!!
        chartCardView = dialog.findViewById(R.id.statsChartCard)!!
        btnRange7 = dialog.findViewById(R.id.btnChartRange7)!!
        btnRange30 = dialog.findViewById(R.id.btnChartRange30)!!
        btnRankingAll = dialog.findViewById(R.id.btnRankingScopeAll)!!
        btnRanking30 = dialog.findViewById(R.id.btnRankingScope30)!!
        btnRanking7 = dialog.findViewById(R.id.btnRankingScope7)!!
        btnMetricTime = dialog.findViewById(R.id.btnRankingMetricTime)!!
        btnMetricOpen = dialog.findViewById(R.id.btnRankingMetricOpen)!!
        sectionRankingLeastView = dialog.findViewById(R.id.sectionRankingLeast)!!
        rankingLeastPreviewRow = dialog.findViewById(R.id.rowRankingLeastPreview)!!
        rankingLeastPreviewText = dialog.findViewById(R.id.tvRankingLeastPreview)!!
        sheetGuidanceTitleView = dialog.findViewById(R.id.tvSheetGuidanceTitle)!!
        sheetGuidanceBodyView = dialog.findViewById(R.id.tvSheetGuidanceBody)!!
        rangeLabel = dialog.findViewById(R.id.tvChartRangeLabel)!!
        summaryHintView = dialog.findViewById(R.id.tvChartSummaryHint)!!
        totalView = dialog.findViewById(R.id.tvChartTotal)!!
        averageSummaryView = dialog.findViewById(R.id.tvChartAverageSummary)!!
        activeDaysView = dialog.findViewById(R.id.tvChartActiveDays)!!
        currentStreakView = dialog.findViewById(R.id.tvChartCurrentStreak)!!
        bestDayView = dialog.findViewById(R.id.tvChartBestDay)!!
        rankingMostTitle = dialog.findViewById(R.id.tvRankingMostTitle)!!
        rankingLeastTitle = dialog.findViewById(R.id.tvRankingLeastTitle)!!
        rankingMostContainer = dialog.findViewById(R.id.containerRankingMost)!!
        rankingLeastContainer = dialog.findViewById(R.id.containerRankingLeast)!!
        rankingEmptyView = dialog.findViewById(R.id.tvRankingEmpty)!!

        val behavior = dialog.behavior
        dialog.setOnShowListener {
            configureBehavior(dialog, behavior)
        }

        btnRange7.setOnClickListener { bindRange(7) }
        btnRange30.setOnClickListener { bindRange(30) }
        btnRankingAll.setOnClickListener { updateRankingScope(RankingScope.ALL_TIME) }
        btnRanking30.setOnClickListener { updateRankingScope(RankingScope.LAST_30_DAYS) }
        btnRanking7.setOnClickListener { updateRankingScope(RankingScope.LAST_7_DAYS) }
        btnMetricTime.setOnClickListener { updateRankingMetric(RankingMetric.STUDY_TIME) }
        btnMetricOpen.setOnClickListener { updateRankingMetric(RankingMetric.OPEN_COUNT) }
        rankingLeastPreviewRow.setOnClickListener { showLeastRankingDetailSheet() }

        bindGuidance(dialog)
        bindRange(7)
        bindRanking()
        applyDepthStyling(dialog)
        dialog.show()
    }

    private fun configureBehavior(
        dialog: BottomSheetDialog,
        behavior: BottomSheetBehavior<FrameLayout>,
    ) {
        val bottomSheet =
            dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return
        bottomSheet.setBackgroundResource(android.R.color.transparent)
        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        behavior.isHideable = true
        behavior.skipCollapsed = false
        behavior.peekHeight = (activity.resources.displayMetrics.heightPixels * 0.64f).toInt()
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun bindGuidance(dialog: BottomSheetDialog) {
        val streakDays = repository.getDailyChart(30).currentStreakDays
        val hasLatest = !summary.latestKanji.isNullOrBlank()
        val catalog = KanjiWidgetPrefs.getKanjiCatalog(activity)
        val hasRandomFallback = catalog.isNotEmpty()
        val randomIntent = KanjiDetailNavigator.buildRandomDetailIntent(
            context = activity,
            catalog = catalog,
            currentKanji = summary.latestKanji,
        )

        guidanceTitleView.text = if (streakDays > 0 || summary.todayStudyMs > 0L) {
            activity.getString(R.string.chart_status_title_active)
        } else if (hasRandomFallback) {
            activity.getString(R.string.chart_status_title_restart)
        } else {
            activity.getString(R.string.chart_status_title_empty)
        }
        sheetGuidanceTitleView.text = activity.getString(R.string.chart_title)
        guidanceBodyView.text = if (hasLatest) {
            activity.getString(
                R.string.chart_status_body_latest,
                summary.latestKanji,
                formatRelativeTime(summary.latestViewedAt)
            )
        } else if (summary.todayStudyMs > 0L) {
            activity.getString(
                R.string.chart_status_body_today,
                formatDurationCompact(summary.todayStudyMs)
            )
        } else if (hasRandomFallback) {
            activity.getString(R.string.chart_status_body_random)
        } else {
            activity.getString(R.string.chart_status_body_empty)
        }
        sheetGuidanceBodyView.text = activity.getString(R.string.chart_sheet_body)
        guidanceBadgeView.text = if (hasLatest) {
            summary.latestKanji
        } else if (summary.todayStudyMs > 0L || streakDays > 0) {
            activity.getString(R.string.chart_status_badge_active)
        } else if (hasRandomFallback) {
            activity.getString(R.string.chart_status_badge_random)
        } else {
            activity.getString(R.string.chart_metric_value_empty)
        }

        if (hasLatest && summary.latestKanji != null) {
            guidanceCardView.isEnabled = true
            guidanceCardView.alpha = 1f
            guidanceCardView.setOnClickListener {
                dialog.dismiss()
                activity.startActivity(
                    KanjiDetailNavigator.buildDetailIntent(
                        context = activity,
                        kanji = summary.latestKanji.orEmpty(),
                        meaningFallback = summary.latestMeaning,
                        jlptFallback = summary.latestJlpt,
                    )
                )
            }
        } else if (randomIntent != null) {
            guidanceCardView.isEnabled = true
            guidanceCardView.alpha = 1f
            guidanceCardView.setOnClickListener {
                dialog.dismiss()
                activity.startActivity(randomIntent)
            }
        } else {
            guidanceCardView.isEnabled = false
            guidanceCardView.alpha = 0.72f
            guidanceCardView.setOnClickListener(null)
        }
    }

    private fun updateRankingScope(scope: RankingScope) {
        currentRankingScope = scope
        bindRanking()
    }

    private fun updateRankingMetric(metric: RankingMetric) {
        currentRankingMetric = metric
        bindRanking()
    }

    private fun bindRange(days: Int) {
        val chartSummary = repository.getDailyChart(days)
        val hasStudyData = chartSummary.totalMs > 0L
        chartView.points = chartSummary.points
        val dayCountText = activity.resources.getQuantityString(R.plurals.day_count, days, days)
        rangeLabel.text = activity.getString(R.string.chart_range_value, dayCountText)
        summaryHintView.text = if (hasStudyData) {
            chartSummary.bestDay?.let {
                val locale = currentLocale()
                val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
                activity.getString(
                    R.string.chart_summary_compact_with_best,
                    formatDurationCompact(chartSummary.averageMs),
                    it.date.format(formatter),
                    formatDurationCompact(it.totalMs)
                )
            } ?: activity.getString(
                R.string.chart_summary_compact_average_only,
                formatDurationCompact(chartSummary.averageMs)
            )
        } else {
            activity.getString(R.string.chart_average_empty_value)
        }
        totalView.text = if (hasStudyData) {
            formatDurationCompact(chartSummary.totalMs)
        } else {
            activity.getString(R.string.chart_metric_value_empty)
        }
        averageSummaryView.text = if (hasStudyData) {
            activity.getString(
                R.string.chart_support_average_value,
                formatDurationCompact(chartSummary.averageMs)
            )
        } else {
            activity.getString(R.string.chart_support_average_empty)
        }
        activeDaysView.text = activity.resources.getQuantityString(
            R.plurals.day_count,
            chartSummary.activeDays,
            chartSummary.activeDays
        )
        currentStreakView.text = if (chartSummary.currentStreakDays > 0) {
            activity.resources.getQuantityString(
                R.plurals.day_count,
                chartSummary.currentStreakDays,
                chartSummary.currentStreakDays
            )
        } else {
            activity.getString(R.string.chart_metric_streak_empty_short)
        }
        bestDayView.text = chartSummary.bestDay?.let {
            val locale = currentLocale()
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
            activity.getString(
                R.string.chart_best_day_value,
                it.date.format(formatter),
                formatDurationCompact(it.totalMs)
            )
        } ?: activity.getString(R.string.chart_best_day_empty)

        updateRangeButtons(days)
    }

    private fun bindRanking() {
        val ranking = rankingRepository.getRanking(currentRankingScope, currentRankingMetric)

        rankingMostTitle.text = activity.getString(R.string.ranking_most_title)
        rankingLeastTitle.text = activity.getString(R.string.ranking_least_title)
        rankingLeastItems = ranking.leastRanked

        renderRankingSection(
            container = rankingMostContainer,
            items = ranking.mostRanked,
            metric = currentRankingMetric
        )

        val hasAnyRanking = ranking.mostRanked.isNotEmpty() || ranking.leastRanked.isNotEmpty()
        rankingLeastItemCount = ranking.leastRanked.size
        rankingEmptyView.visibility = if (hasAnyRanking) View.GONE else View.VISIBLE
        sectionRankingLeastView.visibility = if (rankingLeastItemCount > 0) View.VISIBLE else View.GONE

        updateRankingControls()
        renderRankingSection(
            container = rankingLeastContainer,
            items = rankingLeastItems.take(1),
            metric = currentRankingMetric
        )
        rankingLeastContainer.visibility = if (rankingLeastItemCount > 0) View.VISIBLE else View.GONE
        rankingLeastPreviewRow.isEnabled = rankingLeastItemCount > 0
        rankingLeastPreviewRow.alpha = if (rankingLeastItemCount > 0) 1f else 0.6f
        rankingLeastPreviewText.text = activity.getString(R.string.ranking_least_preview_action)
    }

    private fun showLeastRankingDetailSheet() {
        if (rankingLeastItems.isEmpty()) return

        val dialog = BottomSheetDialog(activity)
        dialog.setContentView(R.layout.view_ranking_detail_sheet)
        dialog.findViewById<TextView>(R.id.tvRankingDetailTitle)?.text =
            activity.getString(R.string.ranking_least_detail_title)
        dialog.findViewById<TextView>(R.id.tvRankingDetailBody)?.text =
            activity.resources.getQuantityString(
                R.plurals.ranking_preview_count,
                rankingLeastItemCount,
                rankingLeastItemCount
            )
        dialog.findViewById<LinearLayout>(R.id.containerRankingDetail)?.let { container ->
            renderRankingSection(
                container = container,
                items = rankingLeastItems,
                metric = currentRankingMetric
            )
        }
        val behavior = dialog.behavior
        dialog.setOnShowListener {
            configureBehavior(dialog, behavior)
        }
        dialog.show()
    }

    private fun renderRankingSection(
        container: LinearLayout,
        items: List<KanjiStudyRankItem>,
        metric: RankingMetric,
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(activity)
        items.forEach { item ->
            val row = inflater.inflate(R.layout.item_kanji_ranking, container, false)
            row.findViewById<TextView>(R.id.tvRankingKanji).text = item.kanji
            row.findViewById<TextView>(R.id.tvRankingPrimary).text =
                item.meaning?.takeIf { it.isNotBlank() } ?: item.kanji
            row.findViewById<TextView>(R.id.tvRankingSecondary).text = buildSecondaryText(item)

            val metricValueView = row.findViewById<TextView>(R.id.tvRankingMetricCaption)
            metricValueView.text = if (metric == RankingMetric.STUDY_TIME) {
                activity.getString(
                    R.string.ranking_metric_time_value,
                    activity.formatDurationForUi(item.totalStudyMs)
                )
            } else {
                activity.resources.getQuantityString(
                    R.plurals.ranking_metric_open_value,
                    item.openCount.toInt(),
                    item.openCount
                )
            }

            row.setOnClickListener { activity.startActivity(buildDetailIntent(item)) }
            ThemeController.applyGlassDepth(row.findViewById(R.id.rankingItemRoot), elevatedDp = 8f)
            container.addView(row)
        }
    }

    private fun buildSecondaryText(item: KanjiStudyRankItem): String {
        val parts = mutableListOf<String>()
        item.jlptLevel?.takeIf { it.isNotBlank() }?.let {
            parts += activity.getString(R.string.jlpt_format, it)
        }
        item.lastActivityAt?.let {
            val nowZone = ZoneId.systemDefault()
            val lastDate = Instant.ofEpochMilli(it).atZone(nowZone).toLocalDate()
            val today = LocalDate.now(nowZone)
            val relativeText = if (lastDate == today) {
                activity.getString(R.string.ranking_last_activity_today)
            } else {
                DateUtils.getRelativeTimeSpanString(
                    it,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
            }
            parts += activity.getString(
                R.string.ranking_secondary_last_activity,
                relativeText
            )
        }
        return parts.joinToString(activity.getString(R.string.bullet_separator)).ifBlank {
            activity.getString(R.string.ranking_secondary_fallback)
        }
    }

    private fun buildDetailIntent(item: KanjiStudyRankItem): Intent {
        return KanjiDetailNavigator.buildDetailIntent(
            context = activity,
            kanji = item.kanji,
            meaningFallback = item.meaning,
            jlptFallback = item.jlptLevel,
        )
    }

    private fun updateRangeButtons(days: Int) {
        val selected = R.drawable.bg_chart_range_selected
        val idle = R.drawable.bg_chart_range_idle
        btnRange7.setBackgroundResource(if (days == 7) selected else idle)
        btnRange30.setBackgroundResource(if (days == 30) selected else idle)
        applySegmentButtonTextColors(btnRange7, isSelected = days == 7)
        applySegmentButtonTextColors(btnRange30, isSelected = days == 30)
    }

    private fun updateRankingControls() {
        val selected = R.drawable.bg_chart_range_selected
        val idle = R.drawable.bg_chart_range_idle

        btnRankingAll.setBackgroundResource(if (currentRankingScope == RankingScope.ALL_TIME) selected else idle)
        btnRanking30.setBackgroundResource(if (currentRankingScope == RankingScope.LAST_30_DAYS) selected else idle)
        btnRanking7.setBackgroundResource(if (currentRankingScope == RankingScope.LAST_7_DAYS) selected else idle)

        applySegmentButtonTextColors(btnRankingAll, isSelected = currentRankingScope == RankingScope.ALL_TIME)
        applySegmentButtonTextColors(btnRanking30, isSelected = currentRankingScope == RankingScope.LAST_30_DAYS)
        applySegmentButtonTextColors(btnRanking7, isSelected = currentRankingScope == RankingScope.LAST_7_DAYS)

        btnMetricTime.setBackgroundResource(if (currentRankingMetric == RankingMetric.STUDY_TIME) selected else idle)
        btnMetricOpen.setBackgroundResource(if (currentRankingMetric == RankingMetric.OPEN_COUNT) selected else idle)

        applySegmentButtonTextColors(btnMetricTime, isSelected = currentRankingMetric == RankingMetric.STUDY_TIME)
        applySegmentButtonTextColors(btnMetricOpen, isSelected = currentRankingMetric == RankingMetric.OPEN_COUNT)
    }

    private fun applySegmentButtonTextColors(button: Button, isSelected: Boolean) {
        val attr = if (isSelected) R.attr.colorPrimaryButtonText else R.attr.colorSecondaryButtonText
        button.setTextColor(ThemeController.resolveColor(activity, attr))
    }

    private fun currentLocale(): Locale {
        return activity.resources.configuration.locales[0]
    }

    private fun formatRelativeTime(timestampMs: Long?): String {
        if (timestampMs == null) {
            return activity.getString(R.string.ranking_secondary_fallback)
        }
        return DateUtils.getRelativeTimeSpanString(
            timestampMs,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    private fun formatDurationCompact(durationMs: Long): String {
        return DateUtils.formatElapsedTime(durationMs / 1000L)
    }

    private fun applyDepthStyling(dialog: BottomSheetDialog) {
        ThemeController.applyGlassDepth(dialog.findViewById(R.id.statsSheetRoot), elevatedDp = 20f)
        ThemeController.applyGlassDepth(dialog.findViewById(R.id.cardGuidance), elevatedDp = 12f)
        ThemeController.applyGlassDepth(dialog.findViewById(R.id.statsChartCard), elevatedDp = 12f)
        ThemeController.applyGlassDepth(dialog.findViewById(R.id.cardChartTotal), elevatedDp = 8f)
        ThemeController.applyGlassDepth(dialog.findViewById(R.id.cardChartActiveDays), elevatedDp = 8f)
        ThemeController.applyGlassDepth(dialog.findViewById(R.id.cardChartCurrentStreak), elevatedDp = 8f)
        ThemeController.applyGlassDepth(dialog.findViewById(R.id.statsSummaryCard), elevatedDp = 10f)
        ThemeController.applyGlassDepth(dialog.findViewById(R.id.statsRankingCard), elevatedDp = 10f)
        ThemeController.applyGlassDepth(btnRange7, elevatedDp = 0f)
        ThemeController.applyGlassDepth(btnRange30, elevatedDp = 0f)
        ThemeController.applyGlassDepth(btnRankingAll, elevatedDp = 0f)
        ThemeController.applyGlassDepth(btnRanking30, elevatedDp = 0f)
        ThemeController.applyGlassDepth(btnRanking7, elevatedDp = 0f)
        ThemeController.applyGlassDepth(btnMetricTime, elevatedDp = 0f)
        ThemeController.applyGlassDepth(btnMetricOpen, elevatedDp = 0f)
        ThemeController.applyGlassDepth(rankingLeastPreviewRow, elevatedDp = 0f)
    }
}
