package com.example.kanjiwidget.widget

import android.content.Context
import org.json.JSONObject

object KanjiWidgetPrefs {
    private const val PREF = "kanji_widget_pref"
    private const val LEGACY_DELIMITER = "\u001F"

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
        val payload = JSONObject().apply {
            put("kanji", entry.kanji)
            put("onyomi", entry.onyomi)
            put("kunyomi", entry.kunyomi)
            put("meaningVi", entry.meaningVi)
            put("example", entry.example)
            put("jlptLevel", entry.jlptLevel)
            put("source", entry.source ?: "kanjiapi.dev")
            put("lastUpdatedEpochMs", entry.lastUpdatedEpochMs ?: System.currentTimeMillis())
        }.toString()
        sp.edit().putString("remote_$kanji", payload).apply()
    }

    fun getRemoteEntry(context: Context, kanji: String): KanjiEntry? {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString("remote_$kanji", null) ?: return null

        return if (raw.trimStart().startsWith("{")) {
            parseJson(raw)
        } else {
            parseLegacy(raw)
        }
    }

    private fun parseJson(raw: String): KanjiEntry? {
        return try {
            val json = JSONObject(raw)
            val kanji = json.optString("kanji")
            val onyomi = json.optString("onyomi")
            val kunyomi = json.optString("kunyomi")
            val meaningVi = json.optString("meaningVi")
            val example = json.optString("example")
            val jlptLevel = json.optString("jlptLevel")
            if (kanji.isBlank() || jlptLevel.isBlank()) return null
            KanjiEntry(
                kanji = kanji,
                onyomi = onyomi,
                kunyomi = kunyomi,
                meaningVi = meaningVi,
                example = example,
                jlptLevel = jlptLevel,
                source = json.optString("source").ifBlank { null },
                lastUpdatedEpochMs = json.optLong("lastUpdatedEpochMs").takeIf { it > 0 }
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLegacy(raw: String): KanjiEntry? {
        val parts = raw.split(LEGACY_DELIMITER)
        if (parts.size < 6) return null
        return KanjiEntry(
            kanji = parts[0],
            onyomi = parts[1],
            kunyomi = parts[2],
            meaningVi = parts[3],
            example = parts[4],
            jlptLevel = parts[5],
            source = "legacy-cache",
            lastUpdatedEpochMs = null
        )
    }
}
