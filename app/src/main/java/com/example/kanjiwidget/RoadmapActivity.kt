package com.example.kanjiwidget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.kanjiwidget.roadmap.KanjiCompletionPrefs
import com.example.kanjiwidget.roadmap.KanjiRoadmapRepository
import com.example.kanjiwidget.roadmap.KanjiRoadmapSnapshot
import com.example.kanjiwidget.roadmap.KanjiRoadmapStageProgress
import com.example.kanjiwidget.theme.ThemeController
import com.example.kanjiwidget.widget.KanjiEntry

class RoadmapActivity : ThemedActivity() {

    private lateinit var repository: KanjiRoadmapRepository
    private lateinit var backButton: ImageButton
    private lateinit var heroLabel: TextView
    private lateinit var heroTitle: TextView
    private lateinit var heroBody: TextView
    private lateinit var heroMetaPrimary: TextView
    private lateinit var heroMetaSecondary: TextView
    private lateinit var batchEmptyView: TextView
    private lateinit var batchContainer: LinearLayout
    private lateinit var stageContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        applyPreparedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_roadmap)
        runScreenEntranceAnimation()

        repository = KanjiRoadmapRepository(this)
        backButton = findViewById(R.id.btnRoadmapBack)
        heroLabel = findViewById(R.id.tvRoadmapHeroLabel)
        heroTitle = findViewById(R.id.tvRoadmapHeroTitle)
        heroBody = findViewById(R.id.tvRoadmapHeroBody)
        heroMetaPrimary = findViewById(R.id.tvRoadmapMetaPrimary)
        heroMetaSecondary = findViewById(R.id.tvRoadmapMetaSecondary)
        batchEmptyView = findViewById(R.id.tvRoadmapBatchEmpty)
        batchContainer = findViewById(R.id.containerRoadmapBatch)
        stageContainer = findViewById(R.id.containerRoadmapStages)

        backButton.setOnClickListener { finish() }
        applyDepthStyling()
    }

    override fun onResume() {
        super.onResume()
        bindRoadmap()
    }

    private fun bindRoadmap() {
        val entries = repository.loadKnownEntries()
        val snapshot = repository.buildSnapshot(entries)
        val recommendation = repository.getRecommendedNextBatch(batchSize = 4, entries = entries)
        bindHero(snapshot)
        bindRecommendedBatch(recommendation.batch, snapshot)
        bindStages(snapshot)
    }

    private fun bindHero(snapshot: KanjiRoadmapSnapshot) {
        val currentStage = snapshot.currentStage
        val nextStage = snapshot.nextStage
        val hasData = snapshot.stages.any { it.totalCount > 0 }
        heroLabel.text = getString(R.string.roadmap_hero_label)
        when {
            !hasData -> {
                heroTitle.text = getString(R.string.roadmap_empty_title)
                heroBody.text = getString(R.string.roadmap_empty_body)
                updatePill(heroMetaPrimary, getString(R.string.roadmap_empty_meta_primary))
                updatePill(heroMetaSecondary, null)
            }

            currentStage == null -> {
                heroTitle.text = getString(R.string.roadmap_complete_title)
                heroBody.text = getString(R.string.roadmap_complete_body)
                updatePill(heroMetaPrimary, getString(R.string.roadmap_complete_meta_primary))
                updatePill(heroMetaSecondary, null)
            }

            else -> {
                heroTitle.text = currentStage.definition.title
                heroBody.text = getString(
                    R.string.roadmap_hero_body_progress,
                    currentStage.completedCount,
                    currentStage.totalCount,
                    currentStage.definition.gradeBandLabel
                )
                updatePill(
                    heroMetaPrimary,
                    getString(R.string.roadmap_meta_current, currentStage.definition.jlptLevel)
                )
                updatePill(
                    heroMetaSecondary,
                    nextStage?.let {
                        getString(R.string.roadmap_meta_next, it.definition.jlptLevel)
                    } ?: getString(R.string.roadmap_meta_final_stage)
                )
            }
        }
    }

    private fun bindRecommendedBatch(
        batch: List<KanjiEntry>,
        snapshot: KanjiRoadmapSnapshot,
    ) {
        batchContainer.removeAllViews()
        batchEmptyView.visibility = if (batch.isEmpty()) View.VISIBLE else View.GONE
        batchContainer.visibility = if (batch.isEmpty()) View.GONE else View.VISIBLE

        if (batch.isEmpty()) {
            batchEmptyView.text = if (snapshot.stages.any { it.totalCount > 0 }) {
                getString(R.string.roadmap_batch_empty_complete)
            } else {
                getString(R.string.roadmap_batch_empty_no_data)
            }
            return
        }

        val inflater = LayoutInflater.from(this)
        batch.forEach { entry ->
            val row = inflater.inflate(R.layout.item_roadmap_batch, batchContainer, false)
            row.findViewById<TextView>(R.id.tvRoadmapBatchKanji).text = entry.kanji
            row.findViewById<TextView>(R.id.tvRoadmapBatchMeaning).text = entry.meaning
            row.findViewById<TextView>(R.id.tvRoadmapBatchMeta).text = getString(
                R.string.roadmap_batch_meta,
                entry.jlptLevel,
                entry.grade?.toString() ?: getString(R.string.roadmap_grade_unknown)
            )

            val completeButton = row.findViewById<Button>(R.id.btnRoadmapBatchToggle)
            val isCompleted = KanjiCompletionPrefs.isCompleted(this, entry.kanji)
            completeButton.text = if (isCompleted) {
                getString(R.string.roadmap_action_completed)
            } else {
                getString(R.string.roadmap_action_mark_complete)
            }
            completeButton.setOnClickListener {
                KanjiCompletionPrefs.setCompleted(this, entry.kanji, !isCompleted)
                bindRoadmap()
            }

            row.setOnClickListener {
                startActivity(
                    KanjiDetailNavigator.buildDetailIntent(
                        context = this,
                        kanji = entry.kanji,
                        meaningFallback = entry.meaning,
                        jlptFallback = entry.jlptLevel,
                    )
                )
            }
            ThemeController.applyMainCardDepth(row, elevatedDp = 3f, defaultDp = 1f)
            batchContainer.addView(row)
        }
    }

    private fun bindStages(snapshot: KanjiRoadmapSnapshot) {
        stageContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        snapshot.stages.forEach { stage ->
            val row = inflater.inflate(R.layout.item_roadmap_stage, stageContainer, false)
            row.findViewById<TextView>(R.id.tvRoadmapStageTitle).text = stage.definition.title
            row.findViewById<TextView>(R.id.tvRoadmapStageState).text = resolveStageState(snapshot, stage)
            row.findViewById<TextView>(R.id.tvRoadmapStageMeta).text = getString(
                R.string.roadmap_stage_meta,
                stage.completedCount,
                stage.totalCount,
                stage.definition.gradeBandLabel
            )
            row.findViewById<ProgressBar>(R.id.progressRoadmapStage).apply {
                max = stage.totalCount.coerceAtLeast(1)
                progress = stage.completedCount.coerceIn(0, max)
            }
            ThemeController.applyMainCardDepth(row, elevatedDp = 2f, defaultDp = 1f)
            stageContainer.addView(row)
        }
    }

    private fun resolveStageState(
        snapshot: KanjiRoadmapSnapshot,
        stage: KanjiRoadmapStageProgress,
    ): String {
        return when {
            stage.totalCount == 0 -> getString(R.string.roadmap_stage_state_unavailable)
            stage.completedCount >= stage.totalCount -> getString(R.string.roadmap_stage_state_done)
            snapshot.currentStage?.definition?.id == stage.definition.id -> getString(R.string.roadmap_stage_state_current)
            snapshot.nextStage?.definition?.id == stage.definition.id -> getString(R.string.roadmap_stage_state_next)
            else -> getString(R.string.roadmap_stage_state_later)
        }
    }

    private fun applyDepthStyling() {
        ThemeController.applyMainHeroDepth(findViewById(R.id.sectionRoadmapHero), elevatedDp = 10f, defaultDp = 4f)
        ThemeController.applyMainCardDepth(findViewById(R.id.sectionRoadmapBatch), elevatedDp = 6f, defaultDp = 3f)
        ThemeController.applyMainCardDepth(findViewById(R.id.sectionRoadmapStages), elevatedDp = 6f, defaultDp = 3f)
        ThemeController.applyMainCardDepth(backButton, elevatedDp = 1.5f, defaultDp = 0.5f)
    }

    private fun updatePill(view: TextView, text: CharSequence?) {
        if (text.isNullOrBlank()) {
            view.visibility = View.GONE
            return
        }
        view.text = text
        view.visibility = View.VISIBLE
    }
}
