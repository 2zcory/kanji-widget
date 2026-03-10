package com.example.kanjiwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { widgetId ->
            KanjiWidgetPrefs.clearWidgetState(context, widgetId)
            WorkManager.getInstance(context).cancelUniqueWork("refresh_widget_$widgetId")
        }
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
            val sizeClass = resolveSizeClass(manager, widgetId)

            val layoutId = when (sizeClass) {
                WidgetSizeClass.COMPACT -> R.layout.widget_kanji_compact
                WidgetSizeClass.MEDIUM -> R.layout.widget_kanji
                WidgetSizeClass.EXPANDED -> R.layout.widget_kanji_expanded
            }
            val views = RemoteViews(context.packageName, layoutId)
            applyResponsiveLayout(manager, widgetId, views, sizeClass)
            views.setInt(
                R.id.widgetBackground,
                "setImageAlpha",
                (KanjiWidgetPrefs.getWidgetSurfaceAlpha(context) * 255).toInt()
            )
            views.setTextViewText(R.id.tvKanji, item?.kanji ?: currentKanji ?: "...")
            views.setTextViewText(R.id.tvJlpt, "JLPT ${item?.jlptLevel ?: "..."}")
            applyStateStyling(views, revealAnswer, hasLoadedEntry)
            views.setTextViewText(
                R.id.tvState,
                when {
                    !hasLoadedEntry -> "Đang tải"
                    revealAnswer -> "Đã mở"
                    else -> "Ẩn đáp án"
                }
            )
            views.setTextViewText(
                R.id.tvReading,
                when {
                    !hasLoadedEntry -> "Đợi tải Kanji đầu tiên"
                    revealAnswer -> "On: ${item!!.onyomi}  •  Kun: ${item.kunyomi}"
                    else -> "Thử nhớ cách đọc trước khi mở"
                }
            )
            views.setTextViewText(
                R.id.tvMeaning,
                when {
                    !hasLoadedEntry -> "Cần mạng cho lần đồng bộ đầu tiên."
                    revealAnswer -> item!!.meaningVi
                    else -> "Đoán nghĩa của chữ này trước khi bấm mở."
                }
            )
            views.setTextViewText(
                R.id.tvExample,
                when {
                    !hasLoadedEntry -> "Widget sẽ sẵn sàng ngay sau khi tải xong."
                    revealAnswer -> item!!.example
                    else -> "Chạm nút bên dưới để mở reading và nghĩa."
                }
            )
            views.setTextViewText(R.id.tvMeta, formatMeta(context, item))
            applyContentVisibility(views, sizeClass, revealAnswer, hasLoadedEntry)

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

        private fun applyResponsiveLayout(
            manager: AppWidgetManager,
            widgetId: Int,
            views: RemoteViews,
            sizeClass: WidgetSizeClass,
        ) {
            val options = manager.getAppWidgetOptions(widgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val widthScale = minWidth / 220f
            val heightScale = minHeight / 110f
            val scale = minOf(widthScale, heightScale).coerceIn(0.42f, 1.45f)

            val kanjiSize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 56f * scale
                WidgetSizeClass.MEDIUM -> 66f * scale
                WidgetSizeClass.EXPANDED -> 72f * scale
            }
            val jlptSize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 14f * scale
                WidgetSizeClass.MEDIUM -> 16f * scale
                WidgetSizeClass.EXPANDED -> 18f * scale
            }
            val bodySize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 13f * scale
                WidgetSizeClass.MEDIUM -> 15f * scale
                WidgetSizeClass.EXPANDED -> 16f * scale
            }
            val exampleSize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 12f * scale
                WidgetSizeClass.MEDIUM -> 14f * scale
                WidgetSizeClass.EXPANDED -> 13f * scale
            }
            val metaSize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 11f * scale
                WidgetSizeClass.MEDIUM -> 12f * scale
                WidgetSizeClass.EXPANDED -> 13f * scale
            }
            val stateSize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 11f * scale
                WidgetSizeClass.MEDIUM -> 12f * scale
                WidgetSizeClass.EXPANDED -> 12f * scale
            }
            val buttonSize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 13f * scale
                WidgetSizeClass.MEDIUM -> 14f * scale
                WidgetSizeClass.EXPANDED -> 15f * scale
            }

            views.setTextViewTextSize(R.id.tvKanji, TypedValue.COMPLEX_UNIT_SP, kanjiSize)
            views.setTextViewTextSize(R.id.tvJlpt, TypedValue.COMPLEX_UNIT_SP, jlptSize)
            views.setTextViewTextSize(R.id.tvState, TypedValue.COMPLEX_UNIT_SP, stateSize)
            views.setTextViewTextSize(R.id.tvReading, TypedValue.COMPLEX_UNIT_SP, bodySize)
            views.setTextViewTextSize(R.id.tvMeaning, TypedValue.COMPLEX_UNIT_SP, bodySize)
            views.setTextViewTextSize(R.id.tvExample, TypedValue.COMPLEX_UNIT_SP, exampleSize)
            views.setTextViewTextSize(R.id.tvMeta, TypedValue.COMPLEX_UNIT_SP, metaSize)
            views.setTextViewTextSize(R.id.btnNext, TypedValue.COMPLEX_UNIT_SP, buttonSize)
        }

        private fun applyStateStyling(
            views: RemoteViews,
            revealAnswer: Boolean,
            hasLoadedEntry: Boolean,
        ) {
            val stateBackground = when {
                !hasLoadedEntry -> R.drawable.bg_widget_pill_loading
                revealAnswer -> R.drawable.bg_widget_pill_revealed
                else -> R.drawable.bg_widget_pill_hidden
            }
            val stateTextColor = when {
                !hasLoadedEntry -> Color.parseColor("#4D6172")
                revealAnswer -> Color.parseColor("#376347")
                else -> Color.parseColor("#4F5B66")
            }
            val heroBackground = when {
                !hasLoadedEntry -> R.drawable.bg_widget_hero_loading
                revealAnswer -> R.drawable.bg_widget_hero_revealed
                else -> R.drawable.bg_widget_hero
            }
            val actionBackground = when {
                !hasLoadedEntry -> R.drawable.bg_widget_action_loading
                revealAnswer -> R.drawable.bg_widget_action_revealed
                else -> R.drawable.bg_widget_action
            }
            val actionTextColor = if (revealAnswer) {
                Color.parseColor("#F5FFF3")
            } else {
                Color.parseColor("#FFF8F1")
            }
            val exampleBackground = if (revealAnswer) {
                R.drawable.bg_widget_info_card_revealed
            } else {
                R.drawable.bg_widget_info_card
            }

            views.setInt(R.id.tvState, "setBackgroundResource", stateBackground)
            views.setTextColor(R.id.tvState, stateTextColor)
            views.setInt(R.id.tvKanji, "setBackgroundResource", heroBackground)
            views.setInt(R.id.btnNext, "setBackgroundResource", actionBackground)
            views.setTextColor(R.id.btnNext, actionTextColor)
            views.setInt(R.id.tvExample, "setBackgroundResource", exampleBackground)
        }

        private fun applyContentVisibility(
            views: RemoteViews,
            sizeClass: WidgetSizeClass,
            revealAnswer: Boolean,
            hasLoadedEntry: Boolean,
        ) {
            val showMeaning = sizeClass != WidgetSizeClass.COMPACT
            val showReading = sizeClass == WidgetSizeClass.EXPANDED
            val showExample = sizeClass == WidgetSizeClass.EXPANDED
                || (sizeClass == WidgetSizeClass.MEDIUM && revealAnswer && hasLoadedEntry)
            val showMeta = sizeClass != WidgetSizeClass.COMPACT

            views.setViewVisibility(R.id.tvReading, if (showReading) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.tvMeaning, if (showMeaning) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.tvExample, if (showExample) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.tvMeta, if (showMeta) View.VISIBLE else View.GONE)
        }

        private fun resolveSizeClass(
            manager: AppWidgetManager,
            widgetId: Int,
        ): WidgetSizeClass {
            val options = manager.getAppWidgetOptions(widgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)

            return when {
                minWidth >= 250 && minHeight >= 160 -> WidgetSizeClass.EXPANDED
                minHeight >= 130 || minWidth >= 200 -> WidgetSizeClass.MEDIUM
                else -> WidgetSizeClass.COMPACT
            }
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

        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, KanjiAppWidgetProvider::class.java)
            manager.getAppWidgetIds(provider).forEach { widgetId ->
                renderWidget(context, manager, widgetId)
            }
        }

        private enum class WidgetSizeClass {
            COMPACT,
            MEDIUM,
            EXPANDED,
        }
    }
}
