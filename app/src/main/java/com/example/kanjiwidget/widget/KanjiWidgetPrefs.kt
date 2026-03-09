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

    fun saveRemoteEntry(context: Context, kanji: String, entry: KanjiEntry) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = listOf(entry.kanji, entry.onyomi, entry.kunyomi, entry.meaningVi, entry.example, entry.jlptLevel)
            .joinToString("\u001F")
        sp.edit().putString("remote_$kanji", raw).apply()
    }

    fun getRemoteEntry(context: Context, kanji: String): KanjiEntry? {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString("remote_$kanji", null) ?: return null
        val parts = raw.split("\u001F")
        if (parts.size < 6) return null
        return KanjiEntry(
            kanji = parts[0],
            onyomi = parts[1],
            kunyomi = parts[2],
            meaningVi = parts[3],
            example = parts[4],
            jlptLevel = parts[5]
        )
    }
}
