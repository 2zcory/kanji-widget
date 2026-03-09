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
            val onyomi = join(json.optJSONArray("onyomi"))
            val kunyomi = join(json.optJSONArray("kunyomi"))
            val meaning = json.optJSONArray("meanings")?.optString(0).orEmpty().ifBlank { "(chưa có nghĩa)" }
            val jlptNum = json.optInt("jlpt", 5)

            KanjiEntry(
                kanji = kanji,
                onyomi = onyomi.ifBlank { "-" },
                kunyomi = kunyomi.ifBlank { "-" },
                meaningVi = meaning,
                example = "Nguồn: kanjiapi.dev",
                jlptLevel = "N$jlptNum"
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun join(arr: JSONArray?): String {
        if (arr == null || arr.length() == 0) return ""
        return buildList {
            for (i in 0 until arr.length()) {
                val v = arr.optString(i)
                if (!v.isNullOrBlank()) add(v)
            }
        }.joinToString("、")
    }
}
