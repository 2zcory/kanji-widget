package com.example.kanjiwidget

import android.content.Intent
import android.widget.SeekBar
import android.widget.TextView
import com.example.kanjiwidget.home.HomeSummaryRepository
import com.example.kanjiwidget.theme.AppThemeMode
import com.example.kanjiwidget.theme.ThemeController
import com.example.kanjiwidget.widget.KanjiAppWidgetProvider
import com.example.kanjiwidget.widget.KanjiWidgetPrefs
import kotlin.math.abs
import kotlin.math.roundToInt

class SettingsActivity : ThemedActivity() {
    private var hasAppliedChanges = false
    private lateinit var repository: HomeSummaryRepository
    private lateinit var widgetControlsBody: TextView
    private lateinit var widgetOpacityValue: TextView
    private lateinit var widgetOpacitySlider: SeekBar
    private lateinit var languageValue: TextView
    private lateinit var themeTiles: Map<AppThemeMode, ThemeTile>

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        applyPreparedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        runScreenEntranceAnimation()
        hasAppliedChanges = savedInstanceState?.getBoolean(EXTRA_SETTINGS_CHANGED) ?: false

        repository = HomeSummaryRepository(this)
        widgetControlsBody = findViewById(R.id.tvSettingsWidgetControlsBody)
        widgetOpacityValue = findViewById(R.id.tvSettingsWidgetOpacityValue)
        widgetOpacitySlider = findViewById(R.id.seekSettingsWidgetOpacity)
        languageValue = findViewById(R.id.tvSettingsLanguageValue)
        themeTiles = mapOf(
            AppThemeMode.SYSTEM to ThemeTile(
                root = findViewById(R.id.tileThemeSystem),
                title = findViewById(R.id.tvThemeSystemTitle),
                body = findViewById(R.id.tvThemeSystemBody),
            ),
            AppThemeMode.LIGHT to ThemeTile(
                root = findViewById(R.id.tileThemeLight),
                title = findViewById(R.id.tvThemeLightTitle),
                body = findViewById(R.id.tvThemeLightBody),
            ),
            AppThemeMode.DARK to ThemeTile(
                root = findViewById(R.id.tileThemeDark),
                title = findViewById(R.id.tvThemeDarkTitle),
                body = findViewById(R.id.tvThemeDarkBody),
            ),
            AppThemeMode.GLASS to ThemeTile(
                root = findViewById(R.id.tileThemeGlass),
                title = findViewById(R.id.tvThemeGlassTitle),
                body = findViewById(R.id.tvThemeGlassBody),
            ),
        )

        findViewById<android.view.View>(R.id.btnSettingsBack).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.rowSettingsWidgetHelp).setOnClickListener {
            SettingsDialogs.showWidgetHelpDialog(this)
        }
        findViewById<android.view.View>(R.id.rowSettingsLanguage).setOnClickListener {
            SettingsDialogs.showLanguageDialog(this) { markSettingsChanged() }
        }
        themeTiles.forEach { (mode, tile) ->
            tile.root.setOnClickListener { applyThemeSelection(mode) }
        }
        bindWidgetOpacitySlider()

        applyDepthStyling()
    }

    override fun onResume() {
        super.onResume()
        bindSettingsSummary()
    }

    private fun bindSettingsSummary() {
        val summary = repository.loadSummary()
        widgetControlsBody.text = if (summary.isWidgetInstalled) {
            getString(R.string.settings_widget_controls_body_installed)
        } else {
            getString(R.string.settings_widget_controls_body_missing)
        }
        val currentOpacity = KanjiWidgetPrefs.getWidgetSurfaceAlpha(this)
        widgetOpacityValue.text = getString(
            R.string.home_widget_opacity_value,
            currentOpacity.toDisplayPercent()
        )
        val targetProgress = alphaToSliderProgress(currentOpacity)
        if (widgetOpacitySlider.progress != targetProgress) {
            widgetOpacitySlider.progress = targetProgress
        }
        val currentTheme = KanjiWidgetPrefs.getAppThemeMode(this)
        languageValue.text = SettingsDialogs.currentLanguageLabel(this)
        bindThemeSelection(currentTheme)
    }

    private fun bindWidgetOpacitySlider() {
        widgetOpacitySlider.max = WIDGET_ALPHA_MAX_PERCENT - WIDGET_ALPHA_MIN_PERCENT
        widgetOpacitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                widgetOpacityValue.text = getString(
                    R.string.home_widget_opacity_value,
                    sliderProgressToAlpha(progress).toDisplayPercent()
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                applyWidgetOpacity(sliderProgressToAlpha(seekBar?.progress ?: 0))
            }
        })
    }

    private fun applyWidgetOpacity(next: Float) {
        val current = KanjiWidgetPrefs.getWidgetSurfaceAlpha(this)
        if (abs(current - next) < 0.01f) return
        KanjiWidgetPrefs.setWidgetSurfaceAlpha(this, next)
        markSettingsChanged()
        widgetOpacityValue.text = getString(R.string.home_widget_opacity_value, next.toDisplayPercent())
        KanjiAppWidgetProvider.refreshAllWidgets(this)
    }

    private fun applyThemeSelection(mode: AppThemeMode) {
        val didChange = ThemeController.updateThemeSelection(this, mode)
        if (!didChange) return
        markSettingsChanged()
        recreate()
    }

    private fun bindThemeSelection(currentMode: AppThemeMode) {
        themeTiles.forEach { (mode, tile) ->
            val isSelected = mode == currentMode
            tile.root.setBackgroundResource(
                if (isSelected) R.drawable.bg_settings_tile_selected else R.drawable.bg_settings_tile
            )
            val titleColor = ThemeController.resolveColor(
                this,
                if (isSelected) R.attr.colorPrimaryButtonText else R.attr.colorTextPrimary
            )
            val bodyColor = ThemeController.resolveColor(
                this,
                if (isSelected) R.attr.colorPrimaryButtonText else R.attr.colorTextMuted
            )
            tile.title.setTextColor(titleColor)
            tile.body.setTextColor(bodyColor)
            tile.root.alpha = if (isSelected) 1f else 0.98f
        }
    }

    private fun applyDepthStyling() {
        ThemeController.applyGlassDepth(findViewById(R.id.btnSettingsBack), elevatedDp = 0f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionSettingsTheme), elevatedDp = 8f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionSettingsUtilityStack), elevatedDp = 8f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionSettingsWidgetControls), elevatedDp = 3f)
        themeTiles.values.forEach { ThemeController.applyGlassDepth(it.root, elevatedDp = 0f) }
        ThemeController.applyGlassDepth(findViewById(R.id.rowSettingsLanguage), elevatedDp = 0f)
        ThemeController.applyGlassDepth(findViewById(R.id.rowSettingsWidgetHelp), elevatedDp = 0f)
    }

    override fun onSaveInstanceState(outState: android.os.Bundle) {
        outState.putBoolean(EXTRA_SETTINGS_CHANGED, hasAppliedChanges)
        super.onSaveInstanceState(outState)
    }

    override fun finish() {
        if (hasAppliedChanges) {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_SETTINGS_CHANGED, true))
        }
        super.finish()
    }

    private fun markSettingsChanged() {
        hasAppliedChanges = true
        setResult(RESULT_OK, Intent().putExtra(EXTRA_SETTINGS_CHANGED, true))
    }

    companion object {
        const val EXTRA_SETTINGS_CHANGED = "extra_settings_changed"
        private const val WIDGET_ALPHA_MIN_PERCENT = 40
        private const val WIDGET_ALPHA_MAX_PERCENT = 100
    }

    private fun alphaToSliderProgress(alpha: Float): Int {
        val clampedPercent = alpha.toDisplayPercent().coerceIn(WIDGET_ALPHA_MIN_PERCENT, WIDGET_ALPHA_MAX_PERCENT)
        return clampedPercent - WIDGET_ALPHA_MIN_PERCENT
    }

    private fun sliderProgressToAlpha(progress: Int): Float {
        val percent = (WIDGET_ALPHA_MIN_PERCENT + progress).coerceIn(
            WIDGET_ALPHA_MIN_PERCENT,
            WIDGET_ALPHA_MAX_PERCENT,
        )
        return percent / 100f
    }

    private fun Float.toDisplayPercent(): Int = (this * 100f).roundToInt()

    private data class ThemeTile(
        val root: android.view.View,
        val title: TextView,
        val body: TextView,
    )
}
