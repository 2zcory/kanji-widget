package com.example.kanjiwidget.widget

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object KanjiApiClient {
    // Free API: https://kanjiapi.dev
    fun fetchKanji(kanji: String): KanjiEntry? {
        return try {
            val url = URL("https://kanjiapi.dev/v1/kanji/$kanji")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return null
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val onyomi = join(json.optJSONArray("on_readings"))
            val kunyomi = join(json.optJSONArray("kun_readings"))
            val meaning = join(json.optJSONArray("meanings"), maxItems = 2).ifBlank { "(no meaning)" }

            val jlptNum = if (json.isNull("jlpt")) null else json.optInt("jlpt", 0)
            val jlpt = if (jlptNum == null || jlptNum <= 0) "N/A" else "N$jlptNum"

            val strokeCount = json.optInt("stroke_count", -1)
            val unicode = json.optString("unicode", "")
            val grade = if (json.isNull("grade")) null else json.optInt("grade", 0)
            val meta = buildList {
                if (strokeCount > 0) add("Nét: $strokeCount")
                if (grade != null && grade > 0) add("Grade: $grade")
                if (unicode.isNotBlank()) add("U+$unicode")
            }.joinToString(" • ")

            KanjiEntry(
                kanji = kanji,
                onyomi = onyomi.ifBlank { "-" },
                kunyomi = kunyomi.ifBlank { "-" },
                meaningVi = meaning,
                example = meta.ifBlank { "Nguồn: kanjiapi.dev" },
                jlptLevel = jlpt,
                source = "kanjiapi.dev",
                lastUpdatedEpochMs = System.currentTimeMillis(),
            )
        } catch (_: Exception) {
            null
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
}
