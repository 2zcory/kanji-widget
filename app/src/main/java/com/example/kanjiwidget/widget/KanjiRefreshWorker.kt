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
        val kanji = inputData.getString(KEY_KANJI) ?: return Result.success()
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return Result.success()

        val remote = KanjiApiClient.fetchKanji(kanji) ?: return Result.retry()
        KanjiWidgetPrefs.saveRemoteEntry(applicationContext, kanji, remote)

        val manager = AppWidgetManager.getInstance(applicationContext)
        val provider = ComponentName(applicationContext, KanjiAppWidgetProvider::class.java)
        val activeWidgetIds = manager.getAppWidgetIds(provider)

        if (activeWidgetIds.contains(widgetId)) {
            KanjiAppWidgetProvider.renderWidget(applicationContext, manager, widgetId)
        }

        return Result.success()
    }

    companion object {
        const val KEY_WIDGET_ID = "key_widget_id"
        const val KEY_KANJI = "key_kanji"
    }
}
