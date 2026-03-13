package com.example.kanjiwidget.stats

import android.content.Context
import com.example.kanjiwidget.widget.KanjiWidgetPrefs
import java.time.LocalDate
import java.time.ZoneId

class KanjiRankingRepository(private val context: Context) {
    private val prefs by lazy {
        context.getSharedPreferences(STUDY_PREF, Context.MODE_PRIVATE)
    }

    fun getRanking(scope: RankingScope, limit: Int = 10): KanjiStudyRanking {
        return buildRankingFromEntries(
            entries = prefs.all,
            scope = scope,
            limit = limit,
            today = LocalDate.now(ZoneId.systemDefault()),
        ) { kanji ->
            KanjiWidgetPrefs.getRemoteEntry(context, kanji)
        }
    }

    internal class Aggregate {
        var totalStudyMs: Long = 0L
        var lastStudiedDate: LocalDate? = null
    }

    companion object {
        private const val STUDY_PREF = "kanji_study_stats"
    }
}

internal fun buildRankingFromEntries(
    entries: Map<String, *>,
    scope: RankingScope,
    limit: Int,
    today: LocalDate,
    metadataProvider: (String) -> com.example.kanjiwidget.widget.KanjiEntry?,
): KanjiStudyRanking {
    require(limit > 0) { "limit must be positive" }

    val dateRange = scope.toDateRange(today)
    val aggregateByKanji = linkedMapOf<String, KanjiRankingRepository.Aggregate>()

    entries.forEach { (key, value) ->
        val match = KANJI_KEY.matchEntire(key) ?: return@forEach
        val date = runCatching { LocalDate.parse(match.groupValues[1]) }.getOrNull() ?: return@forEach
        if (date !in dateRange) return@forEach

        val kanji = match.groupValues[2]
        val totalMs = (value as? Long)?.takeIf { it > 0L } ?: return@forEach
        val aggregate = aggregateByKanji.getOrPut(kanji) { KanjiRankingRepository.Aggregate() }
        aggregate.totalStudyMs += totalMs
        aggregate.lastStudiedDate = maxOf(aggregate.lastStudiedDate ?: date, date)
    }

    val items = aggregateByKanji.entries
        .asSequence()
        .mapNotNull { (kanji, aggregate) ->
            if (aggregate.totalStudyMs <= 0L) return@mapNotNull null
            val entry = metadataProvider(kanji)
            KanjiStudyRankItem(
                kanji = kanji,
                totalStudyMs = aggregate.totalStudyMs,
                lastStudiedAt = aggregate.lastStudiedDate
                    ?.atStartOfDay(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli(),
                meaning = entry?.meaningVi,
                jlptLevel = entry?.jlptLevel,
            )
        }
        .toList()

    return KanjiStudyRanking(
        scope = scope,
        mostStudied = items
            .sortedWith(compareByDescending<KanjiStudyRankItem> { it.totalStudyMs }
                .thenByDescending { it.lastStudiedAt ?: Long.MIN_VALUE }
                .thenBy { it.kanji })
            .take(limit),
        leastStudied = items
            .sortedWith(compareBy<KanjiStudyRankItem> { it.totalStudyMs }
                .thenBy { it.lastStudiedAt ?: Long.MAX_VALUE }
                .thenBy { it.kanji })
            .take(limit),
    )
}

private val KANJI_KEY = Regex("""study_kanji_([0-9]{4}-[0-9]{2}-[0-9]{2})_(.+)""")

data class KanjiStudyRankItem(
    val kanji: String,
    val totalStudyMs: Long,
    val lastStudiedAt: Long?,
    val meaning: String?,
    val jlptLevel: String?,
)

data class KanjiStudyRanking(
    val scope: RankingScope,
    val mostStudied: List<KanjiStudyRankItem>,
    val leastStudied: List<KanjiStudyRankItem>,
)

enum class RankingScope {
    ALL_TIME,
    LAST_30_DAYS,
}

private fun RankingScope.toDateRange(today: LocalDate): ClosedRange<LocalDate> {
    return when (this) {
        RankingScope.ALL_TIME -> LocalDate.of(1970, 1, 1)..today
        RankingScope.LAST_30_DAYS -> today.minusDays(29)..today
    }
}
