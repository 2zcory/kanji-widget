package com.example.kanjiwidget.widget

import android.content.Context
import com.example.kanjiwidget.R

internal enum class WidgetSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

internal fun resolveWidgetSizeClass(
    minWidth: Int,
    minHeight: Int,
): WidgetSizeClass {
    return when {
        minWidth >= 250 && minHeight >= 160 -> WidgetSizeClass.EXPANDED
        minHeight >= 130 || minWidth >= 200 -> WidgetSizeClass.MEDIUM
        else -> WidgetSizeClass.COMPACT
    }
}

internal fun formatWidgetMeta(
    context: Context,
    totalKanji: Int,
    source: String?,
    lastUpdatedEpochMs: Long?,
    nowEpochMs: Long = System.currentTimeMillis(),
): String {
    val resources = context.resources
    return formatWidgetMeta(
        totalKanji = totalKanji,
        source = source,
        lastUpdatedEpochMs = lastUpdatedEpochMs,
        nowEpochMs = nowEpochMs,
        separator = resources.getString(R.string.bullet_separator),
        catalogLoadingText = resources.getString(R.string.widget_meta_catalog_loading),
        catalogCountText = { count ->
            resources.getQuantityString(R.plurals.widget_catalog_count, count, count)
        },
        defaultSource = resources.getString(R.string.stroke_order_source_default),
        sourceText = { value ->
            resources.getString(R.string.widget_meta_source_value, value)
        },
        freshnessJustNowText = resources.getString(R.string.widget_meta_freshness_just_now),
        freshnessMinutesText = { value ->
            resources.getQuantityString(R.plurals.widget_freshness_minutes, value, value)
        },
        freshnessHoursText = { value ->
            resources.getQuantityString(R.plurals.widget_freshness_hours, value, value)
        },
        freshnessDaysText = { value ->
            resources.getQuantityString(R.plurals.widget_freshness_days, value, value)
        },
    )
}

internal fun formatWidgetMeta(
    totalKanji: Int,
    source: String?,
    lastUpdatedEpochMs: Long?,
    nowEpochMs: Long = System.currentTimeMillis(),
    separator: String,
    catalogLoadingText: String,
    catalogCountText: (Int) -> String,
    defaultSource: String,
    sourceText: (String) -> String,
    freshnessJustNowText: String,
    freshnessMinutesText: (Int) -> String,
    freshnessHoursText: (Int) -> String,
    freshnessDaysText: (Int) -> String,
): String {
    val progress = if (totalKanji > 0) catalogCountText(totalKanji) else catalogLoadingText
    val resolvedSource = source?.takeIf { it.isNotBlank() } ?: defaultSource
    val resolvedSourceText = sourceText(resolvedSource)
    if (lastUpdatedEpochMs == null || lastUpdatedEpochMs <= 0L) {
        return listOf(progress, resolvedSourceText).joinToString(separator)
    }

    val ageMs = (nowEpochMs - lastUpdatedEpochMs).coerceAtLeast(0L)
    val freshness = when {
        ageMs < 60_000L -> freshnessJustNowText
        ageMs < 3_600_000L -> freshnessMinutesText((ageMs / 60_000L).toInt().coerceAtLeast(1))
        ageMs < 86_400_000L -> freshnessHoursText((ageMs / 3_600_000L).toInt().coerceAtLeast(1))
        else -> freshnessDaysText((ageMs / 86_400_000L).toInt().coerceAtLeast(1))
    }
    return listOf(progress, resolvedSourceText, freshness).joinToString(separator)
}

internal fun selectNextKanjiIndex(
    catalogSize: Int,
    currentIndex: Int,
    nextRandomInt: (Int) -> Int,
): Int {
    require(catalogSize > 0) { "catalogSize must be positive" }
    if (catalogSize == 1) return 0

    val available = (0 until catalogSize).filter { it != currentIndex }
    return available[nextRandomInt(available.size)]
}

internal fun shouldRotateWidgetForNewDay(
    hasCurrentKanji: Boolean,
    lastShownLocalDay: String?,
    currentLocalDay: String,
): Boolean {
    if (!hasCurrentKanji) return false
    val recordedDay = lastShownLocalDay?.takeIf { it.isNotBlank() } ?: return false
    return recordedDay != currentLocalDay
}
