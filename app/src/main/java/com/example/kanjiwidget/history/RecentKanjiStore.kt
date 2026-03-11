package com.example.kanjiwidget.history

import android.content.Context

object RecentKanjiStore {
    private const val PREF = "kanji_recent_history"
    private const val KEY_LATEST_KANJI = "latest_kanji"
    private const val KEY_LATEST_VIEWED_AT = "latest_kanji_viewed_at"
    private const val KEY_RECENT_HISTORY = "recent_kanji_history_v2"
    private const val ENTRY_SEPARATOR = "\n"
    private const val FIELD_SEPARATOR = "\t"
    private const val MAX_RECENT_ITEMS = 10

    fun recordViewedKanji(context: Context, kanji: String) {
        val normalizedKanji = kanji.trim()
        if (normalizedKanji.isBlank()) return

        val viewedAt = System.currentTimeMillis()
        val updatedHistory = buildList {
            add(RecentKanjiHistoryItem(normalizedKanji, viewedAt))
            addAll(
                getRecentKanji(context)
                    .asSequence()
                    .filterNot { it.kanji == normalizedKanji }
                    .take(MAX_RECENT_ITEMS - 1)
                    .toList()
            )
        }

        prefs(context).edit()
            .putString(KEY_LATEST_KANJI, normalizedKanji)
            .putLong(KEY_LATEST_VIEWED_AT, viewedAt)
            .putString(KEY_RECENT_HISTORY, encodeHistory(updatedHistory))
            .apply()
    }

    fun getLatestKanji(context: Context): String? {
        return prefs(context).getString(KEY_LATEST_KANJI, null)?.takeIf { it.isNotBlank() }
    }

    fun getLatestViewedAt(context: Context): Long? {
        return prefs(context).getLong(KEY_LATEST_VIEWED_AT, 0L).takeIf { it > 0L }
    }

    fun getRecentKanji(context: Context, limit: Int = MAX_RECENT_ITEMS): List<RecentKanjiHistoryItem> {
        require(limit > 0) { "limit must be positive" }

        val storedHistory = decodeHistory(prefs(context).getString(KEY_RECENT_HISTORY, null))
            .distinctBy { it.kanji }
            .take(limit)

        if (storedHistory.isNotEmpty()) {
            return storedHistory
        }

        val latestKanji = getLatestKanji(context) ?: return emptyList()
        val latestViewedAt = getLatestViewedAt(context) ?: return emptyList()
        return listOf(RecentKanjiHistoryItem(latestKanji, latestViewedAt))
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun encodeHistory(items: List<RecentKanjiHistoryItem>): String {
        return items.joinToString(ENTRY_SEPARATOR) { item ->
            "${item.kanji}${FIELD_SEPARATOR}${item.viewedAt}"
        }
    }

    private fun decodeHistory(raw: String?): List<RecentKanjiHistoryItem> {
        if (raw.isNullOrBlank()) return emptyList()

        return raw.split(ENTRY_SEPARATOR)
            .asSequence()
            .mapNotNull { line ->
                val parts = line.split(FIELD_SEPARATOR, limit = 2)
                if (parts.size != 2) return@mapNotNull null

                val kanji = parts[0].trim()
                val viewedAt = parts[1].toLongOrNull()
                if (kanji.isBlank() || viewedAt == null || viewedAt <= 0L) {
                    return@mapNotNull null
                }

                RecentKanjiHistoryItem(kanji = kanji, viewedAt = viewedAt)
            }
            .toList()
    }
}

data class RecentKanjiHistoryItem(
    val kanji: String,
    val viewedAt: Long,
)
