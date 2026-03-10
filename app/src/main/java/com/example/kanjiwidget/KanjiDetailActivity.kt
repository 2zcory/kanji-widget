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
    private lateinit var readingSummaryView: TextView
    private lateinit var meaningView: TextView
    private lateinit var noteView: TextView
    private lateinit var sourceView: TextView

    private var lastHtml: String? = null

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
        readingSummaryView = findViewById(R.id.tvDetailReadingSummary)
        meaningView = findViewById(R.id.tvDetailMeaning)
        noteView = findViewById(R.id.tvDetailNote)
        sourceView = findViewById(R.id.tvDetailSource)

        val kanji = intent.getStringExtra(EXTRA_KANJI)?.trim().orEmpty()
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
        readingSummaryView.text = buildReadingSummary(onyomi, kunyomi)
        meaningView.text = meaning.ifBlank { getString(R.string.stroke_order_meaning_placeholder) }
        noteView.text = note.ifBlank { getString(R.string.stroke_order_note_placeholder) }
        sourceView.text = if (source.isNotBlank()) {
            getString(R.string.stroke_order_source_value, source)
        } else {
            getString(R.string.stroke_order_source_value, getString(R.string.stroke_order_source_default))
        }
    }

    private fun buildReadingSummary(onyomi: String, kunyomi: String): String {
        val on = onyomi.ifBlank { getString(R.string.stroke_order_info_placeholder) }
        val kun = kunyomi.ifBlank { getString(R.string.stroke_order_info_placeholder) }
        return getString(R.string.stroke_order_reading_summary, on, kun)
    }
}
