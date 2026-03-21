package com.example.kanjiwidget

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.example.kanjiwidget.placement.KanjiPlacementRepository
import com.example.kanjiwidget.placement.PlacementAnswer
import com.example.kanjiwidget.placement.PlacementQuestionType
import com.example.kanjiwidget.placement.PlacementTestSession
import com.example.kanjiwidget.theme.ThemeController

class PlacementTestActivity : ThemedActivity() {

    private lateinit var repository: KanjiPlacementRepository
    private lateinit var backButton: ImageButton
    private lateinit var heroLabel: TextView
    private lateinit var heroTitle: TextView
    private lateinit var heroBody: TextView
    private lateinit var progressMeta: TextView
    private lateinit var stageMeta: TextView
    private lateinit var questionTypeLabel: TextView
    private lateinit var promptKanji: TextView
    private lateinit var promptText: TextView
    private lateinit var answerButtons: List<Button>
    private lateinit var nextButton: Button
    private lateinit var emptyTitle: TextView
    private lateinit var emptyBody: TextView

    private lateinit var session: PlacementTestSession
    private val selectedAnswers = linkedMapOf<String, String>()
    private var questionIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        applyPreparedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placement_test)
        runScreenEntranceAnimation()

        repository = KanjiPlacementRepository(this)
        backButton = findViewById(R.id.btnPlacementBack)
        heroLabel = findViewById(R.id.tvPlacementHeroLabel)
        heroTitle = findViewById(R.id.tvPlacementHeroTitle)
        heroBody = findViewById(R.id.tvPlacementHeroBody)
        progressMeta = findViewById(R.id.tvPlacementProgressMeta)
        stageMeta = findViewById(R.id.tvPlacementStageMeta)
        questionTypeLabel = findViewById(R.id.tvPlacementQuestionType)
        promptKanji = findViewById(R.id.tvPlacementPromptKanji)
        promptText = findViewById(R.id.tvPlacementPromptText)
        nextButton = findViewById(R.id.btnPlacementNext)
        emptyTitle = findViewById(R.id.tvPlacementEmptyTitle)
        emptyBody = findViewById(R.id.tvPlacementEmptyBody)
        answerButtons = listOf(
            findViewById(R.id.btnPlacementAnswerOne),
            findViewById(R.id.btnPlacementAnswerTwo),
            findViewById(R.id.btnPlacementAnswerThree),
            findViewById(R.id.btnPlacementAnswerFour),
        )

        backButton.setOnClickListener { finish() }
        session = repository.buildSession()
        applyDepthStyling()
        bindCurrentQuestion()
    }

    private fun bindCurrentQuestion() {
        val questions = session.questions
        val question = questions.getOrNull(questionIndex)
        val hasQuestion = question != null
        findViewById<android.view.View>(R.id.sectionPlacementQuestion).visibility =
            if (hasQuestion) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<android.view.View>(R.id.sectionPlacementEmpty).visibility =
            if (hasQuestion) android.view.View.GONE else android.view.View.VISIBLE

        if (question == null) {
            heroLabel.text = getString(R.string.placement_test_hero_label)
            heroTitle.text = getString(R.string.placement_test_empty_title)
            heroBody.text = getString(R.string.placement_test_empty_body)
            progressMeta.visibility = android.view.View.GONE
            stageMeta.visibility = android.view.View.GONE
            emptyTitle.text = getString(R.string.placement_test_empty_title)
            emptyBody.text = getString(R.string.placement_test_empty_body)
            nextButton.visibility = android.view.View.GONE
            return
        }

        heroLabel.text = getString(R.string.placement_test_hero_label)
        heroTitle.text = getString(R.string.placement_test_hero_title)
        heroBody.text = getString(R.string.placement_test_hero_body)
        progressMeta.text = getString(
            R.string.placement_test_progress_meta,
            questionIndex + 1,
            questions.size,
        )
        progressMeta.visibility = android.view.View.VISIBLE
        stageMeta.text = getString(
            R.string.placement_test_stage_meta,
            question.stage.jlptLevel,
            question.stage.gradeBandLabel,
        )
        stageMeta.visibility = android.view.View.VISIBLE
        questionTypeLabel.text = when (question.type) {
            PlacementQuestionType.MEANING -> getString(R.string.placement_question_type_meaning)
            PlacementQuestionType.READING -> getString(R.string.placement_question_type_reading)
        }
        promptKanji.text = question.promptKanji
        promptText.text = question.promptText

        val selectedAnswer = selectedAnswers[question.id]
        answerButtons.zip(question.options).forEach { (button, option) ->
            button.text = option
            button.isSelected = option == selectedAnswer
            ThemeController.applyMainCardDepth(button, elevatedDp = 2f, defaultDp = 0.5f)
            button.setOnClickListener {
                selectedAnswers[question.id] = option
                bindCurrentQuestion()
            }
        }

        nextButton.visibility = android.view.View.VISIBLE
        nextButton.text = if (questionIndex == questions.lastIndex) {
            getString(R.string.placement_test_action_finish)
        } else {
            getString(R.string.placement_test_action_next)
        }
        nextButton.isEnabled = selectedAnswer != null
        nextButton.alpha = if (selectedAnswer != null) 1f else 0.5f
        nextButton.setOnClickListener {
            if (questionIndex == questions.lastIndex) {
                openResult()
            } else {
                questionIndex += 1
                bindCurrentQuestion()
            }
        }
    }

    private fun openResult() {
        val result = repository.evaluateSession(
            session = session,
            answers = selectedAnswers.map { PlacementAnswer(it.key, it.value) },
        ) ?: return
        startActivity(
            Intent(this, PlacementResultActivity::class.java).apply {
                putExtra(PlacementResultActivity.EXTRA_STAGE_JLPT, result.recommendedStage.jlptLevel)
                putExtra(PlacementResultActivity.EXTRA_STAGE_TITLE, result.recommendedStage.title)
                putExtra(PlacementResultActivity.EXTRA_STAGE_GRADE_BAND, result.recommendedStage.gradeBandLabel)
                putExtra(PlacementResultActivity.EXTRA_CONFIDENCE_LABEL, result.confidenceLabel)
                putExtra(PlacementResultActivity.EXTRA_RATIONALE, result.rationale)
                putExtra(PlacementResultActivity.EXTRA_TOTAL_CORRECT, result.totalCorrectAnswers)
                putExtra(PlacementResultActivity.EXTRA_TOTAL_QUESTIONS, result.totalQuestions)
            }
        )
    }

    private fun applyDepthStyling() {
        ThemeController.applyMainHeroDepth(findViewById(R.id.sectionPlacementHero), elevatedDp = 10f, defaultDp = 4f)
        ThemeController.applyMainCardDepth(findViewById(R.id.sectionPlacementQuestion), elevatedDp = 6f, defaultDp = 3f)
        ThemeController.applyMainCardDepth(findViewById(R.id.sectionPlacementEmpty), elevatedDp = 6f, defaultDp = 3f)
        ThemeController.applyMainCardDepth(nextButton, elevatedDp = 4f, defaultDp = 2f)
        ThemeController.applyMainCardDepth(backButton, elevatedDp = 1.5f, defaultDp = 0.5f)
    }
}
