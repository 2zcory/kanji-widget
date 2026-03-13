package com.example.kanjiwidget.widget

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.kanjiwidget.detail.KanjiCompoundEntry
import com.example.kanjiwidget.detail.UsageHintKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KanjiWidgetPrefsTest {
    private val context = TestContext()

    @Before
    fun resetPrefs() {
        context.getSharedPreferences("kanji_widget_pref", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun widgetStateRoundTripAndCleanup_areScopedPerWidget() {
        KanjiWidgetPrefs.setDeck(context, widgetId = 7, deck = listOf(1, 4, 9))
        KanjiWidgetPrefs.setDeckPos(context, widgetId = 7, value = 2)
        KanjiWidgetPrefs.setIndex(context, widgetId = 7, value = 4)
        KanjiWidgetPrefs.setCurrentKanji(context, widgetId = 7, value = "日")
        KanjiWidgetPrefs.setRevealAnswer(context, widgetId = 7, value = true)

        KanjiWidgetPrefs.setCurrentKanji(context, widgetId = 8, value = "月")
        KanjiWidgetPrefs.setRevealAnswer(context, widgetId = 8, value = true)
        KanjiWidgetPrefs.setWidgetSurfaceAlpha(context, 0.7f)

        assertEquals(listOf(1, 4, 9), KanjiWidgetPrefs.getDeck(context, 7))
        assertEquals(2, KanjiWidgetPrefs.getDeckPos(context, 7))
        assertEquals(4, KanjiWidgetPrefs.getIndex(context, 7))
        assertEquals("日", KanjiWidgetPrefs.getCurrentKanji(context, 7))
        assertTrue(KanjiWidgetPrefs.getRevealAnswer(context, 7))

        KanjiWidgetPrefs.clearWidgetState(context, 7)

        assertTrue(KanjiWidgetPrefs.getDeck(context, 7).isEmpty())
        assertEquals(0, KanjiWidgetPrefs.getDeckPos(context, 7))
        assertEquals(0, KanjiWidgetPrefs.getIndex(context, 7))
        assertNull(KanjiWidgetPrefs.getCurrentKanji(context, 7))
        assertFalse(KanjiWidgetPrefs.getRevealAnswer(context, 7))

        assertEquals("月", KanjiWidgetPrefs.getCurrentKanji(context, 8))
        assertTrue(KanjiWidgetPrefs.getRevealAnswer(context, 8))
        assertEquals(0.7f, KanjiWidgetPrefs.getWidgetSurfaceAlpha(context))
    }

    @Test
    fun widgetSurfaceAlpha_isClampedToSupportedRange() {
        KanjiWidgetPrefs.setWidgetSurfaceAlpha(context, 0.1f)
        assertEquals(0.4f, KanjiWidgetPrefs.getWidgetSurfaceAlpha(context))

        KanjiWidgetPrefs.setWidgetSurfaceAlpha(context, 1.8f)
        assertEquals(1f, KanjiWidgetPrefs.getWidgetSurfaceAlpha(context))
    }

    @Test
    fun perWidgetSurfaceAlpha_fallsBackToSharedDefaultAndCleansUp() {
        KanjiWidgetPrefs.setWidgetSurfaceAlpha(context, 0.7f)
        assertEquals(0.7f, KanjiWidgetPrefs.getWidgetSurfaceAlpha(context, 11))

        KanjiWidgetPrefs.setWidgetSurfaceAlpha(context, widgetId = 11, value = 0.55f)
        KanjiWidgetPrefs.setWidgetSurfaceAlpha(context, widgetId = 12, value = 1.0f)

        assertEquals(0.55f, KanjiWidgetPrefs.getWidgetSurfaceAlpha(context, 11))
        assertEquals(1.0f, KanjiWidgetPrefs.getWidgetSurfaceAlpha(context, 12))

        KanjiWidgetPrefs.clearWidgetState(context, 11)

        assertEquals(0.7f, KanjiWidgetPrefs.getWidgetSurfaceAlpha(context, 11))
        assertEquals(1.0f, KanjiWidgetPrefs.getWidgetSurfaceAlpha(context, 12))
    }

    @Test
    fun cachedCompounds_keepEntriesWithBlankReadingWhenWrittenAndMeaningExist() {
        val entries = filterCachedCompoundEntries(
            listOf(
                KanjiCompoundEntry(
                    written = "日本",
                    reading = "",
                    meaning = "Japan",
                    usageHintKey = UsageHintKey.COMMON_WORD,
                    priorities = listOf("ichi1"),
                ),
                KanjiCompoundEntry(
                    written = "日光",
                    reading = "にっこう",
                    meaning = "sunlight",
                    usageHintKey = UsageHintKey.NEWS_HEAVY,
                    priorities = listOf("news1"),
                ),
                KanjiCompoundEntry(
                    written = "",
                    reading = "ひ",
                    meaning = "sun",
                    usageHintKey = UsageHintKey.STUDY_WORD,
                ),
            )
        )

        assertEquals(2, entries.size)
        assertEquals("日本", entries[0].written)
        assertEquals("", entries[0].reading)
        assertEquals("Japan", entries[0].meaning)
        assertEquals(UsageHintKey.COMMON_WORD, entries[0].usageHintKey)
        assertEquals(listOf("ichi1"), entries[0].priorities)
    }

    @Test
    fun cachedCompounds_returnNullWhenNoEntryHasWrittenAndMeaning() {
        val entries = filterCachedCompoundEntries(
            listOf(
                KanjiCompoundEntry(
                    written = "",
                    reading = "",
                    meaning = "Japan",
                    usageHintKey = UsageHintKey.COMMON_WORD,
                ),
                KanjiCompoundEntry(
                    written = "日本",
                    reading = "にほん",
                    meaning = "",
                    usageHintKey = UsageHintKey.COMMON_WORD,
                ),
            )
        )

        assertTrue(entries.isEmpty())
    }

    private class TestContext : ContextWrapper(null) {
        private val prefs = mutableMapOf<String, SharedPreferences>()

        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
            val key = name ?: "default"
            return prefs.getOrPut(key) { InMemorySharedPreferences() }
        }
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? =
            values[key] as? String ?: defValue

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST")
            (values[key] as? MutableSet<String>) ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            values[key] as? Int ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            values[key] as? Long ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            values[key] as? Float ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            values[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = Editor(values)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private class Editor(
            private val target: MutableMap<String, Any?>,
        ) : SharedPreferences.Editor {
            private val pending = linkedMapOf<String, Any?>()
            private val removals = linkedSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = applyValue(key, value)

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
                applyValue(key, values?.toMutableSet())

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = applyValue(key, value)

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = applyValue(key, value)

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = applyValue(key, value)

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = applyValue(key, value)

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) removals += key
                pending.remove(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                pending.clear()
                removals.clear()
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) target.clear()
                removals.forEach(target::remove)
                pending.forEach { (key, value) ->
                    if (value == null) {
                        target.remove(key)
                    } else {
                        target[key] = value
                    }
                }
            }

            private fun applyValue(key: String?, value: Any?): SharedPreferences.Editor {
                if (key != null) {
                    pending[key] = value
                    removals.remove(key)
                }
                return this
            }
        }
    }
}
