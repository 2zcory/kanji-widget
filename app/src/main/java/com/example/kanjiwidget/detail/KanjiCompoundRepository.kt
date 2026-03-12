package com.example.kanjiwidget.detail

import android.content.Context
import com.example.kanjiwidget.widget.KanjiApiClient
import com.example.kanjiwidget.widget.KanjiWidgetPrefs

class KanjiCompoundRepository(
    private val context: Context,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
) {
    fun getCachedCompounds(kanji: String): List<KanjiCompoundEntry> {
        return KanjiWidgetPrefs.getCachedCompounds(context, kanji)?.entries.orEmpty()
    }

    fun shouldRefreshCompounds(kanji: String): Boolean {
        val cached = KanjiWidgetPrefs.getCachedCompounds(context, kanji) ?: return true
        return nowProvider() - cached.lastUpdatedEpochMs > CACHE_FRESHNESS_MS
    }

    fun refreshCompounds(kanji: String): List<KanjiCompoundEntry>? {
        val fetched = KanjiApiClient.fetchKanjiCompounds(kanji) ?: return null
        KanjiWidgetPrefs.saveCompoundEntries(
            context = context,
            kanji = kanji,
            entries = fetched,
            savedAtEpochMs = nowProvider(),
        )
        return fetched
    }

    companion object {
        const val MAX_COMPOUNDS = 5
        const val CACHE_FRESHNESS_MS = 7L * 24L * 60L * 60L * 1000L
    }
}

internal data class RawKanjiCompound(
    val written: String,
    val reading: String,
    val meaning: String,
    val priorities: List<String>,
)

internal fun selectDisplayCompounds(
    kanji: String,
    candidates: List<RawKanjiCompound>,
    limit: Int = KanjiCompoundRepository.MAX_COMPOUNDS,
): List<KanjiCompoundEntry> {
    return candidates
        .filter { candidate ->
            candidate.written.isNotBlank() &&
                candidate.meaning.isNotBlank() &&
                candidate.written.contains(kanji) &&
                candidate.written.length in 2..8
        }
        .sortedWith(
            compareByDescending<RawKanjiCompound> { compoundScore(it) }
                .thenBy { it.written.length }
                .thenBy { it.reading.length }
        )
        .distinctBy { "${it.written}|${it.reading}" }
        .take(limit)
        .map { raw ->
            KanjiCompoundEntry(
                written = raw.written,
                reading = raw.reading,
                meaning = raw.meaning,
                usageHint = deriveUsageHint(raw.priorities, raw.meaning),
                priorities = raw.priorities,
            )
        }
}

internal fun deriveUsageHint(priorities: List<String>, meaning: String): String {
    val lowerMeaning = meaning.lowercase()
    return when {
        priorities.any { it.startsWith("news") } -> "News-heavy"
        priorities.any { it.startsWith("ichi") || it.startsWith("nf") } -> "Common word"
        lowerMeaning.contains("abbr") || lowerMeaning.contains("former") || lowerMeaning.contains("historical") ->
            "Reference term"
        else -> "Study word"
    }
}

private fun compoundScore(compound: RawKanjiCompound): Int {
    val lengthPenalty = (compound.written.length - 4).coerceAtLeast(0) * 5
    val priorityScore = compound.priorities.fold(0) { total, priority ->
        total + when {
            priority.startsWith("news") -> 70
            priority.startsWith("ichi") -> 50
            priority.startsWith("nf") -> 40
            else -> 10
        }
    }
    val meaningPenalty = if (compound.meaning.lowercase().contains("abbr")) 10 else 0
    return priorityScore - lengthPenalty - meaningPenalty
}
