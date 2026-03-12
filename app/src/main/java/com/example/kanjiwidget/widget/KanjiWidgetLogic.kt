package com.example.kanjiwidget.widget

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
    totalKanji: Int,
    source: String?,
    lastUpdatedEpochMs: Long?,
    nowEpochMs: Long = System.currentTimeMillis(),
): String {
    val progress = if (totalKanji > 0) {
        "Danh sách: $totalKanji chữ"
    } else {
        "Danh sách: đang tải"
    }
    val resolvedSource = source ?: "kanjiapi.dev"
    if (lastUpdatedEpochMs == null || lastUpdatedEpochMs <= 0L) {
        return "$progress • Nguồn: $resolvedSource"
    }

    val ageMs = (nowEpochMs - lastUpdatedEpochMs).coerceAtLeast(0L)
    val freshness = when {
        ageMs < 60_000L -> "Mới cập nhật"
        ageMs < 3_600_000L -> "${ageMs / 60_000L} phút trước"
        ageMs < 86_400_000L -> "${ageMs / 3_600_000L} giờ trước"
        else -> "${ageMs / 86_400_000L} ngày trước"
    }
    return "$progress • Nguồn: $resolvedSource • $freshness"
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
