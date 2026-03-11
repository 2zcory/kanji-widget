package com.example.kanjiwidget

import android.content.Context
import android.content.Intent
import com.example.kanjiwidget.widget.KanjiWidgetPrefs

object KanjiDetailNavigator {

    fun buildDetailIntent(
        context: Context,
        kanji: String,
        meaningFallback: String? = null,
        jlptFallback: String? = null,
    ): Intent {
        val entry = KanjiWidgetPrefs.getRemoteEntry(context, kanji)
        return Intent(context, KanjiDetailActivity::class.java).apply {
            putExtra(KanjiDetailActivity.EXTRA_KANJI, kanji)
            putExtra(
                KanjiDetailActivity.EXTRA_SOURCE,
                entry?.source ?: context.getString(R.string.stroke_order_source_default)
            )
            putExtra(KanjiDetailActivity.EXTRA_JLPT, entry?.jlptLevel ?: jlptFallback)
            putExtra(KanjiDetailActivity.EXTRA_ONYOMI, entry?.onyomi)
            putExtra(KanjiDetailActivity.EXTRA_KUNYOMI, entry?.kunyomi)
            putExtra(KanjiDetailActivity.EXTRA_MEANING, entry?.meaningVi ?: meaningFallback)
            putExtra(KanjiDetailActivity.EXTRA_NOTE, entry?.example)
            putExtra(KanjiDetailActivity.EXTRA_STROKE_COUNT, entry?.strokeCount ?: 0)
            putExtra(KanjiDetailActivity.EXTRA_GRADE, entry?.grade ?: 0)
            putExtra(KanjiDetailActivity.EXTRA_FREQUENCY, entry?.frequency ?: 0)
        }
    }

    fun buildRandomDetailIntent(
        context: Context,
        catalog: List<String>,
        currentKanji: String?,
    ): Intent? {
        val selectedKanji = selectRandomKanji(catalog, currentKanji) ?: return null
        return buildDetailIntent(
            context = context,
            kanji = selectedKanji,
        )
    }

    fun selectRandomKanji(catalog: List<String>, currentKanji: String?): String? {
        return when {
            catalog.isEmpty() -> null
            catalog.size == 1 -> catalog.first()
            else -> catalog
                .filterNot { it == currentKanji }
                .ifEmpty { catalog }
                .random()
        }
    }
}
