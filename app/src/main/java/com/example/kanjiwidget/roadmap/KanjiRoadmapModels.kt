package com.example.kanjiwidget.roadmap

import com.example.kanjiwidget.widget.KanjiEntry

data class KanjiRoadmapStageDefinition(
    val id: KanjiRoadmapStageId,
    val title: String,
    val jlptLevel: String,
    val gradeBandLabel: String,
    val sortOrder: Int,
)

enum class KanjiRoadmapStageId {
    N5_FOUNDATION,
    N4_EXPANSION,
    N3_CORE,
    N2_ADVANCED,
    N1_EXPERT,
    UNCLASSIFIED,
}

data class KanjiRoadmapStageProgress(
    val definition: KanjiRoadmapStageDefinition,
    val totalCount: Int,
    val completedCount: Int,
) {
    val remainingCount: Int
        get() = (totalCount - completedCount).coerceAtLeast(0)
}

data class KanjiRoadmapSnapshot(
    val stages: List<KanjiRoadmapStageProgress>,
    val currentStage: KanjiRoadmapStageProgress?,
    val nextStage: KanjiRoadmapStageProgress?,
    val completedKanji: Set<String>,
)

data class KanjiRoadmapRecommendation(
    val stage: KanjiRoadmapStageProgress?,
    val batch: List<KanjiEntry>,
)

internal val DEFAULT_ROADMAP_STAGE_DEFINITIONS: List<KanjiRoadmapStageDefinition> = listOf(
    KanjiRoadmapStageDefinition(
        id = KanjiRoadmapStageId.N5_FOUNDATION,
        title = "N5 Foundation",
        jlptLevel = "N5",
        gradeBandLabel = "Grades 1-2",
        sortOrder = 0,
    ),
    KanjiRoadmapStageDefinition(
        id = KanjiRoadmapStageId.N4_EXPANSION,
        title = "N4 Expansion",
        jlptLevel = "N4",
        gradeBandLabel = "Grades 3-4",
        sortOrder = 1,
    ),
    KanjiRoadmapStageDefinition(
        id = KanjiRoadmapStageId.N3_CORE,
        title = "N3 Core",
        jlptLevel = "N3",
        gradeBandLabel = "Grades 5-6",
        sortOrder = 2,
    ),
    KanjiRoadmapStageDefinition(
        id = KanjiRoadmapStageId.N2_ADVANCED,
        title = "N2 Advanced",
        jlptLevel = "N2",
        gradeBandLabel = "Secondary Core",
        sortOrder = 3,
    ),
    KanjiRoadmapStageDefinition(
        id = KanjiRoadmapStageId.N1_EXPERT,
        title = "N1 Expert",
        jlptLevel = "N1",
        gradeBandLabel = "Advanced+",
        sortOrder = 4,
    ),
)

internal fun roadmapStageIdForJlpt(jlptLevel: String?): KanjiRoadmapStageId {
    return when (jlptLevel?.trim()?.uppercase()) {
        "N5" -> KanjiRoadmapStageId.N5_FOUNDATION
        "N4" -> KanjiRoadmapStageId.N4_EXPANSION
        "N3" -> KanjiRoadmapStageId.N3_CORE
        "N2" -> KanjiRoadmapStageId.N2_ADVANCED
        "N1" -> KanjiRoadmapStageId.N1_EXPERT
        else -> KanjiRoadmapStageId.UNCLASSIFIED
    }
}
