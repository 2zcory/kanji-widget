package com.example.kanjiwidget

import android.app.Dialog
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.kanjiwidget.theme.ThemeController
import com.example.kanjiwidget.widget.KanjiAppWidgetProvider

object SettingsDialogs {
    fun showWidgetHelpDialog(activity: AppCompatActivity) {
        val dialog = createOverlayDialog(activity, R.layout.dialog_widget_help)
        dialog.findViewById<View>(R.id.dialogWidgetHelpOverlay).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.dialogWidgetHelpRoot).setOnClickListener { }
        dialog.findViewById<Button>(R.id.btnWidgetHelpClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
        ThemeController.applyGlassDepth(dialog.findViewById(R.id.dialogWidgetHelpRoot), elevatedDp = 30f)
        ThemeController.applyGlassDepth(dialog.findViewById(R.id.btnWidgetHelpClose), elevatedDp = 0f)
    }

    fun showLanguageDialog(activity: AppCompatActivity, onApplied: (() -> Unit)? = null) {
        val options = listOf(
            0 to activity.getString(R.string.language_option_system),
            1 to activity.getString(R.string.language_option_english),
            2 to activity.getString(R.string.language_option_vietnamese),
        )
        val currentIndex = resolveLanguageOptionIndex()
        val dialog = createOverlayDialog(activity, R.layout.dialog_language_picker)
        val group = dialog.findViewById<RadioGroup>(R.id.groupLanguageOptions)
        val cancelButton = dialog.findViewById<Button>(R.id.btnLanguageDialogCancel)
        val applyButton = dialog.findViewById<Button>(R.id.btnLanguageDialogApply)

        options.forEach { (index, label) ->
            val option = RadioButton(activity).apply {
                id = View.generateViewId()
                text = label
                tag = index
                textSize = 18f
                setTextColor(ThemeController.resolveColor(activity, R.attr.colorTextPrimary))
                buttonTintList = ColorStateList.valueOf(
                    ThemeController.resolveColor(activity, R.attr.colorAccentMain)
                )
                setPadding(0, 10, 0, 10)
                isChecked = index == currentIndex
            }
            group.addView(option)
        }

        dialog.findViewById<View>(R.id.dialogLanguageOverlay).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.dialogLanguageRoot).setOnClickListener { }
        cancelButton.setOnClickListener { dialog.dismiss() }
        applyButton.setOnClickListener {
            val selected = dialog.findViewById<RadioButton>(group.checkedRadioButtonId)
            val selectedIndex = selected?.tag as? Int ?: currentIndex
            val didChange = applyLanguageSelection(activity, selectedIndex)
            dialog.dismiss()
            if (didChange) onApplied?.invoke()
        }

        dialog.show()
        ThemeController.applyGlassDepth(dialog.findViewById(R.id.dialogLanguageRoot), elevatedDp = 30f)
        ThemeController.applyGlassDepth(applyButton, elevatedDp = 0f)
        ThemeController.applyGlassDepth(cancelButton, elevatedDp = 0f)
    }

    fun currentLanguageLabel(activity: AppCompatActivity): String {
        return when (resolveLanguageOptionIndex()) {
            1 -> activity.getString(R.string.language_option_english)
            2 -> activity.getString(R.string.language_option_vietnamese)
            else -> activity.getString(R.string.language_option_system)
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

    private fun applyLanguageSelection(activity: AppCompatActivity, optionIndex: Int): Boolean {
        val locales = when (optionIndex) {
            1 -> LocaleListCompat.forLanguageTags("en")
            2 -> LocaleListCompat.forLanguageTags("vi")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        if (AppCompatDelegate.getApplicationLocales() == locales) return false
        AppCompatDelegate.setApplicationLocales(locales)
        KanjiAppWidgetProvider.refreshAllWidgets(activity)
        activity.recreate()
        return true
    }

    private fun createOverlayDialog(activity: AppCompatActivity, layoutRes: Int): Dialog {
        return Dialog(activity).apply {
            setContentView(layoutRes)
            setCanceledOnTouchOutside(true)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            window?.setGravity(Gravity.CENTER)
            ThemeController.styleCenteredOverlayDialog(this)
        }
    }
}
