package com.example.kanjiwidget.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class KanjiCompoundRepositoryTest {
    @Test
    fun selectDisplayCompounds_prefersPrioritizedReadableRows() {
        val selected = selectDisplayCompounds(
            kanji = "学",
            candidates = listOf(
                RawKanjiCompound(
                    written = "大学",
                    reading = "だいがく",
                    meaning = "university",
                    priorities = listOf("news1", "ichi1"),
                ),
                RawKanjiCompound(
                    written = "学位",
                    reading = "がくい",
                    meaning = "academic degree",
                    priorities = listOf("nf24"),
                ),
                RawKanjiCompound(
                    written = "国際情報大学",
                    reading = "こくさいじょうほうだいがく",
                    meaning = "University of International and Information Studies",
                    priorities = emptyList(),
                ),
            ),
        )

        assertEquals(listOf("大学", "学位", "国際情報大学"), selected.map { it.written })
        assertEquals("News-heavy", selected.first().usageHint)
        assertEquals("Common word", selected[1].usageHint)
    }

    @Test
    fun selectDisplayCompounds_filtersRowsMissingRequiredFields() {
        val selected = selectDisplayCompounds(
            kanji = "日",
            candidates = listOf(
                RawKanjiCompound(written = "日記", reading = "にっき", meaning = "diary", priorities = emptyList()),
                RawKanjiCompound(written = "日", reading = "にち", meaning = "day", priorities = listOf("news1")),
                RawKanjiCompound(written = "日本", reading = "", meaning = "Japan", priorities = listOf("news1")),
                RawKanjiCompound(written = "休日", reading = "きゅうじつ", meaning = "", priorities = emptyList()),
            ),
        )

        assertEquals(2, selected.size)
        assertEquals(listOf("日本", "日記"), selected.map { it.written })
        assertEquals("", selected.first().reading)
        assertEquals("News-heavy", selected.first().usageHint)
        assertEquals("Study word", selected[1].usageHint)
    }

    @Test
    fun deriveUsageHint_marksReferenceTermsFromMeaningShape() {
        assertEquals(
            "Reference term",
            deriveUsageHint(emptyList(), "(abbr) Japan Federation of Bar Associations")
        )
    }
}
