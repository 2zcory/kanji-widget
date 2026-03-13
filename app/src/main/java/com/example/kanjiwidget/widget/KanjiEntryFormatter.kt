package com.example.kanjiwidget.widget

import android.content.Context
import com.example.kanjiwidget.R

internal fun normalizeMeaning(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isBlank() || trimmed == KanjiEntry.MEANING_UNKNOWN) return null
    return trimmed
}

internal fun formatJlptLabel(
    context: Context,
    jlptLevel: String?,
    placeholderResId: Int,
): String {
    val trimmed = jlptLevel?.trim().orEmpty()
    return if (trimmed.isBlank() || trimmed == KanjiEntry.JLPT_UNKNOWN) {
        context.getString(placeholderResId)
    } else {
        context.getString(R.string.jlpt_format, trimmed)
    }
}

internal fun buildNoteText(context: Context, entry: KanjiEntry?): String {
    if (entry == null) return ""
    val parts = mutableListOf<String>()
    val separator = context.getString(R.string.bullet_separator)
    val unicode = entry.unicode?.trim().takeIf { !it.isNullOrBlank() }
        ?: parseLegacyUnicode(entry.example)
    unicode?.let {
        parts += if (it.startsWith("U+")) it else "U+$it"
    }

    if (shouldShowSourceNote(entry)) {
        val source = entry.source?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.stroke_order_source_default)
        parts += context.getString(R.string.widget_meta_source_value, source)
    }

    if (parts.isEmpty()) {
        return entry.example.trim()
    }
    return parts.joinToString(separator)
}

private fun shouldShowSourceNote(entry: KanjiEntry): Boolean {
    return entry.strokeCount == null && entry.grade == null && entry.frequency == null
}

private fun parseLegacyUnicode(example: String?): String? {
    val match = example?.let { Regex("U\\+[0-9A-Fa-f]+").find(it) } ?: return null
    return match.value
}
