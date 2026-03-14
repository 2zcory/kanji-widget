package com.example.kanjiwidget.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.example.kanjiwidget.history.RecentKanjiStore
import com.example.kanjiwidget.stats.StudyTimeTracker
import com.example.kanjiwidget.widget.KanjiWidgetPrefs
import com.example.kanjiwidget.widget.KanjiAppWidgetProvider
import com.example.kanjiwidget.widget.localizeMeaningIfNeededAsync
import com.example.kanjiwidget.widget.needsVietnameseMeaning
import com.example.kanjiwidget.widget.resolveDisplayMeaning

class HomeSummaryRepository(private val context: Context) {

    fun loadSummary(): HomeSummary {
        val recentItems = RecentKanjiStore.getRecentKanji(context).map { historyItem ->
            val entry = KanjiWidgetPrefs.getRemoteEntry(context, historyItem.kanji)
            RecentKanjiSummaryItem(
                kanji = historyItem.kanji,
                viewedAt = historyItem.viewedAt,
                meaning = resolveDisplayMeaning(context, entry),
                jlpt = entry?.jlptLevel,
            )
        }
        val latestItem = recentItems.firstOrNull()
        val isWidgetInstalled = hasInstalledWidget()
        val todayStudyMs = StudyTimeTracker.getTodayTotalMs(context)
        val todayOpenCount = StudyTimeTracker.getTodayOpenCount(context)
        val showWidgetHelp = !isWidgetInstalled || (todayStudyMs <= 0L && latestItem == null)

        return HomeSummary(
            isWidgetInstalled = isWidgetInstalled,
            todayStudyMs = todayStudyMs,
            todayOpenCount = todayOpenCount,
            latestKanji = latestItem?.kanji,
            latestViewedAt = latestItem?.viewedAt,
            latestMeaning = latestItem?.meaning,
            latestJlpt = latestItem?.jlpt,
            recentKanji = recentItems,
            showWidgetHelp = showWidgetHelp,
        )
    }

    fun backfillVietnameseMeaningsIfNeeded(onUpdated: () -> Unit) {
        val recentKanji = RecentKanjiStore.getRecentKanji(context)
            .map { it.kanji }
            .distinct()

        recentKanji.forEach { kanji ->
            val entry = KanjiWidgetPrefs.getRemoteEntry(context, kanji) ?: return@forEach
            if (!needsVietnameseMeaning(context, entry)) return@forEach
            localizeMeaningIfNeededAsync(context, kanji, entry) {
                onUpdated()
            }
        }
    }

    private fun hasInstalledWidget(): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, KanjiAppWidgetProvider::class.java)
        return manager.getAppWidgetIds(component).isNotEmpty()
    }
}

data class HomeSummary(
    val isWidgetInstalled: Boolean,
    val todayStudyMs: Long,
    val todayOpenCount: Int,
    val latestKanji: String?,
    val latestViewedAt: Long?,
    val latestMeaning: String?,
    val latestJlpt: String?,
    val recentKanji: List<RecentKanjiSummaryItem>,
    val showWidgetHelp: Boolean,
)

data class RecentKanjiSummaryItem(
    val kanji: String,
    val viewedAt: Long,
    val meaning: String?,
    val jlpt: String?,
)
