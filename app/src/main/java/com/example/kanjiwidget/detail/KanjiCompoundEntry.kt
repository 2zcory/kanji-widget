package com.example.kanjiwidget.detail

data class KanjiCompoundEntry(
    val written: String,
    val reading: String,
    val meaning: String,
    val usageHint: String,
    val priorities: List<String> = emptyList(),
)

data class CachedKanjiCompounds(
    val entries: List<KanjiCompoundEntry>,
    val lastUpdatedEpochMs: Long,
)
