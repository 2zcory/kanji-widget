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
    private lateinit var statusView: TextView
    private lateinit var progressView: ProgressBar
    private lateinit var replayButton: Button
    private lateinit var webView: WebView

    private var lastHtml: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kanji_detail)

        titleView = findViewById(R.id.tvDetailKanji)
        subtitleView = findViewById(R.id.tvDetailSubtitle)
        statusView = findViewById(R.id.tvDetailStatus)
        progressView = findViewById(R.id.progressStrokeOrder)
        replayButton = findViewById(R.id.btnReplayStrokeOrder)
        webView = findViewById(R.id.webStrokeOrder)

        val kanji = intent.getStringExtra(EXTRA_KANJI)?.trim().orEmpty()
        val source = intent.getStringExtra(EXTRA_SOURCE)?.trim().orEmpty()

        if (kanji.isBlank()) {
            titleView.text = getString(R.string.stroke_order_empty_title)
            subtitleView.text = getString(R.string.stroke_order_empty_subtitle)
            showError(getString(R.string.stroke_order_empty_message))
            return
        }

        titleView.text = kanji
        subtitleView.text = if (source.isNotBlank()) {
            getString(R.string.stroke_order_subtitle_with_source, source)
        } else {
            getString(R.string.stroke_order_subtitle)
        }

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
    }
}
