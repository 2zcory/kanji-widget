package com.example.kanjiwidget

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
import com.example.kanjiwidget.theme.ThemeController
import com.example.kanjiwidget.widget.KanjiWidgetPrefs

class MainActivity : ThemedActivity() {
    private lateinit var repository: HomeSummaryRepository
    private lateinit var studyStatsRepository: StudyStatsRepository
    private lateinit var homeTitle: TextView
    private lateinit var summaryCardTitle: TextView
    private lateinit var summaryCardSubtitle: TextView
    private lateinit var summaryMeta: TextView
    private lateinit var heroBody: TextView
    private lateinit var widgetStatus: TextView
    private lateinit var continueLearningBody: TextView
    private lateinit var primaryStudyActionButton: Button
    private lateinit var openRandomButton: Button
    private lateinit var statsButton: Button
    private lateinit var recentKanjiSection: View
    private lateinit var recentKanjiContainer: LinearLayout
    private lateinit var openSettingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        prepareTheme(savedInstanceState)
        setContentView(R.layout.activity_main)
        runScreenEntranceAnimation()

        repository = HomeSummaryRepository(this)
        studyStatsRepository = StudyStatsRepository(this)
        homeTitle = findViewById(R.id.tvHomeTitle)
        summaryCardTitle = findViewById(R.id.tvHomeSummaryTitle)
        summaryCardSubtitle = findViewById(R.id.tvHomeSummarySubtitle)
        summaryMeta = findViewById(R.id.tvHomeSummaryMeta)
        heroBody = findViewById(R.id.tvHomeHeroBody)
        widgetStatus = findViewById(R.id.tvWidgetStatus)
        continueLearningBody = findViewById(R.id.tvContinueLearningBody)
        primaryStudyActionButton = findViewById(R.id.btnPrimaryStudyAction)
        openRandomButton = findViewById(R.id.btnOpenRandomKanji)
        statsButton = findViewById(R.id.btnTodayStats)
        recentKanjiSection = findViewById(R.id.sectionRecentKanji)
        recentKanjiContainer = findViewById(R.id.containerRecentKanji)
        openSettingsButton = findViewById(R.id.btnOpenSettings)
        applyDepthStyling()

        openSettingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        bindSummary(repository.loadSummary())
        repository.backfillVietnameseMeaningsIfNeeded {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                bindSummary(repository.loadSummary())
            }
        }
    }

    private fun bindSummary(summary: HomeSummary) {
        homeTitle.text = getString(R.string.home_title)
        summaryCardTitle.text = getString(R.string.home_today_summary_title)
        val openCountText = resources.getQuantityString(
            R.plurals.open_count,
            summary.todayOpenCount,
            summary.todayOpenCount
        )
        summaryCardSubtitle.text = getString(
            R.string.home_today_summary_value,
            formatDuration(summary.todayStudyMs),
            openCountText
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
        heroBody.text = when {
            !summary.isWidgetInstalled -> getString(R.string.home_hero_body_no_widget)
            hasLatest -> getString(R.string.home_hero_body_with_latest)
            else -> getString(R.string.home_hero_body_no_latest)
        }
        continueLearningBody.text = if (hasLatest) {
            getString(R.string.home_continue_body_with_latest)
        } else {
            getString(R.string.home_continue_body_empty)
        }

        bindPrimaryStudyAction(summary)
        bindRandomAction(summary)
        bindRecentKanji(summary.recentKanji)
        statsButton.setOnClickListener { showStudyStatsBottomSheet(summary) }
    }

    private fun bindPrimaryStudyAction(summary: HomeSummary) {
        val catalog = KanjiWidgetPrefs.getKanjiCatalog(this)
        val hasLatest = !summary.latestKanji.isNullOrBlank()
        when {
            hasLatest -> {
                val latestIntent = buildLatestDetailIntent(summary)
                primaryStudyActionButton.text = getString(R.string.home_action_open_latest)
                primaryStudyActionButton.isEnabled = true
                primaryStudyActionButton.alpha = 1f
                primaryStudyActionButton.setOnClickListener { startActivity(latestIntent) }
            }

            summary.showWidgetHelp -> {
                primaryStudyActionButton.text = getString(R.string.home_action_widget_help)
                primaryStudyActionButton.isEnabled = true
                primaryStudyActionButton.alpha = 1f
                primaryStudyActionButton.setOnClickListener { showWidgetHelpDialog() }
            }

            catalog.isNotEmpty() -> {
                primaryStudyActionButton.text = getString(R.string.home_action_open_random)
                primaryStudyActionButton.isEnabled = true
                primaryStudyActionButton.alpha = 1f
                primaryStudyActionButton.setOnClickListener {
                    startActivity(buildRandomDetailIntent(catalog, summary.latestKanji))
                }
            }

            else -> {
                primaryStudyActionButton.text = getString(R.string.home_action_open_random_disabled)
                primaryStudyActionButton.isEnabled = false
                primaryStudyActionButton.alpha = 0.5f
                primaryStudyActionButton.setOnClickListener(null)
            }
        }
    }

    private fun buildLatestDetailIntent(summary: HomeSummary): Intent {
        val kanji = summary.latestKanji.orEmpty()
        return KanjiDetailNavigator.buildDetailIntent(
            context = this,
            kanji = kanji,
            meaningFallback = summary.latestMeaning,
            jlptFallback = summary.latestJlpt,
        )
    }

    private fun showStudyStatsBottomSheet(summary: HomeSummary) {
        StudyStatsBottomSheet(
            activity = this,
            summary = summary,
            repository = studyStatsRepository,
        ).show()
    }

    private fun showWidgetHelpDialog() {
        SettingsDialogs.showWidgetHelpDialog(this)
    }

    fun formatDurationForUi(durationMs: Long): String {
        val totalSeconds = durationMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val minutesText = resources.getQuantityString(
            R.plurals.duration_minutes,
            minutes.toInt(),
            minutes
        )
        val secondsText = resources.getQuantityString(
            R.plurals.duration_seconds,
            seconds.toInt(),
            seconds
        )
        return if (minutes > 0L) {
            getString(R.string.duration_minutes_seconds, minutesText, secondsText)
        } else {
            secondsText
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
        items.forEachIndexed { index, item ->
            val row = inflater.inflate(R.layout.item_recent_kanji, recentKanjiContainer, false)
            row.findViewById<TextView>(R.id.tvRecentLabel).visibility =
                if (index == 0) View.VISIBLE else View.GONE
            row.findViewById<TextView>(R.id.tvRecentKanji).text = item.kanji
            row.findViewById<TextView>(R.id.tvRecentMeaning).text =
                item.meaning ?: getString(R.string.home_latest_meaning_placeholder)
            row.findViewById<TextView>(R.id.tvRecentMeta).text = buildRecentMeta(item)
            row.setOnClickListener { startActivity(buildDetailIntent(item)) }
            ThemeController.applyGlassDepth(row.findViewById(R.id.recentKanjiItemRoot), elevatedDp = 8f)
            row.alpha = 0f
            row.translationY = 20f
            recentKanjiContainer.addView(row)
            row.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 45L)
                .setDuration(220L)
                .start()
        }
    }

    private fun buildRandomDetailIntent(catalog: List<String>, latestKanji: String?): Intent {
        return KanjiDetailNavigator.buildRandomDetailIntent(
            context = this,
            catalog = catalog,
            currentKanji = latestKanji,
        ) ?: KanjiDetailNavigator.buildDetailIntent(
            context = this,
            kanji = latestKanji.orEmpty(),
        )
    }

    private fun buildDetailIntent(item: RecentKanjiSummaryItem): Intent {
        return KanjiDetailNavigator.buildDetailIntent(
            context = this,
            kanji = item.kanji,
            meaningFallback = item.meaning,
            jlptFallback = item.jlpt,
        )
    }

    private fun buildRecentMeta(item: RecentKanjiSummaryItem): String {
        val parts = mutableListOf<String>()
        parts += DateUtils.getRelativeTimeSpanString(
            item.viewedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
        item.jlpt?.takeIf { it.isNotBlank() }?.let { parts += getString(R.string.jlpt_format, it) }
        return parts.joinToString(getString(R.string.bullet_separator))
    }

    private fun applyDepthStyling() {
        ThemeController.applyGlassDepth(findViewById(R.id.sectionHero), elevatedDp = 24f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionContinueLearning), elevatedDp = 7f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionRecentKanji), elevatedDp = 7f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionSettings), elevatedDp = 7f)
        ThemeController.applyGlassDepth(primaryStudyActionButton, elevatedDp = 0f)
        ThemeController.applyGlassDepth(openRandomButton, elevatedDp = 0f)
        ThemeController.applyGlassDepth(statsButton, elevatedDp = 0f)
        ThemeController.applyGlassDepth(openSettingsButton, elevatedDp = 0f)
    }
}
