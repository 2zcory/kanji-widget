package com.example.kanjiwidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class KanjiRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val widgetId = inputData.getInt(KEY_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val advance = inputData.getBoolean(KEY_ADVANCE, false)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return Result.success()

        val catalog = ensureCatalog() ?: return Result.retry()
        if (catalog.isEmpty()) return Result.retry()

        val currentKanji = KanjiWidgetPrefs.getCurrentKanji(applicationContext, widgetId)
        val targetKanji = if (currentKanji.isNullOrBlank() || advance) {
            selectNextKanji(widgetId, catalog, currentKanji)
        } else {
            currentKanji
        }

        KanjiWidgetPrefs.setCurrentKanji(applicationContext, widgetId, targetKanji)

        val remote = KanjiApiClient.fetchKanji(targetKanji)
            ?: KanjiWidgetPrefs.getRemoteEntry(applicationContext, targetKanji)
            ?: return Result.retry()
        KanjiWidgetPrefs.saveRemoteEntry(applicationContext, targetKanji, remote)

        val manager = AppWidgetManager.getInstance(applicationContext)
        val provider = ComponentName(applicationContext, KanjiAppWidgetProvider::class.java)
        val activeWidgetIds = manager.getAppWidgetIds(provider)

        if (activeWidgetIds.contains(widgetId)) {
            KanjiAppWidgetProvider.renderWidget(applicationContext, manager, widgetId)
        }

        return Result.success()
    }

    private fun ensureCatalog(): List<String>? {
        val cached = KanjiWidgetPrefs.getKanjiCatalog(applicationContext)
        if (cached.isNotEmpty()) return cached

        val remote = KanjiApiClient.fetchKanjiList() ?: return null
        KanjiWidgetPrefs.setKanjiCatalog(applicationContext, remote)
        return remote
    }

    private fun selectNextKanji(widgetId: Int, catalog: List<String>, currentKanji: String?): String {
        val currentIndex = catalog.indexOf(currentKanji).takeIf { it >= 0 } ?: -1
        val nextIndex = selectNextKanjiIndex(
            catalogSize = catalog.size,
            currentIndex = currentIndex,
            nextRandomInt = { bound -> (0 until bound).random() },
        )

        KanjiWidgetPrefs.setIndex(applicationContext, widgetId, nextIndex)
        return catalog[nextIndex]
    }

    companion object {
        const val KEY_WIDGET_ID = "key_widget_id"
        const val KEY_ADVANCE = "key_advance"
    }
}
