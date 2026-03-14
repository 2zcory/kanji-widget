package com.example.kanjiwidget.theme

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.kanjiwidget.R
import com.example.kanjiwidget.widget.KanjiWidgetPrefs

object ThemeController {
    fun isGlassMode(context: Context): Boolean {
        return KanjiWidgetPrefs.getAppThemeMode(context) == AppThemeMode.GLASS
    }

    fun applyTheme(activity: AppCompatActivity) {
        val mode = KanjiWidgetPrefs.getAppThemeMode(activity)
        AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
        activity.setTheme(
            if (mode == AppThemeMode.GLASS) {
                R.style.Theme_KanjiWidget_Glass
            } else {
                R.style.Theme_KanjiWidget
            }
        )
    }

    fun updateThemeSelection(activity: Activity, mode: AppThemeMode): Boolean {
        val current = KanjiWidgetPrefs.getAppThemeMode(activity)
        if (current == mode) return false
        KanjiWidgetPrefs.setAppThemeMode(activity, mode)
        AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
        return true
    }

    fun animateScreenEntrance(activity: Activity) {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val root = content.getChildAt(0) ?: return
        root.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.theme_screen_enter))
    }

    fun applyGlassDepth(view: View?, elevatedDp: Float, defaultDp: Float = 0f) {
        if (view == null) return
        view.stateListAnimator = null
        view.translationZ = 0f
        val targetDp = if (isGlassMode(view.context)) elevatedDp else defaultDp
        view.elevation = targetDp * view.resources.displayMetrics.density
    }

    fun styleDialog(dialog: Dialog) {
        val window = dialog.window ?: return
        if (!isGlassMode(dialog.context)) return
        window.setBackgroundDrawableResource(R.drawable.bg_dialog_surface)
        window.decorView.elevation = 28f * dialog.context.resources.displayMetrics.density
        window.decorView.translationZ = 28f * dialog.context.resources.displayMetrics.density
        window.setDimAmount(0f)
    }

    fun styleCenteredOverlayDialog(dialog: Dialog) {
        val window = dialog.window ?: return
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (!isGlassMode(dialog.context)) return
        window.decorView.elevation = 0f
        window.decorView.translationZ = 0f
        window.setDimAmount(0.08f)
    }

    @ColorInt
    fun resolveColor(activity: Activity, @AttrRes attr: Int): Int {
        val typedValue = android.util.TypedValue()
        activity.theme.resolveAttribute(attr, typedValue, true)
        return if (typedValue.resourceId != 0) {
            ContextCompat.getColor(activity, typedValue.resourceId)
        } else {
            typedValue.data
        }
    }

    private fun AppThemeMode.toNightMode(): Int {
        return when (this) {
            AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppThemeMode.GLASS -> AppCompatDelegate.MODE_NIGHT_NO
        }
    }
}
