package com.example.kanjiwidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class KanjiMeaningLocalizationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val kanji = inputData.getString(KEY_KANJI)?.trim().orEmpty()
        if (kanji.isBlank()) return Result.success()

        val entry = KanjiWidgetPrefs.getRemoteEntry(applicationContext, kanji) ?: return Result.success()
        if (!needsVietnameseMeaning(applicationContext, entry)) return Result.success()

        val localized = localizeMeaningIfNeeded(applicationContext, entry)
        if (localized == entry) return Result.success()

        KanjiWidgetPrefs.saveRemoteEntry(applicationContext, kanji, localized)

        val manager = AppWidgetManager.getInstance(applicationContext)
        val provider = ComponentName(applicationContext, KanjiAppWidgetProvider::class.java)
        manager.getAppWidgetIds(provider).forEach { widgetId ->
            KanjiAppWidgetProvider.renderWidget(applicationContext, manager, widgetId)
        }
        return Result.success()
    }

    companion object {
        const val KEY_KANJI = "key_kanji"
    }
}
