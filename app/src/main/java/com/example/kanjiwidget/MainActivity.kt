package com.example.kanjiwidget

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.example.kanjiwidget.home.HomeSummary
import com.example.kanjiwidget.home.HomeSummaryRepository
import com.example.kanjiwidget.home.RecentKanjiSummaryItem
import com.example.kanjiwidget.stats.StudyStatsBottomSheet
import com.example.kanjiwidget.stats.StudyStatsRepository
import com.example.kanjiwidget.theme.ThemeController
import com.example.kanjiwidget.widget.KanjiWidgetPrefs
import kotlin.math.roundToInt
import java.util.Locale

import androidx.lifecycle.lifecycleScope
import com.example.kanjiwidget.db.StudyStatsMigrator
import kotlinx.coroutines.launch

class MainActivity : ThemedActivity() {
    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        recreate()
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val changed = result.data?.getBooleanExtra(SettingsActivity.EXTRA_SETTINGS_CHANGED, false) == true
        if (result.resultCode == RESULT_OK && changed) {
            recreate()
        }
    }

    private lateinit var repository: HomeSummaryRepository
    private lateinit var studyStatsRepository: StudyStatsRepository
    private lateinit var heroLabel: TextView
    private lateinit var heroTitle: TextView
    private lateinit var heroBody: TextView
    private lateinit var heroMetaPrimary: TextView
    private lateinit var heroMetaSecondary: TextView
    private lateinit var widgetStatusBody: TextView
    private lateinit var widgetStatusMetaPrimary: TextView
    private lateinit var widgetStatusMetaSecondary: TextView
    private lateinit var primaryStudyActionButton: Button
    private lateinit var openRandomButton: View
    private lateinit var openRandomButtonLabel: TextView
    private lateinit var statsButton: View
    private lateinit var statsButtonLabel: TextView
    private lateinit var recentKanjiContainer: GridLayout
    private lateinit var recentEmptyState: View
    private lateinit var recentSectionLink: TextView
    private lateinit var openSettingsButton: ImageButton
    private lateinit var todayMetricValue: TextView
    private lateinit var streakMetricValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        applyPreparedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        runScreenEntranceAnimation()

        lifecycleScope.launch {
            StudyStatsMigrator(this@MainActivity).migrateIfNeeded()
        }

        repository = HomeSummaryRepository(this)
        studyStatsRepository = StudyStatsRepository(this)
        heroLabel = findViewById(R.id.tvHeroLabel)
        heroTitle = findViewById(R.id.tvHomeTitle)
        heroBody = findViewById(R.id.tvHomeHeroBody)
        heroMetaPrimary = findViewById(R.id.tvHeroMetaPrimary)
        heroMetaSecondary = findViewById(R.id.tvHeroMetaSecondary)
        widgetStatusBody = findViewById(R.id.tvHomeWidgetStatusBody)
        widgetStatusMetaPrimary = findViewById(R.id.tvWidgetStatusMetaPrimary)
        widgetStatusMetaSecondary = findViewById(R.id.tvWidgetStatusMetaSecondary)
        primaryStudyActionButton = findViewById(R.id.btnPrimaryStudyAction)
        openRandomButton = findViewById(R.id.btnOpenRandomKanji)
        openRandomButtonLabel = findViewById(R.id.tvOpenRandomKanjiLabel)
        statsButton = findViewById(R.id.btnTodayStats)
        statsButtonLabel = findViewById(R.id.tvTodayStatsLabel)
        recentKanjiContainer = findViewById(R.id.containerRecentKanji)
        recentEmptyState = findViewById(R.id.tvRecentEmpty)
        recentSectionLink = findViewById(R.id.tvRecentSectionLink)
        openSettingsButton = findViewById(R.id.btnOpenSettingsIcon)
        todayMetricValue = findViewById(R.id.tvTodayMetricValue)
        streakMetricValue = findViewById(R.id.tvStreakMetricValue)
        applyDepthStyling()

        openSettingsButton.setOnClickListener {
            settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
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
        val catalog = KanjiWidgetPrefs.getKanjiCatalog(this)
        bindHero(summary, catalog)
        bindPrimaryStudyAction(summary, catalog)
        bindRandomAction(summary, catalog)
        bindRecentKanji(summary.recentKanji)
        bindWidgetStatus(summary)
        bindStats(summary)
        statsButton.setOnClickListener { showStudyStatsBottomSheet(summary) }
    }

    private fun bindHero(summary: HomeSummary, catalog: List<String>) {
        val hasLatest = !summary.latestKanji.isNullOrBlank()
        val heroLabelText = when {
            hasLatest -> getString(R.string.home_hero_label_latest)
            summary.showWidgetHelp -> getString(R.string.home_hero_label_setup)
            catalog.isNotEmpty() -> getString(R.string.home_hero_label_random)
            else -> getString(R.string.home_hero_label_ready)
        }
        heroLabel.text = heroLabelText.uppercase(Locale.getDefault())
        heroTitle.text = when {
            hasLatest -> getString(R.string.home_hero_title_latest, summary.latestKanji)
            !summary.isWidgetInstalled -> getString(R.string.home_hero_title_no_widget)
            catalog.isNotEmpty() -> getString(R.string.home_hero_title_random)
            else -> getString(R.string.home_hero_title_empty)
        }
        heroBody.text = when {
            !summary.isWidgetInstalled -> getString(R.string.home_hero_body_no_widget)
            hasLatest -> getString(R.string.home_hero_body_with_latest)
            else -> getString(R.string.home_hero_body_no_latest)
        }

        val primaryMeta = when {
            summary.latestViewedAt != null -> DateUtils.getRelativeTimeSpanString(
                summary.latestViewedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
            !summary.isWidgetInstalled -> getString(R.string.home_meta_widget_missing)
            summary.todayStudyMs > 0L -> getString(R.string.home_meta_today_active)
            else -> getString(R.string.home_meta_recent_empty)
        }
        val secondaryMeta = when {
            hasLatest -> getString(R.string.home_meta_synced_today)
            summary.isWidgetInstalled -> getString(R.string.home_meta_widget_active)
            catalog.isNotEmpty() -> getString(R.string.home_meta_random_ready)
            else -> null
        }
        updatePill(heroMetaPrimary, primaryMeta)
        updatePill(heroMetaSecondary, secondaryMeta)
    }

    private fun bindPrimaryStudyAction(summary: HomeSummary, catalog: List<String>) {
        val hasLatest = !summary.latestKanji.isNullOrBlank()
        when {
            hasLatest -> {
                val latestIntent = buildLatestDetailIntent(summary)
                primaryStudyActionButton.text = getString(R.string.home_action_open_latest)
                primaryStudyActionButton.isEnabled = true
                primaryStudyActionButton.alpha = 1f
                primaryStudyActionButton.setOnClickListener { detailLauncher.launch(latestIntent) }
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
                    detailLauncher.launch(buildRandomDetailIntent(catalog, summary.latestKanji))
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

    private fun bindRandomAction(summary: HomeSummary, catalog: List<String>) {
        openRandomButton.isEnabled = catalog.isNotEmpty()
        openRandomButton.alpha = if (catalog.isNotEmpty()) 1f else 0.5f
        openRandomButtonLabel.text = if (catalog.isNotEmpty()) {
            getString(R.string.home_action_random_review)
        } else {
            getString(R.string.home_action_open_random_disabled)
        }
        openRandomButton.setOnClickListener(
            if (catalog.isEmpty()) {
                null
            } else {
                View.OnClickListener {
                    detailLauncher.launch(buildRandomDetailIntent(catalog, summary.latestKanji))
                }
            }
        )
        statsButtonLabel.text = getString(R.string.home_action_study_stats)
        statsButton.alpha = 1f
        statsButton.isEnabled = true
    }

    private fun bindRecentKanji(items: List<RecentKanjiSummaryItem>) {
        recentKanjiContainer.removeAllViews()
        val visibleItems = items.take(4)
        recentEmptyState.visibility = if (visibleItems.isEmpty()) View.VISIBLE else View.GONE
        recentKanjiContainer.visibility = if (visibleItems.isEmpty()) View.GONE else View.VISIBLE
        recentSectionLink.text = if (visibleItems.isEmpty()) {
            getString(R.string.home_recent_empty_link)
        } else {
            getString(R.string.home_recent_link)
        }
        if (visibleItems.isEmpty()) return

        val inflater = LayoutInflater.from(this)
        visibleItems.forEachIndexed { index, item ->
            val tile = inflater.inflate(R.layout.item_recent_kanji, recentKanjiContainer, false)
            val column = index % 2
            val row = index / 2
            tile.layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(row, 1f),
                GridLayout.spec(column, 1f)
            ).apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                setMargins(
                    if (column == 0) 0 else dp(6),
                    if (row == 0) 0 else dp(8),
                    if (column == 0) dp(6) else 0,
                    0
                )
            }
            tile.findViewById<TextView>(R.id.tvRecentKanji).text = item.kanji
            tile.findViewById<TextView>(R.id.tvRecentMeaning).text =
                item.meaning ?: getString(R.string.home_latest_meaning_placeholder)
            updatePill(
                tile.findViewById(R.id.tvRecentMetaPrimary),
                buildRecentPrimaryMeta(item)
            )
            updatePill(
                tile.findViewById(R.id.tvRecentJlpt),
                item.jlpt?.takeIf { it.isNotBlank() }?.let { getString(R.string.jlpt_format, it) }
            )
            tile.setOnClickListener { detailLauncher.launch(buildDetailIntent(item)) }
            ThemeController.applyMainCardDepth(tile.findViewById(R.id.recentKanjiItemRoot), elevatedDp = 6f, defaultDp = 4f)
            tile.alpha = 0f
            tile.translationY = 18f
            recentKanjiContainer.addView(tile)
            tile.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 45L)
                .setDuration(220L)
                .start()
        }
    }

    private fun bindWidgetStatus(summary: HomeSummary) {
        widgetStatusBody.text = if (summary.isWidgetInstalled) {
            getString(R.string.home_widget_status_body_installed)
        } else {
            getString(R.string.home_widget_status_body_missing)
        }
        updatePill(
            widgetStatusMetaPrimary,
            if (summary.isWidgetInstalled) {
                getString(R.string.home_widget_status_installed)
            } else {
                getString(R.string.home_widget_status_missing)
            }
        )
        updatePill(
            widgetStatusMetaSecondary,
            getString(
                R.string.home_widget_status_pill_opacity,
                (KanjiWidgetPrefs.getWidgetSurfaceAlpha(this) * 100).roundToInt()
            )
        )
    }

    private fun bindStats(summary: HomeSummary) {
        val chart = studyStatsRepository.getDailyChart(days = 14)
        todayMetricValue.text = summary.todayOpenCount.toString()
        streakMetricValue.text = getString(
            R.string.home_stats_streak_value,
            chart.currentStreakDays
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

    private fun buildRecentPrimaryMeta(item: RecentKanjiSummaryItem): String {
        return DateUtils.getRelativeTimeSpanString(
            item.viewedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    private fun applyDepthStyling() {
        ThemeController.applyMainHeroDepth(findViewById(R.id.sectionHero), elevatedDp = 10f, defaultDp = 4f)
        ThemeController.applyMainCardDepth(findViewById(R.id.sectionRecentKanji), elevatedDp = 6f, defaultDp = 3f)
        ThemeController.applyMainCardDepth(findViewById(R.id.sectionWidgetStatus), elevatedDp = 6f, defaultDp = 3f)
        ThemeController.applyMainCardDepth(findViewById(R.id.cardTodayMetric), elevatedDp = 4f, defaultDp = 2f)
        ThemeController.applyMainCardDepth(findViewById(R.id.cardStreakMetric), elevatedDp = 4f, defaultDp = 2f)
        ThemeController.applyMainCardDepth(primaryStudyActionButton, elevatedDp = 4f, defaultDp = 2f)
        ThemeController.applyMainCardDepth(openRandomButton, elevatedDp = 1.5f, defaultDp = 0.5f)
        ThemeController.applyMainCardDepth(statsButton, elevatedDp = 1.5f, defaultDp = 0.5f)
        ThemeController.applyMainCardDepth(openSettingsButton, elevatedDp = 1.5f, defaultDp = 0.5f)
    }

    private fun updatePill(view: TextView, text: CharSequence?) {
        if (text.isNullOrBlank()) {
            view.visibility = View.GONE
            return
        }
        view.text = text
        view.visibility = View.VISIBLE
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
}
