package com.example.kanjiwidget

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.example.kanjiwidget.history.RecentKanjiStore
import com.example.kanjiwidget.stats.StudyTimeTracker
import com.example.kanjiwidget.widget.KanjiStrokeOrderClient
import kotlin.concurrent.thread

class KanjiDetailActivity : Activity() {

    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var jlptBadgeView: TextView
    private lateinit var statusView: TextView
    private lateinit var progressView: ProgressBar
    private lateinit var replayButton: Button
    private lateinit var webView: WebView
    private lateinit var onyomiView: TextView
    private lateinit var kunyomiView: TextView
    private lateinit var meaningView: TextView
    private lateinit var noteView: TextView
    private lateinit var sourceView: TextView
    private lateinit var todayTotalView: TextView
    private lateinit var todayOpenCountView: TextView
    private lateinit var todayKanjiView: TextView

    private var lastHtml: String? = null
    private var currentKanji: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kanji_detail)

        titleView = findViewById(R.id.tvDetailKanji)
        subtitleView = findViewById(R.id.tvDetailSubtitle)
        jlptBadgeView = findViewById(R.id.tvDetailJlptBadge)
        statusView = findViewById(R.id.tvDetailStatus)
        progressView = findViewById(R.id.progressStrokeOrder)
        replayButton = findViewById(R.id.btnReplayStrokeOrder)
        webView = findViewById(R.id.webStrokeOrder)
        onyomiView = findViewById(R.id.tvDetailOnyomi)
        kunyomiView = findViewById(R.id.tvDetailKunyomi)
        meaningView = findViewById(R.id.tvDetailMeaning)
        noteView = findViewById(R.id.tvDetailNote)
        sourceView = findViewById(R.id.tvDetailSource)
        todayTotalView = findViewById(R.id.tvTodayStudyTotal)
        todayOpenCountView = findViewById(R.id.tvTodayStudyOpenCount)
        todayKanjiView = findViewById(R.id.tvTodayStudyKanji)

        val kanji = intent.getStringExtra(EXTRA_KANJI)?.trim().orEmpty()
        currentKanji = kanji
        val source = intent.getStringExtra(EXTRA_SOURCE)?.trim().orEmpty()
        val jlpt = intent.getStringExtra(EXTRA_JLPT)?.trim().orEmpty()
        val onyomi = intent.getStringExtra(EXTRA_ONYOMI)?.trim().orEmpty()
        val kunyomi = intent.getStringExtra(EXTRA_KUNYOMI)?.trim().orEmpty()
        val meaning = intent.getStringExtra(EXTRA_MEANING)?.trim().orEmpty()
        val note = intent.getStringExtra(EXTRA_NOTE)?.trim().orEmpty()

        if (kanji.isBlank()) {
            titleView.text = getString(R.string.stroke_order_empty_title)
            subtitleView.text = getString(R.string.stroke_order_meaning_placeholder)
            jlptBadgeView.text = getString(R.string.stroke_order_badge_placeholder)
            bindStudyInfo("", "", "", "", "")
            refreshTodayStats()
            showError(getString(R.string.stroke_order_empty_message))
            return
        }

        titleView.text = kanji
        subtitleView.text = meaning.ifBlank { getString(R.string.stroke_order_meaning_placeholder) }
        jlptBadgeView.text = if (jlpt.isNotBlank()) "JLPT $jlpt" else getString(R.string.stroke_order_badge_placeholder)
        bindStudyInfo(
            onyomi = onyomi,
            kunyomi = kunyomi,
            meaning = meaning,
            note = note,
            source = source
        )
        refreshTodayStats()

        configureWebView()

        replayButton.setOnClickListener {
            if (lastHtml != null) {
                replayAnimation()
            } else {
                loadStrokeOrder(kanji)
            }
        }

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
        StudyTimeTracker.stopSession(this)
        super.onStop()
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

    companion object {
        const val EXTRA_KANJI = "extra_kanji"
        const val EXTRA_SOURCE = "extra_source"
        const val EXTRA_JLPT = "extra_jlpt"
        const val EXTRA_ONYOMI = "extra_onyomi"
        const val EXTRA_KUNYOMI = "extra_kunyomi"
        const val EXTRA_MEANING = "extra_meaning"
        const val EXTRA_NOTE = "extra_note"
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
        meaningView.text = meaning.ifBlank { getString(R.string.stroke_order_meaning_placeholder) }
        noteView.text = note.ifBlank { getString(R.string.stroke_order_note_placeholder) }
        sourceView.text = if (source.isNotBlank()) {
            getString(R.string.stroke_order_source_value, source)
        } else {
            getString(R.string.stroke_order_source_value, getString(R.string.stroke_order_source_default))
        }
    }

    private fun refreshTodayStats() {
        val totalMs = StudyTimeTracker.getTodayTotalMs(this)
        val openCount = StudyTimeTracker.getTodayOpenCount(this)
        val kanjiMs = StudyTimeTracker.getTodayKanjiMs(this, currentKanji)
        todayTotalView.text = formatDuration(totalMs)
        todayOpenCountView.text = getString(R.string.today_study_open_count_value, openCount)
        todayKanjiView.text = formatDuration(kanjiMs)
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return if (minutes > 0L) {
            getString(R.string.study_duration_minutes_seconds, minutes, seconds)
        } else {
            getString(R.string.study_duration_seconds, seconds)
        }
    }
}
