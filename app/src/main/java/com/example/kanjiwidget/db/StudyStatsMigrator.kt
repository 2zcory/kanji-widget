package com.example.kanjiwidget.db

import android.content.Context
import java.time.LocalDate

import androidx.room.withTransaction

class StudyStatsMigrator(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val studyPrefs = context.getSharedPreferences("kanji_study_stats", Context.MODE_PRIVATE)
    private val migrationPrefs = context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)

    suspend fun migrateIfNeeded() {
        if (migrationPrefs.getBoolean("study_stats_migrated", false)) return

        val allEntries = studyPrefs.all
        val kanjiData = mutableMapOf<Pair<String, String>, KanjiData>() // (date, kanji) -> data
        val totalData = mutableMapOf<String, TotalData>() // date -> data

        allEntries.forEach { (key, value) ->
            val match = KANJI_DATA_REGEX.matchEntire(key)
            if (match != null) {
                val type = match.groupValues[1]
                val date = match.groupValues[2]
                val kanji = match.groupValues[3]
                val numericValue = (value as? Number)?.toLong() ?: 0L
                if (numericValue <= 0) return@forEach

                val entry = kanjiData.getOrPut(date to kanji) { KanjiData() }
                if (type == "kanji") {
                    entry.studyTimeMs = numericValue
                } else if (type == "open_kanji") {
                    entry.openCount = numericValue
                }
                return@forEach
            }

            val totalMatch = TOTAL_DATA_REGEX.matchEntire(key)
            if (totalMatch != null) {
                val type = totalMatch.groupValues[1]
                val date = totalMatch.groupValues[2]
                val numericValue = (value as? Number)?.toLong() ?: 0L
                if (numericValue <= 0) return@forEach

                val entry = totalData.getOrPut(date) { TotalData() }
                if (type == "total") {
                    entry.totalStudyMs = numericValue
                } else if (type == "open_count") {
                    entry.totalOpenCount = numericValue.toInt()
                }
            }
        }

        if (kanjiData.isEmpty() && totalData.isEmpty()) {
            migrationPrefs.edit().putBoolean("study_stats_migrated", true).apply()
            return
        }

        val kanjiEntities = kanjiData.map { (key, data) ->
            DailyKanjiStudyEntity(key.first, key.second, data.studyTimeMs, data.openCount)
        }
        val totalEntities = totalData.map { (date, data) ->
            DailyTotalStudyEntity(date, data.totalStudyMs, data.totalOpenCount)
        }

        db.withTransaction {
            val kanjiDao = db.dailyKanjiStudyDao()
            val totalDao = db.dailyTotalStudyDao()
            kanjiEntities.forEach { kanjiDao.upsert(it) }
            totalEntities.forEach { totalDao.upsert(it) }
        }

        // Clear old keys
        val editor = studyPrefs.edit()
        allEntries.keys.forEach { key ->
            if (KANJI_DATA_REGEX.matches(key) || TOTAL_DATA_REGEX.matches(key)) {
                editor.remove(key)
            }
        }
        editor.apply()

        migrationPrefs.edit().putBoolean("study_stats_migrated", true).apply()
    }

    private class KanjiData {
        var studyTimeMs: Long = 0
        var openCount: Long = 0
    }

    private class TotalData {
        var totalStudyMs: Long = 0
        var totalOpenCount: Int = 0
    }

    companion object {
        private val KANJI_DATA_REGEX = Regex("""study_(kanji|open_kanji)_([0-9]{4}-[0-9]{2}-[0-9]{2})_(.+)""")
        private val TOTAL_DATA_REGEX = Regex("""study_(total|open_count)_([0-9]{4}-[0-9]{2}-[0-9]{2})""")
    }
}
