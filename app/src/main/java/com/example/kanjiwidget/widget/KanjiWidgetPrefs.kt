package com.example.kanjiwidget.widget

import android.content.Context
import org.json.JSONObject

object KanjiWidgetPrefs {
    private const val PREF = "kanji_widget_pref"
    private const val LEGACY_DELIMITER = "\u001F"

    fun getKanjiCatalog(context: Context): List<String> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString("kanji_catalog", null) ?: return emptyList()
        return raw.split(LEGACY_DELIMITER).filter { it.isNotBlank() }
    }

    fun setKanjiCatalog(context: Context, kanji: List<String>) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val payload = kanji.joinToString(LEGACY_DELIMITER)
        sp.edit().putString("kanji_catalog", payload).apply()
    }

    fun getDeck(context: Context, widgetId: Int): List<Int> {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = sp.getString("deck_$widgetId", null) ?: return emptyList()
        return raw.split(',').mapNotNull { it.toIntOrNull() }
    }

    fun setDeck(context: Context, widgetId: Int, deck: List<Int>) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val payload = deck.joinToString(",")
        sp.edit().putString("deck_$widgetId", payload).apply()
    }

    fun getDeckPos(context: Context, widgetId: Int): Int {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return sp.getInt("deck_pos_$widgetId", 0)
    }

    fun setDeckPos(context: Context, widgetId: Int, value: Int) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().putInt("deck_pos_$widgetId", value).apply()
    }

    fun getIndex(context: Context, widgetId: Int): Int {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return sp.getInt("idx_$widgetId", 0)
    }

    fun setIndex(context: Context, widgetId: Int, value: Int) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().putInt("idx_$widgetId", value).apply()
    }

    fun getCurrentKanji(context: Context, widgetId: Int): String? {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return sp.getString("current_kanji_$widgetId", null)?.takeIf { it.isNotBlank() }
    }

    fun setCurrentKanji(context: Context, widgetId: Int, value: String) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().putString("current_kanji_$widgetId", value).apply()
    }

    fun getRevealAnswer(context: Context, widgetId: Int): Boolean {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return sp.getBoolean("reveal_$widgetId", false)
    }

    fun setRevealAnswer(context: Context, widgetId: Int, value: Boolean) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().putBoolean("reveal_$widgetId", value).apply()
    }

    fun clearWidgetState(context: Context, widgetId: Int) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit()
            .remove("deck_$widgetId")
            .remove("deck_pos_$widgetId")
            .remove("idx_$widgetId")
            .remove("current_kanji_$widgetId")
            .remove("reveal_$widgetId")
            .apply()
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
