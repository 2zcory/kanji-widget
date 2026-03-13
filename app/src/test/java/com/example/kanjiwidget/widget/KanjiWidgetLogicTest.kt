package com.example.kanjiwidget.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class KanjiWidgetLogicTest {
    @Test
    fun resolveWidgetSizeClass_returnsCompactForSmallBounds() {
        assertEquals(WidgetSizeClass.COMPACT, resolveWidgetSizeClass(minWidth = 180, minHeight = 100))
    }

    @Test
    fun resolveWidgetSizeClass_returnsMediumWhenOneMediumThresholdIsMet() {
        assertEquals(WidgetSizeClass.MEDIUM, resolveWidgetSizeClass(minWidth = 220, minHeight = 110))
        assertEquals(WidgetSizeClass.MEDIUM, resolveWidgetSizeClass(minWidth = 180, minHeight = 140))
    }

    @Test
    fun resolveWidgetSizeClass_returnsExpandedForLargeBounds() {
        assertEquals(WidgetSizeClass.EXPANDED, resolveWidgetSizeClass(minWidth = 260, minHeight = 170))
    }

    @Test
    fun formatWidgetMeta_formatsLoadingStateWithoutTimestamp() {
        assertEquals(
            "Danh sách: đang tải • Nguồn: kanjiapi.dev",
            formatWidgetMeta(
                totalKanji = 0,
                source = null,
                lastUpdatedEpochMs = null,
                nowEpochMs = 1_000L,
                separator = " • ",
                catalogLoadingText = "Danh sách: đang tải",
                catalogCountText = { count -> "Danh sách: $count chữ" },
                defaultSource = "kanjiapi.dev",
                sourceText = { value -> "Nguồn: $value" },
                freshnessJustNowText = "Mới cập nhật",
                freshnessMinutesText = { value -> "$value phút trước" },
                freshnessHoursText = { value -> "$value giờ trước" },
                freshnessDaysText = { value -> "$value ngày trước" },
            ),
        )
    }

    @Test
    fun formatWidgetMeta_formatsRelativeAge() {
        assertEquals(
            "Danh sách: 12 chữ • Nguồn: cache • 2 giờ trước",
            formatWidgetMeta(
                totalKanji = 12,
                source = "cache",
                lastUpdatedEpochMs = 1_000L,
                nowEpochMs = 7_201_000L,
                separator = " • ",
                catalogLoadingText = "Danh sách: đang tải",
                catalogCountText = { count -> "Danh sách: $count chữ" },
                defaultSource = "kanjiapi.dev",
                sourceText = { value -> "Nguồn: $value" },
                freshnessJustNowText = "Mới cập nhật",
                freshnessMinutesText = { value -> "$value phút trước" },
                freshnessHoursText = { value -> "$value giờ trước" },
                freshnessDaysText = { value -> "$value ngày trước" },
            ),
        )
    }

    @Test
    fun selectNextKanjiIndex_avoidsCurrentIndexWhenPossible() {
        val nextIndex = selectNextKanjiIndex(
            catalogSize = 4,
            currentIndex = 1,
            nextRandomInt = { lastBound ->
                assertEquals(3, lastBound)
                1
            },
        )

        assertEquals(2, nextIndex)
    }

    @Test
    fun selectNextKanjiIndex_returnsOnlyEntryForSingleItemCatalog() {
        assertEquals(0, selectNextKanjiIndex(catalogSize = 1, currentIndex = 0, nextRandomInt = { 0 }))
    }
}
