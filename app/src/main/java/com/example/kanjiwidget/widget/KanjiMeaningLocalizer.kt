package com.example.kanjiwidget.widget

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.kanjiwidget.detail.KanjiCompoundEntry
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlin.concurrent.thread
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

internal fun resolveDisplayMeaning(context: Context, entry: KanjiEntry?): String? {
    if (entry == null) return null
    return resolveDisplayMeaning(context, entry.meaning, entry.meaningVi)
}

internal fun resolveDisplayMeaning(
    context: Context,
    sourceMeaning: String?,
    localizedVietnameseMeaning: String?,
): String? {
    return if (shouldPreferVietnameseMeaning(context)) {
        normalizeMeaning(localizedVietnameseMeaning) ?: normalizeMeaning(sourceMeaning)
    } else {
        normalizeMeaning(sourceMeaning) ?: normalizeMeaning(localizedVietnameseMeaning)
    }
}

internal fun shouldPreferVietnameseMeaning(context: Context): Boolean {
    val localizedContext = ContextCompat.getContextForLanguage(context)
    val locale = localizedContext.resources.configuration.locales[0]
    return locale.language.equals("vi", ignoreCase = true)
}

internal fun needsVietnameseMeaning(context: Context, entry: KanjiEntry?): Boolean {
    return shouldPreferVietnameseMeaning(context) &&
        entry != null &&
        normalizeMeaning(entry.meaning) != null &&
        normalizeMeaning(entry.meaningVi) == null
}

internal fun localizeMeaningIfNeededAsync(
    context: Context,
    kanji: String,
    entry: KanjiEntry,
    onUpdated: (KanjiEntry) -> Unit,
) {
    if (!needsVietnameseMeaning(context, entry)) return
    thread(name = "meaning-localizer-$kanji") {
        val localized = runCatching { localizeMeaningIfNeeded(context, entry) }.getOrNull() ?: return@thread
        if (localized == entry) return@thread
        KanjiWidgetPrefs.saveRemoteEntry(context, kanji, localized)
        onUpdated(localized)
    }
}

internal fun localizeMeaningIfNeeded(context: Context, entry: KanjiEntry): KanjiEntry {
    if (!needsVietnameseMeaning(context, entry)) return entry

    val normalizedMeaning = normalizeMeaning(entry.meaning) ?: return entry
    val translated = translateMeaningToVietnamese(normalizedMeaning) ?: return entry
    val normalizedVietnamese = normalizeMeaning(translated) ?: return entry
    return entry.copy(meaningVi = normalizedVietnamese)
}

internal fun localizeCompoundMeaningsIfNeeded(
    context: Context,
    entries: List<KanjiCompoundEntry>,
): List<KanjiCompoundEntry> {
    if (!shouldPreferVietnameseMeaning(context) || entries.isEmpty()) return entries

    return entries.map { entry ->
        if (normalizeMeaning(entry.meaning).isNullOrBlank() || !normalizeMeaning(entry.meaningVi).isNullOrBlank()) {
            entry
        } else {
            val translated = translateMeaningToVietnamese(entry.meaning)
            val normalizedVietnamese = normalizeMeaning(translated)
            if (normalizedVietnamese == null) entry else entry.copy(meaningVi = normalizedVietnamese)
        }
    }
}

private fun translateMeaningToVietnamese(sourceMeaning: String): String? {
    val normalizedSource = normalizeMeaningSource(sourceMeaning)
    if (normalizedSource.isBlank()) return null

    val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.VIETNAMESE)
        .build()
    val translator = Translation.getClient(options)

    return try {
        translator.awaitDownloadModel(
            DownloadConditions.Builder().build()
        )
        normalizeTranslatedMeaning(
            translator.awaitTranslation(normalizedSource)
        )
    } finally {
        translator.close()
    }
}

private fun normalizeMeaningSource(value: String): String {
    return value
        .replace('、', ',')
        .replace(';', ',')
        .split(',')
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(", ")
}

private fun normalizeTranslatedMeaning(value: String?): String? {
    val translated = value?.trim().orEmpty()
    if (translated.isBlank()) return null
    return translated
        .replace(" ,", ",")
        .replace(" ;", ";")
        .replace(" / ", ", ")
        .replace(" , ", ", ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .takeIf { it.isNotBlank() }
}

private fun com.google.mlkit.nl.translate.Translator.awaitDownloadModel(
    conditions: DownloadConditions,
) {
    val errorRef = AtomicReference<Exception?>()
    val latch = CountDownLatch(1)
    downloadModelIfNeeded(conditions)
        .addOnSuccessListener { latch.countDown() }
        .addOnFailureListener {
            errorRef.set(it as? Exception ?: RuntimeException(it))
            latch.countDown()
        }
    latch.await()
    errorRef.get()?.let { throw it }
}

private fun com.google.mlkit.nl.translate.Translator.awaitTranslation(text: String): String {
    val resultRef = AtomicReference<String?>()
    val errorRef = AtomicReference<Exception?>()
    val latch = CountDownLatch(1)
    translate(text)
        .addOnSuccessListener {
            resultRef.set(it)
            latch.countDown()
        }
        .addOnFailureListener {
            errorRef.set(it as? Exception ?: RuntimeException(it))
            latch.countDown()
        }
    latch.await()
    errorRef.get()?.let { throw it }
    return resultRef.get().orEmpty()
}
