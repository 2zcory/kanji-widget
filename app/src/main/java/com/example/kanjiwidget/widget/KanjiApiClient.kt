package com.example.kanjiwidget.widget

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object KanjiApiClient {
    private const val BASE_URL = "https://kanjiapi.dev/v1"

    // Free API: https://kanjiapi.dev
    fun fetchKanjiList(): List<String>? {
        return try {
            val body = get("/kanji/joyo") ?: return null
            val json = JSONArray(body)
            buildList {
                for (i in 0 until json.length()) {
                    val value = json.optString(i)
                    if (!value.isNullOrBlank()) {
                        add(value)
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun fetchKanji(kanji: String): KanjiEntry? {
        return try {
            val body = get("/kanji/$kanji") ?: return null
            val json = JSONObject(body)
            val onyomi = join(json.optJSONArray("on_readings"))
            val kunyomi = join(json.optJSONArray("kun_readings"))
            val meaning = join(json.optJSONArray("meanings"), maxItems = 2).ifBlank { "(no meaning)" }

            val jlptNum = if (json.isNull("jlpt")) null else json.optInt("jlpt", 0)
            val jlpt = if (jlptNum == null || jlptNum <= 0) "N/A" else "N$jlptNum"

            val strokeCount = json.optInt("stroke_count", -1).takeIf { it > 0 }
            val grade = if (json.isNull("grade")) null else json.optInt("grade", 0).takeIf { it > 0 }
            val frequency = if (json.isNull("freq_mainichi_shinbun")) {
                null
            } else {
                json.optInt("freq_mainichi_shinbun", 0).takeIf { it > 0 }
            }
            val unicode = json.optString("unicode", "").takeIf { it.isNotBlank() }
            val note = buildList {
                unicode?.let { add("U+$it") }
                if (sourceNoteNeeded(strokeCount, grade, frequency)) add("Nguồn: kanjiapi.dev")
            }.joinToString(" • ")

            KanjiEntry(
                kanji = kanji,
                onyomi = onyomi.ifBlank { "-" },
                kunyomi = kunyomi.ifBlank { "-" },
                meaningVi = meaning,
                example = note,
                jlptLevel = jlpt,
                strokeCount = strokeCount,
                grade = grade,
                frequency = frequency,
                source = "kanjiapi.dev",
                lastUpdatedEpochMs = System.currentTimeMillis(),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun get(path: String): String? {
        val url = URL("$BASE_URL$path")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }

        return try {
            if (conn.responseCode !in 200..299) {
                null
            } else {
                conn.inputStream.bufferedReader().use { it.readText() }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun join(arr: JSONArray?, maxItems: Int = Int.MAX_VALUE): String {
        if (arr == null || arr.length() == 0) return ""
        return buildList {
            for (i in 0 until arr.length()) {
                if (size >= maxItems) break
                val v = arr.optString(i)
                if (!v.isNullOrBlank()) add(v)
            }
        }.joinToString("、")
    }

    private fun sourceNoteNeeded(
        strokeCount: Int?,
        grade: Int?,
        frequency: Int?,
    ): Boolean {
        return strokeCount == null && grade == null && frequency == null
    }
}
