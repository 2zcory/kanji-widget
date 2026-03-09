package com.example.kanjiwidget.widget

import android.content.Context

object KanjiWidgetPrefs {
    private const val PREF = "kanji_widget_pref"

    fun getIndex(context: Context, widgetId: Int): Int {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return sp.getInt("idx_$widgetId", 0)
    }

    fun setIndex(context: Context, widgetId: Int, value: Int) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().putInt("idx_$widgetId", value).apply()
    }
}
