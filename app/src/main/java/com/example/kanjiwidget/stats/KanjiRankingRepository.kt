package com.example.kanjiwidget.stats

import android.content.Context
import com.example.kanjiwidget.widget.KanjiWidgetPrefs
import com.example.kanjiwidget.widget.resolveDisplayMeaning
import java.time.LocalDate
import java.time.ZoneId

class KanjiRankingRepository(private val context: Context) {
    private val prefs by lazy {
        context.getSharedPreferences(STUDY_PREF, Context.MODE_PRIVATE)
    }

    fun getRanking(
        scope: RankingScope,
        metric: RankingMetric = RankingMetric.STUDY_TIME,
        limit: Int = 10
    ): KanjiStudyRanking {
        return buildRankingFromEntries(
            context = context,
            entries = prefs.all,
            scope = scope,
            metric = metric,
            limit = limit,
            today = LocalDate.now(ZoneId.systemDefault()),
        ) { kanji ->
            KanjiWidgetPrefs.getRemoteEntry(context, kanji)
        }
    }

    internal class Aggregate {
        var totalStudyMs: Long = 0L
        var openCount: Long = 0L
        var lastActivityDate: LocalDate? = null
    }

    companion object {
        private const val STUDY_PREF = "kanji_study_stats"
    }
}

internal fun buildRankingFromEntries(
    context: Context? = null,
    entries: Map<String, *>,
    scope: RankingScope,
    metric: RankingMetric,
    limit: Int,
    today: LocalDate,
    metadataProvider: (String) -> com.example.kanjiwidget.widget.KanjiEntry?,
): KanjiStudyRanking {
    require(limit > 0) { "limit must be positive" }

    val dateRange = scope.toDateRange(today)
    val aggregateByKanji = linkedMapOf<String, KanjiRankingRepository.Aggregate>()

    entries.forEach { (key, value) ->
        val match = KANJI_DATA_KEY.matchEntire(key) ?: return@forEach
        val type = match.groupValues[1] // "kanji" or "open_kanji"
        val date = runCatching { LocalDate.parse(match.groupValues[2]) }.getOrNull() ?: return@forEach
        if (date !in dateRange) return@forEach

        val kanji = match.groupValues[3]
        val numericValue = (value as? Number)?.toLong()?.takeIf { it > 0L } ?: return@forEach
        
        val aggregate = aggregateByKanji.getOrPut(kanji) { KanjiRankingRepository.Aggregate() }
        if (type == "kanji") {
            aggregate.totalStudyMs += numericValue
        } else if (type == "open_kanji") {
            aggregate.openCount += numericValue
        }
        aggregate.lastActivityDate = maxOf(aggregate.lastActivityDate ?: date, date)
    }

    val items = aggregateByKanji.entries
        .asSequence()
        .mapNotNull { (kanji, aggregate) ->
            val primaryValue = if (metric == RankingMetric.STUDY_TIME) {
                aggregate.totalStudyMs
            } else {
                aggregate.openCount
            }
            
            if (primaryValue <= 0L) return@mapNotNull null
            
            val entry = metadataProvider(kanji)
            KanjiStudyRankItem(
                kanji = kanji,
                totalStudyMs = aggregate.totalStudyMs,
                openCount = aggregate.openCount,
                lastActivityAt = aggregate.lastActivityDate
                    ?.atStartOfDay(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli(),
                meaning = if (context != null) resolveDisplayMeaning(context, entry) else entry?.meaning,
                jlptLevel = entry?.jlptLevel,
            )
        }
        .toList()

    val primarySelector: (KanjiStudyRankItem) -> Long = {
        if (metric == RankingMetric.STUDY_TIME) it.totalStudyMs else it.openCount
    }

    return KanjiStudyRanking(
        scope = scope,
        metric = metric,
        mostRanked = items
            .sortedWith(compareByDescending<KanjiStudyRankItem> { primarySelector(it) }
                .thenByDescending { it.lastActivityAt ?: Long.MIN_VALUE }
                .thenBy { it.kanji })
            .take(limit),
        leastRanked = items
            .sortedWith(compareBy<KanjiStudyRankItem> { primarySelector(it) }
                .thenBy { it.lastActivityAt ?: Long.MAX_VALUE }
                .thenBy { it.kanji })
            .take(limit),
    )
}

private val KANJI_DATA_KEY = Regex("""study_(kanji|open_kanji)_([0-9]{4}-[0-9]{2}-[0-9]{2})_(.+)""")

data class KanjiStudyRankItem(
    val kanji: String,
    val totalStudyMs: Long,
    val openCount: Long,
    val lastActivityAt: Long?,
    val meaning: String?,
    val jlptLevel: String?,
)

data class KanjiStudyRanking(
    val scope: RankingScope,
    val metric: RankingMetric,
    val mostRanked: List<KanjiStudyRankItem>,
    val leastRanked: List<KanjiStudyRankItem>,
)

enum class RankingScope {
    ALL_TIME,
    LAST_30_DAYS,
    LAST_7_DAYS,
}

enum class RankingMetric {
    STUDY_TIME,
    OPEN_COUNT,
}

private fun RankingScope.toDateRange(today: LocalDate): ClosedRange<LocalDate> {
    return when (this) {
        RankingScope.ALL_TIME -> LocalDate.of(1970, 1, 1)..today
        RankingScope.LAST_30_DAYS -> today.minusDays(29)..today
        RankingScope.LAST_7_DAYS -> today.minusDays(6)..today
    }
}
