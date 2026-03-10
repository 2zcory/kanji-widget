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
        if (catalog.size == 1) return catalog.first()

        val currentIndex = catalog.indexOf(currentKanji).takeIf { it >= 0 } ?: -1
        val validIndices = catalog.indices.toSet()

        var deck = KanjiWidgetPrefs.getDeck(applicationContext, widgetId).filter { it in validIndices }
        var pos = KanjiWidgetPrefs.getDeckPos(applicationContext, widgetId)

        val needsReshuffle = deck.size != catalog.size || pos !in deck.indices
        if (needsReshuffle) {
            deck = catalog.indices.shuffled().toMutableList().also { shuffled ->
                if (currentIndex >= 0 && shuffled.firstOrNull() == currentIndex && shuffled.size > 1) {
                    val tmp = shuffled[0]
                    shuffled[0] = shuffled[1]
                    shuffled[1] = tmp
                }
            }
            pos = 0
        }

        val nextIndex = deck[pos]
        val nextPos = if (pos + 1 >= deck.size) 0 else pos + 1

        if (nextPos == 0) {
            val reshuffled = catalog.indices.shuffled().toMutableList().also { shuffled ->
                if (shuffled.firstOrNull() == nextIndex && shuffled.size > 1) {
                    val tmp = shuffled[0]
                    shuffled[0] = shuffled[1]
                    shuffled[1] = tmp
                }
            }
            KanjiWidgetPrefs.setDeck(applicationContext, widgetId, reshuffled)
            KanjiWidgetPrefs.setDeckPos(applicationContext, widgetId, 0)
        } else {
            KanjiWidgetPrefs.setDeck(applicationContext, widgetId, deck)
            KanjiWidgetPrefs.setDeckPos(applicationContext, widgetId, nextPos)
        }

        KanjiWidgetPrefs.setIndex(applicationContext, widgetId, nextIndex)
        return catalog[nextIndex]
    }

    companion object {
        const val KEY_WIDGET_ID = "key_widget_id"
        const val KEY_ADVANCE = "key_advance"
    }
}
