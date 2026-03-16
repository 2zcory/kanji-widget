package com.example.kanjiwidget.stats

import android.content.Context
import android.os.SystemClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object StudyTimeTracker {
    private const val PREF = "kanji_study_stats"
    private const val KEY_ACTIVE_KANJI = "active_kanji"
    private const val KEY_ACTIVE_START_WALL = "active_start_wall_ms"
    private const val KEY_ACTIVE_START_ELAPSED = "active_start_elapsed_ms"
    private const val MIN_SESSION_MS = 1_000L
    private const val MAX_SESSION_MS = 10 * 60_000L

    @Synchronized
    fun startSession(context: Context, kanji: String) {
        val normalizedKanji = kanji.trim()
        if (normalizedKanji.isBlank()) return

        val sp = prefs(context)
        val activeKanji = sp.getString(KEY_ACTIVE_KANJI, null)
        if (activeKanji == normalizedKanji && sp.contains(KEY_ACTIVE_START_ELAPSED)) {
            return
        }

        // Close any unfinished session before starting another one.
        stopSession(context)

        sp.edit()
            .putString(KEY_ACTIVE_KANJI, normalizedKanji)
            .putLong(KEY_ACTIVE_START_WALL, System.currentTimeMillis())
            .putLong(KEY_ACTIVE_START_ELAPSED, SystemClock.elapsedRealtime())
            .apply()
    }

    @Synchronized
    fun stopSession(context: Context) {
        val sp = prefs(context)
        val kanji = sp.getString(KEY_ACTIVE_KANJI, null)?.trim().orEmpty()
        val startWallMs = sp.getLong(KEY_ACTIVE_START_WALL, -1L)
        val startElapsedMs = sp.getLong(KEY_ACTIVE_START_ELAPSED, -1L)

        clearActiveSession(sp)

        if (kanji.isBlank() || startWallMs <= 0L || startElapsedMs <= 0L) return

        val rawDurationMs = (SystemClock.elapsedRealtime() - startElapsedMs).coerceAtLeast(0L)
        if (rawDurationMs < MIN_SESSION_MS) return

        val durationMs = rawDurationMs.coerceAtMost(MAX_SESSION_MS)
        val endWallMs = startWallMs + durationMs
        persistAcrossDates(sp, kanji, startWallMs, endWallMs)
    }

    fun getTodayTotalMs(context: Context): Long {
        return prefs(context).getLong(totalKey(today()), 0L)
    }

    fun getTotalMs(context: Context, date: LocalDate): Long {
        return prefs(context).getLong(totalKey(date), 0L)
    }

    fun getTodayKanjiMs(context: Context, kanji: String): Long {
        val normalizedKanji = kanji.trim()
        if (normalizedKanji.isBlank()) return 0L
        return prefs(context).getLong(kanjiKey(today(), normalizedKanji), 0L)
    }

    @Synchronized
    fun recordKanjiOpen(context: Context, kanji: String) {
        val normalizedKanji = kanji.trim()
        if (normalizedKanji.isBlank()) return
        val sp = prefs(context)
        val key = kanjiOpenKey(today(), normalizedKanji)
        sp.edit().putLong(key, sp.getLong(key, 0L) + 1L).apply()
    }

    fun getTodayKanjiOpenCount(context: Context, kanji: String): Long {
        val normalizedKanji = kanji.trim()
        if (normalizedKanji.isBlank()) return 0L
        return prefs(context).getLong(kanjiOpenKey(today(), normalizedKanji), 0L)
    }

    fun getTodayOpenCount(context: Context): Int {
        return prefs(context).getInt(openCountKey(today()), 0)
    }

    fun getOpenCount(context: Context, date: LocalDate): Int {
        return prefs(context).getInt(openCountKey(date), 0)
    }

    private fun persistAcrossDates(
        sp: android.content.SharedPreferences,
        kanji: String,
        startWallMs: Long,
        endWallMs: Long,
    ) {
        if (endWallMs <= startWallMs) return

        val zoneId = ZoneId.systemDefault()
        var cursor = startWallMs
        val editor = sp.edit()
        var incrementedOpenCount = false

        while (cursor < endWallMs) {
            val date = dateAt(cursor, zoneId)
            val nextBoundary = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val segmentEnd = minOf(endWallMs, nextBoundary)
            val segmentDuration = (segmentEnd - cursor).coerceAtLeast(0L)
            if (segmentDuration > 0L) {
                editor.putLong(
                    totalKey(date),
                    sp.getLong(totalKey(date), 0L) + segmentDuration
                )
                editor.putLong(
                    kanjiKey(date, kanji),
                    sp.getLong(kanjiKey(date, kanji), 0L) + segmentDuration
                )
                if (!incrementedOpenCount) {
                    editor.putInt(
                        openCountKey(date),
                        sp.getInt(openCountKey(date), 0) + 1
                    )
                    incrementedOpenCount = true
                }
            }
            cursor = segmentEnd
        }

        editor.apply()
    }

    private fun clearActiveSession(sp: android.content.SharedPreferences) {
        sp.edit()
            .remove(KEY_ACTIVE_KANJI)
            .remove(KEY_ACTIVE_START_WALL)
            .remove(KEY_ACTIVE_START_ELAPSED)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun totalKey(date: LocalDate) = "study_total_$date"

    private fun openCountKey(date: LocalDate) = "study_open_count_$date"

    private fun kanjiOpenKey(date: LocalDate, kanji: String) = "study_open_kanji_${date}_$kanji"

    private fun kanjiKey(date: LocalDate, kanji: String) = "study_kanji_${date}_$kanji"

    private fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())

    private fun dateAt(epochMs: Long, zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
}
