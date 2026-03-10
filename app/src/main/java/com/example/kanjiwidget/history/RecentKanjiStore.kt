package com.example.kanjiwidget.history

import android.content.Context

object RecentKanjiStore {
    private const val PREF = "kanji_recent_history"
    private const val KEY_LATEST_KANJI = "latest_kanji"
    private const val KEY_LATEST_VIEWED_AT = "latest_kanji_viewed_at"

    fun recordViewedKanji(context: Context, kanji: String) {
        val normalizedKanji = kanji.trim()
        if (normalizedKanji.isBlank()) return

        prefs(context).edit()
            .putString(KEY_LATEST_KANJI, normalizedKanji)
            .putLong(KEY_LATEST_VIEWED_AT, System.currentTimeMillis())
            .apply()
    }

    fun getLatestKanji(context: Context): String? {
        return prefs(context).getString(KEY_LATEST_KANJI, null)?.takeIf { it.isNotBlank() }
    }

    fun getLatestViewedAt(context: Context): Long? {
        return prefs(context).getLong(KEY_LATEST_VIEWED_AT, 0L).takeIf { it > 0L }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
