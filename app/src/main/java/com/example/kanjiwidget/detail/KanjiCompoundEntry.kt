package com.example.kanjiwidget.detail

data class KanjiCompoundEntry(
    val written: String,
    val reading: String,
    val meaning: String,
    val usageHintKey: UsageHintKey,
    val priorities: List<String> = emptyList(),
)

enum class UsageHintKey(val storageKey: String) {
    NEWS_HEAVY("news_heavy"),
    COMMON_WORD("common_word"),
    REFERENCE_TERM("reference_term"),
    STUDY_WORD("study_word");

    companion object {
        fun fromStorageKey(value: String?): UsageHintKey? {
            return values().firstOrNull { it.storageKey == value }
        }

        fun fromLegacyText(value: String?): UsageHintKey? {
            return when (value?.trim()) {
                "News-heavy" -> NEWS_HEAVY
                "Common word" -> COMMON_WORD
                "Reference term" -> REFERENCE_TERM
                "Study word" -> STUDY_WORD
                else -> null
            }
        }
    }
}

data class CachedKanjiCompounds(
    val entries: List<KanjiCompoundEntry>,
    val lastUpdatedEpochMs: Long,
)
