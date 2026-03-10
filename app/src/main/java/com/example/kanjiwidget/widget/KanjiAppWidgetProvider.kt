package com.example.kanjiwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kanjiwidget.KanjiDetailActivity
import com.example.kanjiwidget.R

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
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, KanjiAppWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (!ids.contains(widgetId)) return

            val revealNow = KanjiWidgetPrefs.getRevealAnswer(context, widgetId)
            if (!revealNow) {
                if (KanjiWidgetPrefs.getCurrentKanji(context, widgetId).isNullOrBlank()) {
                    enqueueRefresh(context, widgetId)
                    renderWidget(context, manager, widgetId)
                    return
                }
                KanjiWidgetPrefs.setRevealAnswer(context, widgetId, true)
                renderWidget(context, manager, widgetId)
                return
            }

            KanjiWidgetPrefs.setRevealAnswer(context, widgetId, false)
            renderWidget(context, manager, widgetId)
            enqueueRefresh(context, widgetId, advance = true)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        renderWidget(context, appWidgetManager, appWidgetId)
    }

    private fun enqueueRefresh(context: Context, widgetId: Int, advance: Boolean = false) {
        val data = Data.Builder()
            .putInt(KanjiRefreshWorker.KEY_WIDGET_ID, widgetId)
            .putBoolean(KanjiRefreshWorker.KEY_ADVANCE, advance)
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
            "refresh_widget_$widgetId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val ACTION_NEXT_KANJI = "com.example.kanjiwidget.ACTION_NEXT_KANJI"

        fun renderWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val currentKanji = KanjiWidgetPrefs.getCurrentKanji(context, widgetId)
            val item = currentKanji?.let { KanjiWidgetPrefs.getRemoteEntry(context, it) }
            val revealAnswer = KanjiWidgetPrefs.getRevealAnswer(context, widgetId)
            val hasLoadedEntry = item != null

            val views = RemoteViews(context.packageName, R.layout.widget_kanji)
            applyResponsiveTextSizes(manager, widgetId, views)
            views.setTextViewText(R.id.tvKanji, item?.kanji ?: currentKanji ?: "...")
            views.setTextViewText(R.id.tvJlpt, "JLPT ${item?.jlptLevel ?: "..."}")
            views.setTextViewText(
                R.id.tvReading,
                when {
                    !hasLoadedEntry -> "Đang tải dữ liệu kanji từ API"
                    revealAnswer -> "On: ${item!!.onyomi} / Kun: ${item.kunyomi}"
                    else -> "Tự nhớ cách đọc trước khi hiện đáp án"
                }
            )
            views.setTextViewText(
                R.id.tvMeaning,
                when {
                    !hasLoadedEntry -> "Widget sẽ hiển thị kanji sau khi lấy xong từ API."
                    revealAnswer -> item!!.meaningVi
                    else -> "Bạn đoán nghĩa của chữ này là gì?"
                }
            )
            views.setTextViewText(
                R.id.tvExample,
                when {
                    !hasLoadedEntry -> "Cần kết nối mạng để tải từ kanjiapi.dev"
                    revealAnswer -> item!!.example
                    else -> "Nhấn nút để hiện đáp án"
                }
            )
            views.setTextViewText(R.id.tvMeta, formatMeta(context, item))

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

            val detailIntent = Intent(context, KanjiDetailActivity::class.java).apply {
                putExtra(KanjiDetailActivity.EXTRA_KANJI, item?.kanji ?: currentKanji)
                putExtra(KanjiDetailActivity.EXTRA_SOURCE, item?.source ?: "kanjiapi.dev")
                putExtra(KanjiDetailActivity.EXTRA_JLPT, item?.jlptLevel)
                putExtra(KanjiDetailActivity.EXTRA_ONYOMI, item?.onyomi)
                putExtra(KanjiDetailActivity.EXTRA_KUNYOMI, item?.kunyomi)
                putExtra(KanjiDetailActivity.EXTRA_MEANING, item?.meaningVi)
                putExtra(KanjiDetailActivity.EXTRA_NOTE, item?.example)
            }
            val detailPi = PendingIntent.getActivity(
                context,
                widgetId + 10_000,
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetContent, detailPi)

            views.setTextViewText(
                R.id.btnNext,
                when {
                    !hasLoadedEntry -> "Tải kanji"
                    revealAnswer -> "Chữ tiếp theo"
                    else -> "Hiện đáp án"
                }
            )

            manager.updateAppWidget(widgetId, views)
        }

        private fun applyResponsiveTextSizes(
            manager: AppWidgetManager,
            widgetId: Int,
            views: RemoteViews
        ) {
            val options = manager.getAppWidgetOptions(widgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val widthScale = minWidth / 220f
            val heightScale = minHeight / 110f
            val scale = minOf(widthScale, heightScale).coerceIn(0.42f, 1.35f)

            views.setTextViewTextSize(R.id.tvKanji, TypedValue.COMPLEX_UNIT_SP, 72f * scale)
            views.setTextViewTextSize(R.id.tvJlpt, TypedValue.COMPLEX_UNIT_SP, 18f * scale)
            views.setTextViewTextSize(R.id.tvReading, TypedValue.COMPLEX_UNIT_SP, 18f * scale)
            views.setTextViewTextSize(R.id.tvMeaning, TypedValue.COMPLEX_UNIT_SP, 18f * scale)
            views.setTextViewTextSize(R.id.tvExample, TypedValue.COMPLEX_UNIT_SP, 16f * scale)
            views.setTextViewTextSize(R.id.tvMeta, TypedValue.COMPLEX_UNIT_SP, 13f * scale)
            views.setTextViewTextSize(R.id.btnNext, TypedValue.COMPLEX_UNIT_SP, 15f * scale)
        }

        private fun formatMeta(context: Context, item: KanjiEntry?): String {
            val total = KanjiWidgetPrefs.getKanjiCatalog(context).size
            val progress = if (total > 0) {
                "Chế độ: ngẫu nhiên • Danh sách: $total chữ"
            } else {
                "Danh sách: đang tải"
            }
            if (item == null) return "$progress • Nguồn: kanjiapi.dev"

            val source = item.source ?: "kanjiapi.dev"
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
