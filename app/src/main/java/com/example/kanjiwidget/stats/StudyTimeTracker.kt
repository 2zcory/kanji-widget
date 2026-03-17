package com.example.kanjiwidget.stats

import android.content.Context
import android.os.SystemClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

import com.example.kanjiwidget.db.AppDatabase
import com.example.kanjiwidget.db.DailyKanjiStudyEntity
import com.example.kanjiwidget.db.DailyTotalStudyEntity
import androidx.room.withTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object StudyTimeTracker {
    private const val PREF = "kanji_study_stats"
    private const val KEY_ACTIVE_KANJI = "active_kanji"
    private const val KEY_ACTIVE_START_WALL = "active_start_wall_ms"
    private const val KEY_ACTIVE_START_ELAPSED = "active_start_elapsed_ms"
    private const val MIN_SESSION_MS = 1_000L
    private const val MAX_SESSION_MS = 10 * 60_000L

    private val scope = CoroutineScope(Dispatchers.IO)

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
        
        scope.launch {
            persistAcrossDates(context, kanji, startWallMs, endWallMs)
        }
    }

    fun getTodayTotalMs(context: Context): Long = runBlocking {
        AppDatabase.getInstance(context).dailyTotalStudyDao().getEntry(today().toString())?.totalStudyMs ?: 0L
    }

    fun getTotalMs(context: Context, date: LocalDate): Long = runBlocking {
        AppDatabase.getInstance(context).dailyTotalStudyDao().getEntry(date.toString())?.totalStudyMs ?: 0L
    }

    fun getTodayKanjiMs(context: Context, kanji: String): Long = runBlocking {
        val normalizedKanji = kanji.trim()
        if (normalizedKanji.isBlank()) return@runBlocking 0L
        AppDatabase.getInstance(context).dailyKanjiStudyDao().getEntry(today().toString(), normalizedKanji)?.studyTimeMs ?: 0L
    }

    @Synchronized
    fun recordKanjiOpen(context: Context, kanji: String) {
        val normalizedKanji = kanji.trim()
        if (normalizedKanji.isBlank()) return
        
        scope.launch {
            val date = today().toString()
            val dao = AppDatabase.getInstance(context).dailyKanjiStudyDao()
            val existing = dao.getEntry(date, normalizedKanji)
            dao.upsert(
                DailyKanjiStudyEntity(
                    date = date,
                    kanji = normalizedKanji,
                    studyTimeMs = existing?.studyTimeMs ?: 0L,
                    openCount = (existing?.openCount ?: 0L) + 1L
                )
            )
        }
    }

    fun getTodayKanjiOpenCount(context: Context, kanji: String): Long = runBlocking {
        val normalizedKanji = kanji.trim()
        if (normalizedKanji.isBlank()) return@runBlocking 0L
        AppDatabase.getInstance(context).dailyKanjiStudyDao().getEntry(today().toString(), normalizedKanji)?.openCount ?: 0L
    }

    fun getTodayOpenCount(context: Context): Int = runBlocking {
        AppDatabase.getInstance(context).dailyTotalStudyDao().getEntry(today().toString())?.totalOpenCount ?: 0
    }

    fun getOpenCount(context: Context, date: LocalDate): Int = runBlocking {
        AppDatabase.getInstance(context).dailyTotalStudyDao().getEntry(date.toString())?.totalOpenCount ?: 0
    }

    private suspend fun persistAcrossDates(
        context: Context,
        kanji: String,
        startWallMs: Long,
        endWallMs: Long,
    ) {
        if (endWallMs <= startWallMs) return

        val zoneId = ZoneId.systemDefault()
        var cursor = startWallMs
        var incrementedOpenCount = false
        
        val db = AppDatabase.getInstance(context)

        db.withTransaction {
            val kanjiDao = db.dailyKanjiStudyDao()
            val totalDao = db.dailyTotalStudyDao()

            while (cursor < endWallMs) {
                val date = dateAt(cursor, zoneId)
                val dateStr = date.toString()
                val nextBoundary = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val segmentEnd = minOf(endWallMs, nextBoundary)
                val segmentDuration = (segmentEnd - cursor).coerceAtLeast(0L)
                
                if (segmentDuration > 0L) {
                    // Update Total
                    val existingTotal = totalDao.getEntry(dateStr)
                    totalDao.upsert(
                        DailyTotalStudyEntity(
                            date = dateStr,
                            totalStudyMs = (existingTotal?.totalStudyMs ?: 0L) + segmentDuration,
                            totalOpenCount = (existingTotal?.totalOpenCount ?: 0) + (if (!incrementedOpenCount) 1 else 0)
                        )
                    )
                    
                    // Update Kanji
                    val existingKanji = kanjiDao.getEntry(dateStr, kanji)
                    kanjiDao.upsert(
                        DailyKanjiStudyEntity(
                            date = dateStr,
                            kanji = kanji,
                            studyTimeMs = (existingKanji?.studyTimeMs ?: 0L) + segmentDuration,
                            openCount = existingKanji?.openCount ?: 0L
                        )
                    )
                    
                    incrementedOpenCount = true
                }
                cursor = segmentEnd
            }
        }
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
