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
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kanjiwidget.KanjiDetailNavigator
import com.example.kanjiwidget.R
import com.example.kanjiwidget.stats.StudyStatsRepository

class KanjiAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            val shouldRotateForNewDay = KanjiWidgetPrefs.shouldRotateForNewDay(context, id)
            renderWidget(context, appWidgetManager, id)
            if (!shouldRotateForNewDay) {
                enqueueRefresh(context, id)
            }
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

            if (KanjiWidgetPrefs.shouldRotateForNewDay(context, widgetId)) {
                KanjiWidgetPrefs.setRevealAnswer(context, widgetId, false)
                renderWidget(context, manager, widgetId)
                return
            }

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

    companion object {
        const val ACTION_NEXT_KANJI = "com.example.kanjiwidget.ACTION_NEXT_KANJI"

        fun enqueueRefresh(context: Context, widgetId: Int, advance: Boolean = false) {
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

        private fun enqueueMeaningLocalization(context: Context, kanji: String) {
            val request = OneTimeWorkRequestBuilder<KanjiMeaningLocalizationWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KanjiMeaningLocalizationWorker.KEY_KANJI, kanji)
                        .build()
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "localize_meaning_$kanji",
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun renderWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val localizedContext = ContextCompat.getContextForLanguage(context)
            val shouldRotateForNewDay = KanjiWidgetPrefs.shouldRotateForNewDay(context, widgetId)
            if (shouldRotateForNewDay) {
                KanjiWidgetPrefs.setRevealAnswer(context, widgetId, false)
                enqueueRefresh(context, widgetId, advance = true)
            } else {
                KanjiWidgetPrefs.ensureCurrentKanjiRotationBaseline(context, widgetId)
            }
            val currentKanji = KanjiWidgetPrefs.getCurrentKanji(context, widgetId)
            val item = currentKanji?.let { KanjiWidgetPrefs.getRemoteEntry(context, it) }
            val revealAnswer = KanjiWidgetPrefs.getRevealAnswer(context, widgetId)
            val hasLoadedEntry = item != null
            if (item != null && needsVietnameseMeaning(localizedContext, item)) {
                enqueueMeaningLocalization(context, item.kanji)
            }
            val sizeClass = resolveSizeClass(manager, widgetId)

            val layoutId = when (sizeClass) {
                WidgetSizeClass.COMPACT -> R.layout.widget_kanji_compact
                WidgetSizeClass.MEDIUM -> R.layout.widget_kanji
                WidgetSizeClass.EXPANDED -> R.layout.widget_kanji_expanded
            }
            val views = RemoteViews(context.packageName, layoutId)

            val statsRepository = StudyStatsRepository(context)
            val statsSummary = statsRepository.getDailyChart(days = 30)

            applyResponsiveLayout(manager, widgetId, views, sizeClass)
            views.setInt(
                R.id.widgetBackground,
                "setImageAlpha",
                (KanjiWidgetPrefs.getWidgetSurfaceAlpha(context, widgetId) * 255).toInt()
            )
            views.setTextViewText(
                R.id.tvKanji,
                item?.kanji ?: currentKanji ?: localizedContext.getString(R.string.widget_placeholder_kanji)
            )
            views.setTextViewText(
                R.id.tvJlpt,
                formatJlptLabel(localizedContext, item?.jlptLevel, R.string.jlpt_placeholder)
            )
            applyStateStyling(views, revealAnswer, hasLoadedEntry)
            applyStreakBadge(localizedContext, views, statsSummary.currentStreakDays, statsSummary.points.last().totalMs)

            views.setTextViewText(
                R.id.tvState,
                when {
                    !hasLoadedEntry -> localizedContext.getString(R.string.widget_state_loading)
                    revealAnswer -> localizedContext.getString(R.string.widget_state_revealed)
                    else -> localizedContext.getString(R.string.widget_state_hidden)
                }
            )
            views.setTextViewText(
                R.id.tvReading,
                when {
                    !hasLoadedEntry -> localizedContext.getString(R.string.widget_reading_loading)
                    revealAnswer -> formatReading(localizedContext, item!!, sizeClass)
                    else -> localizedContext.getString(R.string.widget_reading_hint)
                }
            )
            views.setTextViewText(
                R.id.tvMeaning,
                when {
                    !hasLoadedEntry -> localizedContext.getString(R.string.widget_meaning_loading)
                    revealAnswer -> resolveMeaning(localizedContext, item!!)
                    else -> localizedContext.getString(R.string.widget_meaning_hint)
                }
            )
            views.setTextViewText(
                R.id.tvExample,
                when {
                    !hasLoadedEntry -> localizedContext.getString(R.string.widget_example_loading)
                    revealAnswer -> buildExampleText(localizedContext, item!!)
                    else -> localizedContext.getString(R.string.widget_example_hint)
                }
            )
            views.setTextViewText(R.id.tvMeta, formatMeta(localizedContext, context, item))
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

            val detailIntent = KanjiDetailNavigator.buildDetailIntent(
                context = localizedContext,
                kanji = item?.kanji ?: currentKanji.orEmpty(),
                meaningFallback = resolveDisplayMeaning(localizedContext, item),
                jlptFallback = item?.jlptLevel,
            )
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
                    !hasLoadedEntry -> localizedContext.getString(R.string.widget_action_load)
                    revealAnswer -> localizedContext.getString(R.string.widget_action_next)
                    else -> localizedContext.getString(R.string.widget_action_reveal)
                }
            )

            manager.updateAppWidget(widgetId, views)
        }

        private fun applyStreakBadge(
            context: Context,
            views: RemoteViews,
            streakDays: Int,
            todayMs: Long,
        ) {
            val streakText = formatWidgetStreak(streakDays) { count ->
                context.getString(R.string.widget_streak_value, count)
            }
            if (streakText == null) {
                views.setViewVisibility(R.id.tvStreak, View.GONE)
                return
            }

            views.setViewVisibility(R.id.tvStreak, View.VISIBLE)
            
            val isDone = todayMs > 0L
            if (isDone) {
                views.setTextViewText(R.id.tvStreak, context.getString(R.string.widget_study_done))
                views.setTextColor(R.id.tvStreak, Color.parseColor("#2E7D32")) // Green
            } else {
                views.setTextViewText(R.id.tvStreak, streakText)
                views.setTextColor(R.id.tvStreak, Color.parseColor("#E65100")) // Orange
            }
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
                WidgetSizeClass.COMPACT -> 52f * scale
                WidgetSizeClass.MEDIUM -> 60f * scale
                WidgetSizeClass.EXPANDED -> 66f * scale
            }
            val jlptSize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 12f * scale
                WidgetSizeClass.MEDIUM -> 13f * scale
                WidgetSizeClass.EXPANDED -> 13f * scale
            }
            val bodySize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 13f * scale
                WidgetSizeClass.MEDIUM -> 14f * scale
                WidgetSizeClass.EXPANDED -> 15f * scale
            }
            val exampleSize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 12f * scale
                WidgetSizeClass.MEDIUM -> 13f * scale
                WidgetSizeClass.EXPANDED -> 13f * scale
            }
            val metaSize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 10f * scale
                WidgetSizeClass.MEDIUM -> 11f * scale
                WidgetSizeClass.EXPANDED -> 12f * scale
            }
            val stateSize = when (sizeClass) {
                WidgetSizeClass.COMPACT -> 11f * scale
                WidgetSizeClass.MEDIUM -> 11f * scale
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
            views.setTextViewTextSize(R.id.tvStreak, TypedValue.COMPLEX_UNIT_SP, stateSize)
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
                !hasLoadedEntry -> Color.parseColor("#54687B")
                revealAnswer -> Color.parseColor("#296E69")
                else -> Color.parseColor("#5B6A7D")
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
            val actionTextColor = Color.parseColor("#FFFFFF")
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
            val showReading = true
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
            return resolveWidgetSizeClass(minWidth, minHeight)
        }

        private fun formatReading(context: Context, item: KanjiEntry, sizeClass: WidgetSizeClass): String {
            return when (sizeClass) {
                WidgetSizeClass.EXPANDED -> context.getString(
                    R.string.widget_reading_multiline,
                    item.onyomi,
                    item.kunyomi
                )
                WidgetSizeClass.MEDIUM -> context.getString(
                    R.string.widget_reading_multiline,
                    item.onyomi,
                    item.kunyomi
                )
                WidgetSizeClass.COMPACT -> context.getString(
                    R.string.widget_reading_inline,
                    item.onyomi,
                    item.kunyomi
                )
            }
        }

        private fun formatMeta(
            localizedContext: Context,
            storageContext: Context,
            item: KanjiEntry?,
        ): String {
            return formatWidgetMeta(
                context = localizedContext,
                totalKanji = KanjiWidgetPrefs.getKanjiCatalog(storageContext).size,
                source = item?.source,
                lastUpdatedEpochMs = item?.lastUpdatedEpochMs,
            )
        }

        private fun resolveMeaning(context: Context, item: KanjiEntry): String {
            return resolveDisplayMeaning(context, item)
                ?: context.getString(R.string.widget_meaning_missing)
        }

        private fun buildExampleText(context: Context, item: KanjiEntry): String {
            return buildNoteText(context, item).ifBlank {
                context.getString(R.string.widget_example_missing)
            }
        }

        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, KanjiAppWidgetProvider::class.java)
            manager.getAppWidgetIds(provider).forEach { widgetId ->
                renderWidget(context, manager, widgetId)
            }
        }
    }
}
