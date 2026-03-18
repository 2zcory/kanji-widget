package com.example.kanjiwidget.stats

import android.content.Context
import com.example.kanjiwidget.db.AppDatabase
import com.example.kanjiwidget.db.KanjiRankingResult
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId

class KanjiRankingRepository(private val context: Context) {
    private val db by lazy { AppDatabase.getInstance(context) }

    fun getRanking(
        scope: RankingScope,
        metric: RankingMetric = RankingMetric.STUDY_TIME,
        limit: Int = 10
    ): KanjiStudyRanking = runBlocking {
        val today = LocalDate.now(ZoneId.systemDefault())
        val dateRange = scope.toDateRange(today)
        
        val results = db.dailyKanjiStudyDao().getRanking(
            startDate = dateRange.start.toString(),
            endDate = dateRange.endInclusive.toString()
        )

        buildRankingFromResults(
            context = context,
            results = results,
            scope = scope,
            metric = metric,
            limit = limit
        ) { kanji ->
            com.example.kanjiwidget.widget.KanjiWidgetPrefs.getRemoteEntry(context, kanji)
        }
    }
}

internal fun buildRankingFromResults(
    context: Context? = null,
    results: List<KanjiRankingResult>,
    scope: RankingScope,
    metric: RankingMetric,
    limit: Int,
    metadataProvider: (String) -> com.example.kanjiwidget.widget.KanjiEntry?,
): KanjiStudyRanking {
    require(limit > 0) { "limit must be positive" }

    val items = results
        .asSequence()
        .mapNotNull { result ->
            val primaryValue = if (metric == RankingMetric.STUDY_TIME) {
                result.totalStudyTimeMs
            } else {
                result.totalOpenCount
            }
            
            if (primaryValue <= 0L) return@mapNotNull null
            
            val entry = metadataProvider(result.kanji)
            val lastActivityDate = runCatching { LocalDate.parse(result.lastActivityDate) }.getOrNull()
            
            KanjiStudyRankItem(
                kanji = result.kanji,
                totalStudyMs = result.totalStudyTimeMs,
                openCount = result.totalOpenCount,
                lastActivityAt = lastActivityDate
                    ?.atStartOfDay(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli(),
                meaning = if (context != null) com.example.kanjiwidget.widget.resolveDisplayMeaning(context, entry) else entry?.meaning,
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
