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

class KanjiAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            KanjiWidgetPrefs.setRevealAnswer(context, id, false)
            renderWidget(context, appWidgetManager, id)
            enqueueRefresh(context, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NEXT_KANJI) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, KanjiAppWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (!ids.contains(widgetId)) return

            val revealNow = KanjiWidgetPrefs.getRevealAnswer(context, widgetId)
            if (!revealNow) {
                KanjiWidgetPrefs.setRevealAnswer(context, widgetId, true)
                renderWidget(context, manager, widgetId)
                return
            }

            val nextIndex = nextStudyIndex(widgetId, context)
            KanjiWidgetPrefs.setIndex(context, widgetId, nextIndex)
            KanjiWidgetPrefs.setRevealAnswer(context, widgetId, false)
            renderWidget(context, manager, widgetId)
            enqueueRefresh(context, widgetId)
        }
    }

    private fun nextStudyIndex(widgetId: Int, context: Context): Int {
        val total = KanjiRepository.n5.size
        if (total <= 1) return 0

        val current = KanjiWidgetPrefs.getIndex(context, widgetId).coerceIn(0, total - 1)
        val validIndices = (0 until total).toSet()

        var deck = KanjiWidgetPrefs.getDeck(context, widgetId).filter { it in validIndices }
        var pos = KanjiWidgetPrefs.getDeckPos(context, widgetId)

        val needsReshuffle = deck.size != total || pos !in deck.indices
        if (needsReshuffle) {
            deck = (0 until total).shuffled().toMutableList().also { shuffled ->
                if (shuffled.firstOrNull() == current && shuffled.size > 1) {
                    val tmp = shuffled[0]
                    shuffled[0] = shuffled[1]
                    shuffled[1] = tmp
                }
            }
            pos = 0
        }

        val next = deck[pos]
        val nextPos = if (pos + 1 >= deck.size) 0 else pos + 1

        if (nextPos == 0) {
            val reshuffled = (0 until total).shuffled().toMutableList().also { shuffled ->
                if (shuffled.firstOrNull() == next && shuffled.size > 1) {
                    val tmp = shuffled[0]
                    shuffled[0] = shuffled[1]
                    shuffled[1] = tmp
                }
            }
            KanjiWidgetPrefs.setDeck(context, widgetId, reshuffled)
            KanjiWidgetPrefs.setDeckPos(context, widgetId, 0)
        } else {
            KanjiWidgetPrefs.setDeck(context, widgetId, deck)
            KanjiWidgetPrefs.setDeckPos(context, widgetId, nextPos)
        }

        return next
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
            val remote = KanjiWidgetPrefs.getRemoteEntry(context, base.kanji)
            val item = resolveDisplayEntry(base, remote)

            val revealAnswer = KanjiWidgetPrefs.getRevealAnswer(context, widgetId)

            val views = RemoteViews(context.packageName, R.layout.widget_kanji)
            views.setTextViewText(R.id.tvKanji, item.kanji)
            views.setTextViewText(R.id.tvJlpt, "JLPT ${item.jlptLevel}")
            views.setTextViewText(
                R.id.tvReading,
                if (revealAnswer) "On: ${item.onyomi} / Kun: ${item.kunyomi}" else "Tự nhớ cách đọc trước khi hiện đáp án"
            )
            views.setTextViewText(
                R.id.tvMeaning,
                if (revealAnswer) item.meaningVi else "Bạn đoán nghĩa của chữ này là gì?"
            )
            views.setTextViewText(
                R.id.tvExample,
                if (revealAnswer) item.example else "Nhấn nút để hiện đáp án"
            )
            views.setTextViewText(R.id.tvMeta, formatMeta(item, index, KanjiRepository.n5.size))

            val nextIntent = Intent(context, KanjiAppWidgetProvider::class.java).apply {
                action = ACTION_NEXT_KANJI
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                widgetId,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btnNext, pi)
            views.setTextViewText(R.id.btnNext, if (revealAnswer) "Chữ tiếp theo" else "Hiện đáp án")

            manager.updateAppWidget(widgetId, views)
        }

        private fun resolveDisplayEntry(base: KanjiEntry, remote: KanjiEntry?): KanjiEntry {
            if (remote == null) return base

            return KanjiEntry(
                kanji = base.kanji,
                onyomi = remote.onyomi.ifBlank { base.onyomi },
                kunyomi = remote.kunyomi.ifBlank { base.kunyomi },
                meaningVi = base.meaningVi,
                example = base.example,
                jlptLevel = remote.jlptLevel.ifBlank { base.jlptLevel },
                source = remote.source,
                lastUpdatedEpochMs = remote.lastUpdatedEpochMs,
            )
        }

        private fun formatMeta(item: KanjiEntry, index: Int, total: Int): String {
            val source = item.source ?: "built-in"
            val progress = "Tiến độ: ${index + 1}/$total"
            val ts = item.lastUpdatedEpochMs
            if (ts == null || ts <= 0L) return "$progress • Nguồn: $source"

            val ageMs = (System.currentTimeMillis() - ts).coerceAtLeast(0L)
            val freshness = when {
                ageMs < 60_000L -> "Mới cập nhật"
                ageMs < 3_600_000L -> "${ageMs / 60_000L} phút trước"
                ageMs < 86_400_000L -> "${ageMs / 3_600_000L} giờ trước"
                else -> "${ageMs / 86_400_000L} ngày trước"
            }
            return "$progress • Nguồn: $source • $freshness"
        }
    }
}
