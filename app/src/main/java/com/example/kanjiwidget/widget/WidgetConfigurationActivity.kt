package com.example.kanjiwidget.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.example.kanjiwidget.R
import com.example.kanjiwidget.ThemedActivity
import com.example.kanjiwidget.theme.ThemeController

class WidgetConfigurationActivity : ThemedActivity() {
    private lateinit var titleView: TextView
    private lateinit var bodyView: TextView
    private lateinit var selectionValueView: TextView
    private lateinit var presetsGroup: RadioGroup
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private val opacityLevels = listOf(1.0f, 0.85f, 0.70f, 0.55f, 0.40f)

    override fun onCreate(savedInstanceState: Bundle?) {
        prepareTheme(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.activity_widget_configuration)
        runScreenEntranceAnimation()

        titleView = findViewById(R.id.tvWidgetConfigTitle)
        bodyView = findViewById(R.id.tvWidgetConfigBody)
        selectionValueView = findViewById(R.id.tvWidgetConfigSelectionValue)
        presetsGroup = findViewById(R.id.groupWidgetOpacityPresets)
        saveButton = findViewById(R.id.btnWidgetConfigSave)
        cancelButton = findViewById(R.id.btnWidgetConfigCancel)
        applyDepthStyling()

        titleView.text = getString(R.string.widget_config_title)
        bodyView.text = getString(R.string.widget_config_body)

        val defaultOpacity = KanjiWidgetPrefs.getWidgetSurfaceAlpha(this)
        val selectedOpacity = KanjiWidgetPrefs.getWidgetSurfaceAlpha(this, appWidgetId)
        val selectedIndex = opacityLevels.indexOfFirst { kotlin.math.abs(it - selectedOpacity) < 0.01f }
            .takeIf { it >= 0 } ?: opacityLevels.indexOfFirst { kotlin.math.abs(it - defaultOpacity) < 0.01f }
            .takeIf { it >= 0 } ?: 0

        populatePresetOptions(selectedIndex)
        updateSelectedOpacityLabel(opacityLevels[selectedIndex])

        presetsGroup.setOnCheckedChangeListener { _, checkedId ->
            val checkedButton = findViewById<RadioButton>(checkedId) ?: return@setOnCheckedChangeListener
            val opacity = checkedButton.tag as? Float ?: return@setOnCheckedChangeListener
            updateSelectedOpacityLabel(opacity)
        }

        saveButton.setOnClickListener { completeConfiguration() }
        cancelButton.setOnClickListener { finish() }
    }

    private fun populatePresetOptions(selectedIndex: Int) {
        presetsGroup.removeAllViews()
        opacityLevels.forEachIndexed { index, level ->
            val button = RadioButton(this).apply {
                id = android.view.View.generateViewId()
                text = getString(R.string.widget_config_opacity_option, (level * 100).toInt())
                tag = level
                textSize = 16f
                setTextColor(ThemeController.resolveColor(this@WidgetConfigurationActivity, R.attr.colorTextPrimary))
                isChecked = index == selectedIndex
            }
            presetsGroup.addView(button)
        }
    }

    private fun updateSelectedOpacityLabel(opacity: Float) {
        selectionValueView.text = getString(R.string.widget_config_selection_value, (opacity * 100).toInt())
    }

    private fun completeConfiguration() {
        val checkedButton = findViewById<RadioButton>(presetsGroup.checkedRadioButtonId)
        val selectedOpacity = checkedButton?.tag as? Float ?: KanjiWidgetPrefs.getWidgetSurfaceAlpha(this)
        KanjiWidgetPrefs.setWidgetSurfaceAlpha(this, appWidgetId, selectedOpacity)

        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultIntent)

        val manager = AppWidgetManager.getInstance(this)
        KanjiAppWidgetProvider().onUpdate(this, manager, intArrayOf(appWidgetId))
        finish()
    }

    private fun applyDepthStyling() {
        ThemeController.applyGlassDepth(findViewById(R.id.sectionWidgetConfigHero), elevatedDp = 18f)
        ThemeController.applyGlassDepth(findViewById(R.id.sectionWidgetConfigOptions), elevatedDp = 12f)
        ThemeController.applyGlassDepth(saveButton, elevatedDp = 10f)
        ThemeController.applyGlassDepth(cancelButton, elevatedDp = 8f)
    }
}
