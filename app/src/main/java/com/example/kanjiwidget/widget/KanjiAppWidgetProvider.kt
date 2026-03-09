package com.example.kanjiwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.kanjiwidget.R
import kotlin.random.Random

class KanjiAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
            refreshFromApiAsync(context, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NEXT_KANJI) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, KanjiAppWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { id ->
                KanjiWidgetPrefs.setIndex(context, id, Random.nextInt(KanjiRepository.n5.size))
                updateWidget(context, manager, id)
                refreshFromApiAsync(context, id)
            }
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val index = KanjiWidgetPrefs.getIndex(context, widgetId).coerceIn(0, KanjiRepository.n5.lastIndex)
        val base = KanjiRepository.n5[index]
        val item = KanjiWidgetPrefs.getRemoteEntry(context, base.kanji) ?: base

        val views = RemoteViews(context.packageName, R.layout.widget_kanji)
        views.setTextViewText(R.id.tvKanji, item.kanji)
        views.setTextViewText(R.id.tvJlpt, "JLPT ${item.jlptLevel}")
        views.setTextViewText(R.id.tvReading, "${item.onyomi} / ${item.kunyomi}")
        views.setTextViewText(R.id.tvMeaning, item.meaningVi)
        views.setTextViewText(R.id.tvExample, item.example)

        val nextIntent = Intent(context, KanjiAppWidgetProvider::class.java).apply {
            action = ACTION_NEXT_KANJI
        }
        val pi = PendingIntent.getBroadcast(
            context,
            1001,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnNext, pi)

        manager.updateAppWidget(widgetId, views)
    }

    private fun refreshFromApiAsync(context: Context, widgetId: Int) {
        Thread {
            val manager = AppWidgetManager.getInstance(context)
            val index = KanjiWidgetPrefs.getIndex(context, widgetId).coerceIn(0, KanjiRepository.n5.lastIndex)
            val base = KanjiRepository.n5[index]
            val remote = KanjiApiClient.fetchKanji(base.kanji) ?: return@Thread
            KanjiWidgetPrefs.saveRemoteEntry(context, base.kanji, remote)
            updateWidget(context, manager, widgetId)
        }.start()
    }

    companion object {
        const val ACTION_NEXT_KANJI = "com.example.kanjiwidget.ACTION_NEXT_KANJI"
    }
}
