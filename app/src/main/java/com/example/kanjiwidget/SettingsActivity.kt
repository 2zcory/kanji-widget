package com.example.kanjiwidget

import android.widget.Button
import android.widget.TextView
import com.example.kanjiwidget.home.HomeSummaryRepository
import com.example.kanjiwidget.theme.ThemeController
import com.example.kanjiwidget.widget.KanjiAppWidgetProvider
import com.example.kanjiwidget.widget.KanjiWidgetPrefs

class SettingsActivity : ThemedActivity() {
    private val widgetOpacityLevels = listOf(1.0f, 0.85f, 0.70f, 0.55f, 0.40f)
    private lateinit var repository: HomeSummaryRepository
    private lateinit var widgetControlsBody: TextView
    private lateinit var widgetOpacityValue: TextView
    private lateinit var themeValue: TextView
    private lateinit var languageValue: TextView

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        prepareTheme(savedInstanceState)
        setContentView(R.layout.activity_settings)
        runScreenEntranceAnimation()

        repository = HomeSummaryRepository(this)
        widgetControlsBody = findViewById(R.id.tvSettingsWidgetControlsBody)
        widgetOpacityValue = findViewById(R.id.tvSettingsWidgetOpacityValue)
        themeValue = findViewById(R.id.tvSettingsThemeValue)
        languageValue = findViewById(R.id.tvSettingsLanguageValue)

        findViewById<Button>(R.id.btnSettingsBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSettingsWidgetOpacity).setOnClickListener { cycleWidgetOpacity() }
        findViewById<Button>(R.id.btnSettingsWidgetHelp).setOnClickListener {
            SettingsDialogs.showWidgetHelpDialog(this)
        }
        findViewById<Button>(R.id.btnSettingsTheme).setOnClickListener {
            SettingsDialogs.showThemeDialog(this)
        }
        findViewById<Button>(R.id.btnSettingsLanguage).setOnClickListener {
            SettingsDialogs.showLanguageDialog(this)
        }

        applyDepthStyling()
    }

    override fun onResume() {
        super.onResume()
        bindSettingsSummary()
    }

    private fun bindSettingsSummary() {
        val summary = repository.loadSummary()
        widgetControlsBody.text = if (summary.isWidgetInstalled) {
            getString(R.string.home_widget_controls_body_installed)
        } else {
            getString(R.string.home_widget_controls_body_missing)
        }
        widgetOpacityValue.text = getString(
            R.string.home_widget_opacity_value,
            (KanjiWidgetPrefs.getWidgetSurfaceAlpha(this) * 100).toInt()
        )
        themeValue.text = SettingsDialogs.currentThemeLabel(this)
        languageValue.text = SettingsDialogs.currentLanguageLabel(this)
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

    private fun applyDepthStyling() {
        ThemeController.applyGlassDepth(findViewById(R.id.sectionSettingsHero), elevatedDp = 24f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionSettingsWidgetControls), elevatedDp = 7f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionSettingsTheme), elevatedDp = 7f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionSettingsLanguage), elevatedDp = 7f)
        ThemeController.applyGlassDepth(findViewById(R.id.btnSettingsBack), elevatedDp = 0f)
        ThemeController.applyGlassDepth(findViewById(R.id.btnSettingsWidgetOpacity), elevatedDp = 0f)
        ThemeController.applyGlassDepth(findViewById(R.id.btnSettingsWidgetHelp), elevatedDp = 0f)
        ThemeController.applyGlassDepth(findViewById(R.id.btnSettingsTheme), elevatedDp = 0f)
        ThemeController.applyGlassDepth(findViewById(R.id.btnSettingsLanguage), elevatedDp = 0f)
    }
}
