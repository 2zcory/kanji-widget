package com.example.kanjiwidget

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.example.kanjiwidget.placement.PlacementResultPrefs
import com.example.kanjiwidget.placement.SavedPlacementResult
import com.example.kanjiwidget.roadmap.KanjiRoadmapRepository
import com.example.kanjiwidget.roadmap.roadmapStageIdForJlpt
import com.example.kanjiwidget.theme.ThemeController

class PlacementResultActivity : ThemedActivity() {

    companion object {
        const val EXTRA_STAGE_JLPT = "placement_stage_jlpt"
        const val EXTRA_STAGE_TITLE = "placement_stage_title"
        const val EXTRA_STAGE_GRADE_BAND = "placement_stage_grade_band"
        const val EXTRA_CONFIDENCE_LABEL = "placement_confidence_label"
        const val EXTRA_RATIONALE = "placement_rationale"
        const val EXTRA_TOTAL_CORRECT = "placement_total_correct"
        const val EXTRA_TOTAL_QUESTIONS = "placement_total_questions"
    }

    private lateinit var roadmapRepository: KanjiRoadmapRepository
    private lateinit var backButton: ImageButton
    private lateinit var heroLabel: TextView
    private lateinit var heroTitle: TextView
    private lateinit var heroBody: TextView
    private lateinit var confidenceMeta: TextView
    private lateinit var gradeMeta: TextView
    private lateinit var scoreBody: TextView
    private lateinit var rationaleBody: TextView
    private lateinit var roadmapButton: Button
    private lateinit var batchButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        applyPreparedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placement_result)
        runScreenEntranceAnimation()

        roadmapRepository = KanjiRoadmapRepository(this)
        backButton = findViewById(R.id.btnPlacementResultBack)
        heroLabel = findViewById(R.id.tvPlacementResultHeroLabel)
        heroTitle = findViewById(R.id.tvPlacementResultHeroTitle)
        heroBody = findViewById(R.id.tvPlacementResultHeroBody)
        confidenceMeta = findViewById(R.id.tvPlacementResultMetaPrimary)
        gradeMeta = findViewById(R.id.tvPlacementResultMetaSecondary)
        scoreBody = findViewById(R.id.tvPlacementResultScore)
        rationaleBody = findViewById(R.id.tvPlacementResultRationale)
        roadmapButton = findViewById(R.id.btnPlacementResultOpenRoadmap)
        batchButton = findViewById(R.id.btnPlacementResultStartBatch)

        backButton.setOnClickListener { finish() }
        bindResult()
        applyDepthStyling()
    }

    private fun bindResult() {
        val stageJlpt = intent.getStringExtra(EXTRA_STAGE_JLPT).orEmpty()
        val stageTitle = intent.getStringExtra(EXTRA_STAGE_TITLE).orEmpty()
        val gradeBand = intent.getStringExtra(EXTRA_STAGE_GRADE_BAND).orEmpty()
        val confidenceLabel = intent.getStringExtra(EXTRA_CONFIDENCE_LABEL).orEmpty()
        val rationale = intent.getStringExtra(EXTRA_RATIONALE).orEmpty()
        val totalCorrect = intent.getIntExtra(EXTRA_TOTAL_CORRECT, 0)
        val totalQuestions = intent.getIntExtra(EXTRA_TOTAL_QUESTIONS, 0)

        PlacementResultPrefs.save(
            this,
            SavedPlacementResult(
                jlptLevel = stageJlpt,
                stageTitle = stageTitle,
                gradeBand = gradeBand,
                confidenceLabel = confidenceLabel,
                totalCorrectAnswers = totalCorrect,
                totalQuestions = totalQuestions,
            )
        )

        heroLabel.text = getString(R.string.placement_result_hero_label)
        heroTitle.text = getString(R.string.placement_result_hero_title, stageJlpt)
        heroBody.text = getString(R.string.placement_result_hero_body, stageTitle)
        confidenceMeta.text = confidenceLabel
        gradeMeta.text = gradeBand
        scoreBody.text = getString(R.string.placement_result_score, totalCorrect, totalQuestions)
        rationaleBody.text = rationale

        roadmapButton.setOnClickListener {
            startActivity(Intent(this, RoadmapActivity::class.java))
        }

        val stageId = roadmapStageIdForJlpt(stageJlpt)
        val batch = roadmapRepository.getRecommendedBatchForStage(stageId, batchSize = 4).batch
        val batchEntry = batch.firstOrNull()
        batchButton.isEnabled = batchEntry != null
        batchButton.alpha = if (batchEntry != null) 1f else 0.5f
        batchButton.setOnClickListener(
            if (batchEntry == null) {
                null
            } else {
                android.view.View.OnClickListener {
                    startActivity(
                        KanjiDetailNavigator.buildDetailIntent(
                            context = this,
                            kanji = batchEntry.kanji,
                            meaningFallback = batchEntry.meaning,
                            jlptFallback = batchEntry.jlptLevel,
                        )
                    )
                }
            }
        )
    }

    private fun applyDepthStyling() {
        ThemeController.applyMainHeroDepth(findViewById(R.id.sectionPlacementResultHero), elevatedDp = 10f, defaultDp = 4f)
        ThemeController.applyMainCardDepth(findViewById(R.id.sectionPlacementResultSummary), elevatedDp = 6f, defaultDp = 3f)
        ThemeController.applyMainCardDepth(roadmapButton, elevatedDp = 4f, defaultDp = 2f)
        ThemeController.applyMainCardDepth(batchButton, elevatedDp = 1.5f, defaultDp = 0.5f)
        ThemeController.applyMainCardDepth(backButton, elevatedDp = 1.5f, defaultDp = 0.5f)
    }
}
