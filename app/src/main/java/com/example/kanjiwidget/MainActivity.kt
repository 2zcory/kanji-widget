package com.example.kanjiwidget

import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.kanjiwidget.home.HomeSummary
import com.example.kanjiwidget.home.RecentKanjiSummaryItem
import com.example.kanjiwidget.home.HomeSummaryRepository
import com.example.kanjiwidget.stats.StudyStatsBottomSheet
import com.example.kanjiwidget.stats.StudyStatsRepository
import com.example.kanjiwidget.theme.AppThemeMode
import com.example.kanjiwidget.theme.ThemeController
import com.example.kanjiwidget.widget.KanjiAppWidgetProvider
import com.example.kanjiwidget.widget.KanjiWidgetPrefs

class MainActivity : ThemedActivity() {
    private val widgetOpacityLevels = listOf(1.0f, 0.85f, 0.70f, 0.55f, 0.40f)
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
    private lateinit var widgetControlsBody: TextView
    private lateinit var widgetOpacityValue: TextView
    private lateinit var widgetOpacityButton: Button
    private lateinit var themeValue: TextView
    private lateinit var themeButton: Button
    private lateinit var languageValue: TextView
    private lateinit var languageButton: Button

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
        widgetControlsBody = findViewById(R.id.tvWidgetControlsBody)
        widgetOpacityValue = findViewById(R.id.tvWidgetOpacityValue)
        widgetOpacityButton = findViewById(R.id.btnWidgetOpacity)
        themeValue = findViewById(R.id.tvThemeValue)
        themeButton = findViewById(R.id.btnTheme)
        languageValue = findViewById(R.id.tvLanguageValue)
        languageButton = findViewById(R.id.btnLanguage)
        applyDepthStyling()

        findViewById<Button>(R.id.btnWidgetHelp).setOnClickListener {
            showWidgetHelpDialog()
        }
        widgetOpacityButton.setOnClickListener { cycleWidgetOpacity() }
        themeButton.setOnClickListener { showThemeDialog() }
        languageButton.setOnClickListener { showLanguageDialog() }
    }

    override fun onResume() {
        super.onResume()
        bindSummary(repository.loadSummary())
        updateThemeSummary()
        updateLanguageSummary()
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

    private fun showLanguageDialog() {
        val options = arrayOf(
            getString(R.string.language_option_system),
            getString(R.string.language_option_english),
            getString(R.string.language_option_vietnamese),
        )
        val currentIndex = resolveLanguageOptionIndex()
        AlertDialog.Builder(this)
            .setTitle(R.string.language_dialog_title)
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                applyLanguageSelection(which)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showThemeDialog() {
        val modes = AppThemeMode.entries.toTypedArray()
        val options = arrayOf(
            getString(R.string.theme_option_system),
            getString(R.string.theme_option_light),
            getString(R.string.theme_option_dark),
            getString(R.string.theme_option_glass),
        )
        val currentIndex = modes.indexOf(KanjiWidgetPrefs.getAppThemeMode(this))
        AlertDialog.Builder(this)
            .setTitle(R.string.theme_dialog_title)
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val didChange = ThemeController.updateThemeSelection(this, modes[which])
                dialog.dismiss()
                if (didChange) {
                    updateThemeSummary()
                    recreate()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateThemeSummary() {
        themeValue.text = when (KanjiWidgetPrefs.getAppThemeMode(this)) {
            AppThemeMode.LIGHT -> getString(R.string.theme_option_light)
            AppThemeMode.DARK -> getString(R.string.theme_option_dark)
            AppThemeMode.GLASS -> getString(R.string.theme_option_glass)
            AppThemeMode.SYSTEM -> getString(R.string.theme_option_system)
        }
    }

    private fun updateLanguageSummary() {
        val option = resolveLanguageOptionIndex()
        languageValue.text = when (option) {
            1 -> getString(R.string.language_option_english)
            2 -> getString(R.string.language_option_vietnamese)
            else -> getString(R.string.language_option_system)
        }
    }

    private fun resolveLanguageOptionIndex(): Int {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return 0
        val tag = locales.toLanguageTags()
        return when {
            tag.startsWith("en") -> 1
            tag.startsWith("vi") -> 2
            else -> 0
        }
    }

    private fun applyLanguageSelection(optionIndex: Int) {
        val locales = when (optionIndex) {
            1 -> LocaleListCompat.forLanguageTags("en")
            2 -> LocaleListCompat.forLanguageTags("vi")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
        KanjiAppWidgetProvider.refreshAllWidgets(this)
        updateLanguageSummary()
        recreate()
    }

    private fun applyDepthStyling() {
        ThemeController.applyGlassDepth(findViewById(R.id.sectionHero), elevatedDp = 18f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionContinueLearning), elevatedDp = 12f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionRecentKanji), elevatedDp = 12f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionWidgetControls), elevatedDp = 12f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionTheme), elevatedDp = 12f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionLanguage), elevatedDp = 12f)
        ThemeController.applyGlassDepth(primaryStudyActionButton, elevatedDp = 10f)
        ThemeController.applyGlassDepth(openRandomButton, elevatedDp = 8f)
        ThemeController.applyGlassDepth(statsButton, elevatedDp = 8f)
        ThemeController.applyGlassDepth(widgetOpacityButton, elevatedDp = 8f)
        ThemeController.applyGlassDepth(themeButton, elevatedDp = 8f)
        ThemeController.applyGlassDepth(languageButton, elevatedDp = 8f)
        ThemeController.applyGlassDepth(findViewById(R.id.btnWidgetHelp), elevatedDp = 8f)
    }
}
