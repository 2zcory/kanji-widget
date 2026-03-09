package com.example.kanjiwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kanjiwidget.R
import kotlin.random.Random

class KanjiAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            renderWidget(context, appWidgetManager, id)
            enqueueRefresh(context, id)
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
                renderWidget(context, manager, id)
                enqueueRefresh(context, id)
            }
        }
    }

    private fun enqueueRefresh(context: Context, widgetId: Int) {
        val index = KanjiWidgetPrefs.getIndex(context, widgetId).coerceIn(0, KanjiRepository.n5.lastIndex)
        val base = KanjiRepository.n5[index]

        val data = Data.Builder()
            .putInt(KanjiRefreshWorker.KEY_WIDGET_ID, widgetId)
            .putString(KanjiRefreshWorker.KEY_KANJI, base.kanji)
            .build()

        val request = OneTimeWorkRequestBuilder<KanjiRefreshWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "refresh_widget_${widgetId}_${base.kanji}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val ACTION_NEXT_KANJI = "com.example.kanjiwidget.ACTION_NEXT_KANJI"

        fun renderWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val index = KanjiWidgetPrefs.getIndex(context, widgetId).coerceIn(0, KanjiRepository.n5.lastIndex)
            val base = KanjiRepository.n5[index]
            val item = KanjiWidgetPrefs.getRemoteEntry(context, base.kanji) ?: base

            val views = RemoteViews(context.packageName, R.layout.widget_kanji)
            views.setTextViewText(R.id.tvKanji, item.kanji)
            views.setTextViewText(R.id.tvJlpt, "JLPT ${item.jlptLevel}")
            views.setTextViewText(R.id.tvReading, "${item.onyomi} / ${item.kunyomi}")
            views.setTextViewText(R.id.tvMeaning, item.meaningVi)
            views.setTextViewText(R.id.tvExample, item.example)
            views.setTextViewText(R.id.tvMeta, formatMeta(item))

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

        private fun formatMeta(item: KanjiEntry): String {
            val source = item.source ?: "built-in"
            val ts = item.lastUpdatedEpochMs
            if (ts == null || ts <= 0L) return "Nguồn: $source • Chưa có mốc cập nhật"

            val ageMs = (System.currentTimeMillis() - ts).coerceAtLeast(0L)
            val freshness = when {
                ageMs < 60_000L -> "Mới cập nhật"
                ageMs < 3_600_000L -> "${ageMs / 60_000L} phút trước"
                ageMs < 86_400_000L -> "${ageMs / 3_600_000L} giờ trước"
                else -> "${ageMs / 86_400_000L} ngày trước"
            }
            return "Nguồn: $source • $freshness"
        }
    }
}
