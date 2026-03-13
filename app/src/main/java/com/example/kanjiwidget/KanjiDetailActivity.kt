package com.example.kanjiwidget

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.LayoutInflater
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.kanjiwidget.detail.KanjiCompoundEntry
import com.example.kanjiwidget.detail.KanjiCompoundRepository
import com.example.kanjiwidget.detail.KanjiSpeechController
import com.example.kanjiwidget.detail.UsageHintKey
import com.example.kanjiwidget.history.RecentKanjiStore
import com.example.kanjiwidget.stats.StudyTimeTracker
import com.example.kanjiwidget.widget.KanjiStrokeOrderClient
import com.example.kanjiwidget.widget.formatJlptLabel
import com.example.kanjiwidget.widget.normalizeMeaning
import kotlin.concurrent.thread

class KanjiDetailActivity : AppCompatActivity() {

    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var heroMetaView: TextView
    private lateinit var jlptBadgeView: TextView
    private lateinit var statusView: TextView
    private lateinit var progressView: ProgressBar
    private lateinit var replayButton: Button
    private lateinit var nextRandomButton: Button
    private lateinit var webView: WebView
    private lateinit var onyomiView: TextView
    private lateinit var kunyomiView: TextView
    private lateinit var playMainReadingButton: ImageButton
    private lateinit var meaningView: TextView
    private lateinit var noteView: TextView
    private lateinit var sourceView: TextView
    private lateinit var todayTotalView: TextView
    private lateinit var todayOpenCountView: TextView
    private lateinit var todayKanjiView: TextView
    private lateinit var compoundsSection: View
    private lateinit var compoundsContainer: LinearLayout

    private var lastHtml: String? = null
    private var currentKanji: String = ""
    private lateinit var compoundRepository: KanjiCompoundRepository
    private lateinit var speechController: KanjiSpeechController
    private var mainReadingToSpeak: String? = null
    private var renderedCompounds: List<KanjiCompoundEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kanji_detail)

        compoundRepository = KanjiCompoundRepository(this)
        speechController = KanjiSpeechController(this) {
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                bindMainReadingAudio(mainReadingToSpeak)
                renderCompounds(renderedCompounds)
            }
        }
        titleView = findViewById(R.id.tvDetailKanji)
        subtitleView = findViewById(R.id.tvDetailSubtitle)
        heroMetaView = findViewById(R.id.tvDetailHeroMeta)
        jlptBadgeView = findViewById(R.id.tvDetailJlptBadge)
        statusView = findViewById(R.id.tvDetailStatus)
        progressView = findViewById(R.id.progressStrokeOrder)
        replayButton = findViewById(R.id.btnReplayStrokeOrder)
        nextRandomButton = findViewById(R.id.btnNextRandomKanji)
        webView = findViewById(R.id.webStrokeOrder)
        onyomiView = findViewById(R.id.tvDetailOnyomi)
        kunyomiView = findViewById(R.id.tvDetailKunyomi)
        playMainReadingButton = findViewById(R.id.btnPlayMainReading)
        meaningView = findViewById(R.id.tvDetailMeaning)
        noteView = findViewById(R.id.tvDetailNote)
        sourceView = findViewById(R.id.tvDetailSource)
        todayTotalView = findViewById(R.id.tvTodayStudyTotal)
        todayOpenCountView = findViewById(R.id.tvTodayStudyOpenCount)
        todayKanjiView = findViewById(R.id.tvTodayStudyKanji)
        compoundsSection = findViewById(R.id.sectionCompoundExamples)
        compoundsContainer = findViewById(R.id.containerCompoundExamples)

        val kanji = intent.getStringExtra(EXTRA_KANJI)?.trim().orEmpty()
        currentKanji = kanji
        val source = intent.getStringExtra(EXTRA_SOURCE)?.trim().orEmpty()
        val jlpt = intent.getStringExtra(EXTRA_JLPT)?.trim().orEmpty()
        val onyomi = intent.getStringExtra(EXTRA_ONYOMI)?.trim().orEmpty()
        val kunyomi = intent.getStringExtra(EXTRA_KUNYOMI)?.trim().orEmpty()
        val meaning = intent.getStringExtra(EXTRA_MEANING)?.trim().orEmpty()
        val note = intent.getStringExtra(EXTRA_NOTE)?.trim().orEmpty()
        val strokeCount = intent.getIntExtra(EXTRA_STROKE_COUNT, 0).takeIf { it > 0 }
        val grade = intent.getIntExtra(EXTRA_GRADE, 0).takeIf { it > 0 }
        val frequency = intent.getIntExtra(EXTRA_FREQUENCY, 0).takeIf { it > 0 }

        if (kanji.isBlank()) {
            titleView.text = getString(R.string.stroke_order_empty_title)
            subtitleView.text = getString(R.string.stroke_order_meaning_placeholder)
            bindHeroMetadata(null, null, null)
            jlptBadgeView.text = getString(R.string.stroke_order_badge_placeholder)
            bindStudyInfo("", "", "", "", "")
            bindMainReadingAudio(null)
            renderCompounds(emptyList())
            refreshTodayStats()
            showError(getString(R.string.stroke_order_empty_message))
            return
        }

        titleView.text = kanji
        subtitleView.text = normalizeMeaning(meaning)
            ?: getString(R.string.stroke_order_meaning_placeholder)
        bindHeroMetadata(strokeCount, grade, frequency)
        jlptBadgeView.text = formatJlptLabel(this, jlpt, R.string.stroke_order_badge_placeholder)
        bindStudyInfo(
            onyomi = onyomi,
            kunyomi = kunyomi,
            meaning = meaning,
            note = note,
            source = source
        )
        bindMainReadingAudio(selectMainReading(onyomi, kunyomi))
        bindCompounds(kanji)
        refreshTodayStats()

        configureWebView()

        replayButton.setOnClickListener {
            if (lastHtml != null) {
                replayAnimation()
            } else {
                loadStrokeOrder(kanji)
            }
        }
        bindNextRandomAction()

        loadStrokeOrder(kanji)
    }

    override fun onStart() {
        super.onStart()
        if (currentKanji.isNotBlank()) {
            RecentKanjiStore.recordViewedKanji(this, currentKanji)
            StudyTimeTracker.startSession(this, currentKanji)
            refreshTodayStats()
        }
    }

    override fun onStop() {
        speechController.stop()
        StudyTimeTracker.stopSession(this)
        super.onStop()
    }

    override fun onDestroy() {
        speechController.release()
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.settings.apply {
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }
    }

    private fun loadStrokeOrder(kanji: String) {
        showLoading()
        thread(name = "stroke-order-loader") {
            val svg = KanjiStrokeOrderClient.fetchSvg(kanji)
            val html = svg?.let { KanjiStrokeOrderClient.buildAnimatedHtml(it, kanji) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (html == null) {
                    showError(getString(R.string.stroke_order_load_error, kanji))
                } else {
                    lastHtml = html
                    statusView.visibility = View.GONE
                    progressView.visibility = View.GONE
                    replayButton.isEnabled = true
                    webView.visibility = View.VISIBLE
                    webView.loadDataWithBaseURL(
                        "https://raw.githubusercontent.com/",
                        html,
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            }
        }
    }

    private fun replayAnimation() {
        statusView.visibility = View.GONE
        webView.visibility = View.VISIBLE
        replayButton.isEnabled = true
        webView.evaluateJavascript(
            "window.restartStrokeOrder && window.restartStrokeOrder();",
            null
        )
    }

    private fun showLoading() {
        statusView.visibility = View.VISIBLE
        statusView.text = getString(R.string.stroke_order_loading)
        progressView.visibility = View.VISIBLE
        replayButton.isEnabled = false
        webView.visibility = View.INVISIBLE
    }

    private fun showError(message: String) {
        lastHtml = null
        statusView.visibility = View.VISIBLE
        statusView.text = message
        progressView.visibility = View.GONE
        replayButton.isEnabled = true
        webView.visibility = View.INVISIBLE
    }

    private fun bindNextRandomAction() {
        val catalog = com.example.kanjiwidget.widget.KanjiWidgetPrefs.getKanjiCatalog(this)
        val isAvailable = catalog.isNotEmpty()
        nextRandomButton.isEnabled = isAvailable
        nextRandomButton.alpha = if (isAvailable) 1f else 0.5f
        nextRandomButton.setOnClickListener(
            if (!isAvailable) {
                null
            } else {
                View.OnClickListener {
                    val nextIntent = KanjiDetailNavigator.buildRandomDetailIntent(
                        context = this,
                        catalog = catalog,
                        currentKanji = currentKanji,
                    ) ?: return@OnClickListener
                    startActivity(nextIntent)
                    finish()
                }
            }
        )
    }

    companion object {
        const val EXTRA_KANJI = "extra_kanji"
        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_JLPT = "extra_jlpt"
        const val EXTRA_ONYOMI = "extra_onyomi"
        const val EXTRA_KUNYOMI = "extra_kunyomi"
        const val EXTRA_MEANING = "extra_meaning"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_STROKE_COUNT = "extra_stroke_count"
        const val EXTRA_GRADE = "extra_grade"
        const val EXTRA_FREQUENCY = "extra_frequency"
    }

    private fun bindHeroMetadata(
        strokeCount: Int?,
        grade: Int?,
        frequency: Int?,
    ) {
        val parts = buildList {
            strokeCount?.let {
                add(resources.getQuantityString(R.plurals.stroke_count, it, it))
            }
            grade?.let { add(getString(R.string.stroke_order_meta_grade, it)) }
            frequency?.let { add(getString(R.string.stroke_order_meta_frequency, it)) }
        }
        if (parts.isEmpty()) {
            heroMetaView.visibility = View.GONE
            heroMetaView.text = ""
        } else {
            heroMetaView.visibility = View.VISIBLE
            heroMetaView.text = parts.joinToString(getString(R.string.bullet_separator))
        }
    }

    private fun bindStudyInfo(
        onyomi: String,
        kunyomi: String,
        meaning: String,
        note: String,
        source: String,
    ) {
        onyomiView.text = onyomi.ifBlank { getString(R.string.stroke_order_info_placeholder) }
        kunyomiView.text = kunyomi.ifBlank { getString(R.string.stroke_order_info_placeholder) }
        meaningView.text = normalizeMeaning(meaning)
            ?: getString(R.string.stroke_order_meaning_placeholder)
        noteView.text = note.ifBlank { getString(R.string.stroke_order_note_placeholder) }
        sourceView.text = if (source.isNotBlank()) {
            getString(R.string.stroke_order_source_value, source)
        } else {
            getString(R.string.stroke_order_source_value, getString(R.string.stroke_order_source_default))
        }
    }

    private fun bindMainReadingAudio(reading: String?) {
        mainReadingToSpeak = reading?.takeIf { isPlayableReading(it) }
        val hasUsableReading = !mainReadingToSpeak.isNullOrBlank()
        val isReady = speechController.isReady()
        playMainReadingButton.visibility = if (hasUsableReading) View.VISIBLE else View.GONE
        playMainReadingButton.isEnabled = hasUsableReading && isReady
        playMainReadingButton.alpha = if (hasUsableReading && isReady) 1f else 0.5f
        playMainReadingButton.setOnClickListener(
            if (!hasUsableReading || !isReady) {
                null
            } else {
                View.OnClickListener {
                    speechController.speak(mainReadingToSpeak.orEmpty())
                }
            }
        )
    }

    private fun bindCompounds(kanji: String) {
        val cachedEntries = compoundRepository.getCachedCompounds(kanji)
        renderCompounds(cachedEntries)

        if (!compoundRepository.shouldRefreshCompounds(kanji)) return

        thread(name = "compound-loader") {
            val refreshed = compoundRepository.refreshCompounds(kanji)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                renderCompounds(refreshed ?: cachedEntries)
            }
        }
    }

    private fun renderCompounds(entries: List<KanjiCompoundEntry>) {
        renderedCompounds = entries
        compoundsContainer.removeAllViews()
        if (entries.isEmpty()) {
            compoundsSection.visibility = View.GONE
            return
        }

        val inflater = LayoutInflater.from(this)
        entries.forEach { entry ->
            val row = inflater.inflate(R.layout.item_compound_example, compoundsContainer, false)
            row.findViewById<TextView>(R.id.tvCompoundWritten).text = entry.written
            row.findViewById<TextView>(R.id.tvCompoundReading).text = entry.reading.ifBlank {
                getString(R.string.stroke_order_info_placeholder)
            }
            row.findViewById<TextView>(R.id.tvCompoundMeaning).text = entry.meaning
            val usageHint = formatUsageHint(entry.usageHintKey)
            row.findViewById<TextView>(R.id.tvCompoundUsage).text =
                getString(R.string.detail_compound_usage_value, usageHint)
            val playButton = row.findViewById<ImageButton>(R.id.btnPlayCompoundReading)
            val canPlay = isPlayableReading(entry.reading)
            val isReady = speechController.isReady()
            playButton.visibility = if (canPlay) View.VISIBLE else View.GONE
            playButton.isEnabled = canPlay && isReady
            playButton.alpha = if (canPlay && isReady) 1f else 0.5f
            playButton.setOnClickListener(
                if (!canPlay || !isReady) {
                    null
                } else {
                    View.OnClickListener {
                        speechController.speak(entry.reading)
                    }
                }
            )
            compoundsContainer.addView(row)
        }
        compoundsSection.visibility = View.VISIBLE
    }

    private fun selectMainReading(
        onyomi: String,
        kunyomi: String,
    ): String? {
        return onyomi.takeIf { isPlayableReading(it) }
            ?: kunyomi.takeIf { isPlayableReading(it) }
    }

    private fun isPlayableReading(value: String?): Boolean {
        val normalized = value?.trim()
        return !normalized.isNullOrBlank() &&
            normalized != "-" &&
            normalized != getString(R.string.stroke_order_info_placeholder)
    }

    private fun refreshTodayStats() {
        val totalMs = StudyTimeTracker.getTodayTotalMs(this)
        val openCount = StudyTimeTracker.getTodayOpenCount(this)
        val kanjiMs = StudyTimeTracker.getTodayKanjiMs(this, currentKanji)
        todayTotalView.text = formatDuration(totalMs)
        todayOpenCountView.text = resources.getQuantityString(
            R.plurals.open_count,
            openCount,
            openCount
        )
        todayKanjiView.text = formatDuration(kanjiMs)
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        val minutesText = resources.getQuantityString(
            R.plurals.duration_minutes,
            minutes.toInt(),
            minutes
        )
        val secondsText = resources.getQuantityString(
            R.plurals.duration_seconds,
            seconds.toInt(),
            seconds
        )
        return if (minutes > 0L) {
            getString(R.string.duration_minutes_seconds, minutesText, secondsText)
        } else {
            secondsText
        }
    }

    private fun formatUsageHint(key: UsageHintKey): String {
        return when (key) {
            UsageHintKey.NEWS_HEAVY -> getString(R.string.compound_usage_news_heavy)
            UsageHintKey.COMMON_WORD -> getString(R.string.compound_usage_common_word)
            UsageHintKey.REFERENCE_TERM -> getString(R.string.compound_usage_reference_term)
            UsageHintKey.STUDY_WORD -> getString(R.string.compound_usage_study_word)
        }
    }
}
