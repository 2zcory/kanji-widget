package com.example.kanjiwidget

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.kanjiwidget.home.HomeSummary
import com.example.kanjiwidget.home.HomeSummaryRepository
import com.example.kanjiwidget.stats.StudyStatsBottomSheet
import com.example.kanjiwidget.stats.StudyStatsRepository
import com.example.kanjiwidget.widget.KanjiWidgetPrefs

class MainActivity : Activity() {
    private lateinit var repository: HomeSummaryRepository
    private lateinit var studyStatsRepository: StudyStatsRepository
    private lateinit var summaryCardTitle: TextView
    private lateinit var summaryCardSubtitle: TextView
    private lateinit var summaryMeta: TextView
    private lateinit var widgetStatus: TextView
    private lateinit var latestCard: View
    private lateinit var latestKanji: TextView
    private lateinit var latestMeaning: TextView
    private lateinit var latestMeta: TextView
    private lateinit var openLatestButton: Button
    private lateinit var statsButton: Button
    private lateinit var widgetHelpSection: View
    private lateinit var widgetHelpBody: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = HomeSummaryRepository(this)
        studyStatsRepository = StudyStatsRepository(this)
        summaryCardTitle = findViewById(R.id.tvHomeSummaryTitle)
        summaryCardSubtitle = findViewById(R.id.tvHomeSummarySubtitle)
        summaryMeta = findViewById(R.id.tvHomeSummaryMeta)
        widgetStatus = findViewById(R.id.tvWidgetStatus)
        latestCard = findViewById(R.id.cardLatestKanji)
        latestKanji = findViewById(R.id.tvLatestKanji)
        latestMeaning = findViewById(R.id.tvLatestMeaning)
        latestMeta = findViewById(R.id.tvLatestMeta)
        openLatestButton = findViewById(R.id.btnOpenLatestKanji)
        statsButton = findViewById(R.id.btnTodayStats)
        widgetHelpSection = findViewById(R.id.sectionWidgetHelp)
        widgetHelpBody = findViewById(R.id.tvWidgetHelpBody)

        findViewById<Button>(R.id.btnWidgetHelp).setOnClickListener {
            showWidgetHelpDialog()
        }
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
        latestCard.visibility = if (hasLatest) View.VISIBLE else View.GONE
        openLatestButton.visibility = if (hasLatest) View.VISIBLE else View.GONE

        if (hasLatest) {
            latestKanji.text = summary.latestKanji
            latestMeaning.text = summary.latestMeaning ?: getString(R.string.home_latest_meaning_placeholder)
            latestMeta.text = buildLatestMeta(summary)

            val latestIntent = buildLatestDetailIntent(summary)
            latestCard.setOnClickListener { startActivity(latestIntent) }
            openLatestButton.setOnClickListener { startActivity(latestIntent) }
        } else {
            latestCard.setOnClickListener(null)
            openLatestButton.setOnClickListener(null)
        }

        statsButton.setOnClickListener { showStudyStatsBottomSheet(summary) }
        widgetHelpSection.visibility = if (summary.showWidgetHelp) View.VISIBLE else View.GONE
        widgetHelpBody.text = getString(R.string.home_widget_help_body)
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

    private fun buildLatestMeta(summary: HomeSummary): String {
        val parts = mutableListOf<String>()
        summary.latestJlpt?.takeIf { it.isNotBlank() }?.let { parts += "JLPT $it" }
        summary.latestViewedAt?.let {
            parts += DateUtils.getRelativeTimeSpanString(
                it,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        }
        return parts.joinToString(" • ").ifBlank { getString(R.string.home_latest_meta_fallback) }
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
}
