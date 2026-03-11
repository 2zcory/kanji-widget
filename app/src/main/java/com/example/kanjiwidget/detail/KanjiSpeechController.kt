package com.example.kanjiwidget.detail

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class KanjiSpeechController(
    context: Context,
    private val onStateChanged: (SpeechInitState) -> Unit = {},
) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var initState: SpeechInitState = SpeechInitState.Pending

    override fun onInit(status: Int) {
        val engine = tts
        if (status != TextToSpeech.SUCCESS || engine == null) {
            initState = SpeechInitState.Unavailable
            onStateChanged(initState)
            return
        }

        val locale = Locale.JAPANESE
        val availability = engine.isLanguageAvailable(locale)
        initState = if (availability >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = locale
            SpeechInitState.Ready
        } else {
            SpeechInitState.Unavailable
        }
        onStateChanged(initState)
    }

    fun isReady(): Boolean = initState == SpeechInitState.Ready

    fun speak(text: String) {
        val engine = tts ?: return
        if (!isReady() || text.isBlank()) return
        engine.stop()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kanji-detail-audio")
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        initState = SpeechInitState.Unavailable
    }
}

enum class SpeechInitState {
    Pending,
    Ready,
    Unavailable,
}
