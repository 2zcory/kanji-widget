package com.example.kanjiwidget.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.example.kanjiwidget.history.RecentKanjiStore
import com.example.kanjiwidget.stats.StudyTimeTracker
import com.example.kanjiwidget.widget.KanjiWidgetPrefs
import com.example.kanjiwidget.widget.KanjiAppWidgetProvider

class HomeSummaryRepository(private val context: Context) {

    fun loadSummary(): HomeSummary {
        val latestKanji = RecentKanjiStore.getLatestKanji(context)
        val latestViewedAt = RecentKanjiStore.getLatestViewedAt(context)
        val latestEntry = latestKanji?.let { KanjiWidgetPrefs.getRemoteEntry(context, it) }
        val isWidgetInstalled = hasInstalledWidget()
        val todayStudyMs = StudyTimeTracker.getTodayTotalMs(context)
        val todayOpenCount = StudyTimeTracker.getTodayOpenCount(context)
        val showWidgetHelp = !isWidgetInstalled || (todayStudyMs <= 0L && latestKanji == null)

        return HomeSummary(
            isWidgetInstalled = isWidgetInstalled,
            todayStudyMs = todayStudyMs,
            todayOpenCount = todayOpenCount,
            latestKanji = latestKanji,
            latestViewedAt = latestViewedAt,
            latestMeaning = latestEntry?.meaningVi,
            latestJlpt = latestEntry?.jlptLevel,
            showWidgetHelp = showWidgetHelp,
        )
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
    val showWidgetHelp: Boolean,
)
