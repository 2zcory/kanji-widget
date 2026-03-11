package com.example.kanjiwidget

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.kanjiwidget.home.HomeSummary
import com.example.kanjiwidget.home.RecentKanjiSummaryItem
import com.example.kanjiwidget.home.HomeSummaryRepository
import com.example.kanjiwidget.stats.StudyStatsBottomSheet
import com.example.kanjiwidget.stats.StudyStatsRepository
import com.example.kanjiwidget.widget.KanjiAppWidgetProvider
import com.example.kanjiwidget.widget.KanjiWidgetPrefs

class MainActivity : Activity() {
    private val widgetOpacityLevels = listOf(1.0f, 0.85f, 0.70f, 0.55f, 0.40f)
    private lateinit var repository: HomeSummaryRepository
    private lateinit var studyStatsRepository: StudyStatsRepository
    private lateinit var summaryCardTitle: TextView
    private lateinit var summaryCardSubtitle: TextView
    private lateinit var summaryMeta: TextView
    private lateinit var widgetStatus: TextView
    private lateinit var continueLearningBody: TextView
    private lateinit var openLatestButton: Button
    private lateinit var openRandomButton: Button
    private lateinit var statsButton: Button
    private lateinit var recentKanjiSection: View
    private lateinit var recentKanjiContainer: LinearLayout
    private lateinit var widgetControlsBody: TextView
    private lateinit var widgetOpacityValue: TextView
    private lateinit var widgetOpacityButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = HomeSummaryRepository(this)
        studyStatsRepository = StudyStatsRepository(this)
        summaryCardTitle = findViewById(R.id.tvHomeSummaryTitle)
        summaryCardSubtitle = findViewById(R.id.tvHomeSummarySubtitle)
        summaryMeta = findViewById(R.id.tvHomeSummaryMeta)
        widgetStatus = findViewById(R.id.tvWidgetStatus)
        continueLearningBody = findViewById(R.id.tvContinueLearningBody)
        openLatestButton = findViewById(R.id.btnOpenLatestKanji)
        openRandomButton = findViewById(R.id.btnOpenRandomKanji)
        statsButton = findViewById(R.id.btnTodayStats)
        recentKanjiSection = findViewById(R.id.sectionRecentKanji)
        recentKanjiContainer = findViewById(R.id.containerRecentKanji)
        widgetControlsBody = findViewById(R.id.tvWidgetControlsBody)
        widgetOpacityValue = findViewById(R.id.tvWidgetOpacityValue)
        widgetOpacityButton = findViewById(R.id.btnWidgetOpacity)

        findViewById<Button>(R.id.btnWidgetHelp).setOnClickListener {
            showWidgetHelpDialog()
        }
        widgetOpacityButton.setOnClickListener { cycleWidgetOpacity() }
    }

    override fun onResume() {
        super.onResume()
        bindSummary(repository.loadSummary())
    }

    private fun bindSummary(summary: HomeSummary) {
        summaryCardTitle.text = getString(R.string.home_today_summary_title)
        summaryCardSubtitle.text = getString(
            R.string.home_today_summary_value,
            formatDuration(summary.todayStudyMs),
            summary.todayOpenCount
        )
        summaryMeta.text = if (summary.todayStudyMs > 0L) {
            getString(R.string.home_today_summary_meta_active)
        } else {
            getString(R.string.home_today_summary_meta_empty)
        }
        widgetStatus.text = if (summary.isWidgetInstalled) {
            getString(R.string.home_widget_status_installed)
        } else {
            getString(R.string.home_widget_status_missing)
        }

        val hasLatest = !summary.latestKanji.isNullOrBlank()
        continueLearningBody.text = if (hasLatest) {
            getString(R.string.home_continue_body_with_latest)
        } else {
            getString(R.string.home_continue_body_empty)
        }
        openLatestButton.isEnabled = hasLatest
        openLatestButton.alpha = if (hasLatest) 1f else 0.5f
        if (hasLatest) {
            val latestIntent = buildLatestDetailIntent(summary)
            openLatestButton.setOnClickListener { startActivity(latestIntent) }
        } else {
            openLatestButton.setOnClickListener(null)
        }

        bindRandomAction(summary)
        bindRecentKanji(summary.recentKanji)
        statsButton.setOnClickListener { showStudyStatsBottomSheet(summary) }
        widgetControlsBody.text = if (summary.isWidgetInstalled) {
            getString(R.string.home_widget_controls_body_installed)
        } else {
            getString(R.string.home_widget_controls_body_missing)
        }
        widgetOpacityValue.text = getString(
            R.string.home_widget_opacity_value,
            (KanjiWidgetPrefs.getWidgetSurfaceAlpha(this) * 100).toInt()
        )
    }

    private fun buildLatestDetailIntent(summary: HomeSummary): Intent {
        val kanji = summary.latestKanji.orEmpty()
        val entry = KanjiWidgetPrefs.getRemoteEntry(this, kanji)
        return Intent(this, KanjiDetailActivity::class.java).apply {
            putExtra(KanjiDetailActivity.EXTRA_KANJI, kanji)
            putExtra(KanjiDetailActivity.EXTRA_SOURCE, entry?.source ?: getString(R.string.stroke_order_source_default))
            putExtra(KanjiDetailActivity.EXTRA_JLPT, entry?.jlptLevel)
            putExtra(KanjiDetailActivity.EXTRA_ONYOMI, entry?.onyomi)
            putExtra(KanjiDetailActivity.EXTRA_KUNYOMI, entry?.kunyomi)
            putExtra(KanjiDetailActivity.EXTRA_MEANING, entry?.meaningVi ?: summary.latestMeaning)
            putExtra(KanjiDetailActivity.EXTRA_NOTE, entry?.example)
        }
    }

    private fun showStudyStatsBottomSheet(summary: HomeSummary) {
        StudyStatsBottomSheet(
            activity = this,
            summary = summary,
            repository = studyStatsRepository,
        ).show()
    }

    private fun showWidgetHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.home_widget_help_title)
            .setMessage(getString(R.string.home_widget_help_dialog_message))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun cycleWidgetOpacity() {
        val current = KanjiWidgetPrefs.getWidgetSurfaceAlpha(this)
        val currentIndex = widgetOpacityLevels.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
            .takeIf { it >= 0 } ?: 0
        val next = widgetOpacityLevels[(currentIndex + 1) % widgetOpacityLevels.size]
        KanjiWidgetPrefs.setWidgetSurfaceAlpha(this, next)
        widgetOpacityValue.text = getString(R.string.home_widget_opacity_value, (next * 100).toInt())
        KanjiAppWidgetProvider.refreshAllWidgets(this)
    }

    fun formatDurationForUi(durationMs: Long): String {
        val totalSeconds = durationMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return if (minutes > 0L) {
            getString(R.string.study_duration_minutes_seconds, minutes, seconds)
        } else {
            getString(R.string.study_duration_seconds, seconds)
        }
    }

    private fun formatDuration(durationMs: Long): String = formatDurationForUi(durationMs)

    private fun bindRandomAction(summary: HomeSummary) {
        val catalog = KanjiWidgetPrefs.getKanjiCatalog(this)
        openRandomButton.isEnabled = catalog.isNotEmpty()
        openRandomButton.alpha = if (catalog.isNotEmpty()) 1f else 0.5f
        openRandomButton.text = if (catalog.isNotEmpty()) {
            getString(R.string.home_action_open_random)
        } else {
            getString(R.string.home_action_open_random_disabled)
        }
        openRandomButton.setOnClickListener(
            if (catalog.isEmpty()) {
                null
            } else {
                View.OnClickListener {
                    startActivity(buildRandomDetailIntent(catalog, summary.latestKanji))
                }
            }
        )
    }

    private fun bindRecentKanji(items: List<RecentKanjiSummaryItem>) {
        recentKanjiContainer.removeAllViews()
        recentKanjiSection.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        if (items.isEmpty()) return

        val inflater = LayoutInflater.from(this)
        items.forEach { item ->
            val row = inflater.inflate(R.layout.item_recent_kanji, recentKanjiContainer, false)
            row.findViewById<TextView>(R.id.tvRecentKanji).text = item.kanji
            row.findViewById<TextView>(R.id.tvRecentMeaning).text =
                item.meaning ?: getString(R.string.home_latest_meaning_placeholder)
            row.findViewById<TextView>(R.id.tvRecentMeta).text = buildRecentMeta(item)
            row.setOnClickListener { startActivity(buildDetailIntent(item)) }
            recentKanjiContainer.addView(row)
        }
    }

    private fun buildRandomDetailIntent(catalog: List<String>, latestKanji: String?): Intent {
        val selectedKanji = when {
            catalog.isEmpty() -> latestKanji.orEmpty()
            catalog.size == 1 -> catalog.first()
            else -> catalog
                .filterNot { it == latestKanji }
                .ifEmpty { catalog }
                .random()
        }
        return buildDetailIntent(
            RecentKanjiSummaryItem(
                kanji = selectedKanji,
                viewedAt = System.currentTimeMillis(),
                meaning = null,
                jlpt = null,
            )
        )
    }

    private fun buildDetailIntent(item: RecentKanjiSummaryItem): Intent {
        val entry = KanjiWidgetPrefs.getRemoteEntry(this, item.kanji)
        return Intent(this, KanjiDetailActivity::class.java).apply {
            putExtra(KanjiDetailActivity.EXTRA_KANJI, item.kanji)
            putExtra(KanjiDetailActivity.EXTRA_SOURCE, entry?.source ?: getString(R.string.stroke_order_source_default))
            putExtra(KanjiDetailActivity.EXTRA_JLPT, entry?.jlptLevel ?: item.jlpt)
            putExtra(KanjiDetailActivity.EXTRA_ONYOMI, entry?.onyomi)
            putExtra(KanjiDetailActivity.EXTRA_KUNYOMI, entry?.kunyomi)
            putExtra(KanjiDetailActivity.EXTRA_MEANING, entry?.meaningVi ?: item.meaning)
            putExtra(KanjiDetailActivity.EXTRA_NOTE, entry?.example)
        }
    }

    private fun buildRecentMeta(item: RecentKanjiSummaryItem): String {
        val parts = mutableListOf<String>()
        parts += DateUtils.getRelativeTimeSpanString(
            item.viewedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
        item.jlpt?.takeIf { it.isNotBlank() }?.let { parts += "JLPT $it" }
        return parts.joinToString(" • ")
    }
}
