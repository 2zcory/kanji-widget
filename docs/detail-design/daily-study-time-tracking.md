# Daily Study Time Tracking

## Purpose

Track how long the user keeps the kanji detail screen open each day.

This feature measures engagement time, not guaranteed active study time.
The tracked metric is:
- time while the kanji detail screen is visible in the foreground

## Scope

In scope:
- Record daily total viewing time for the kanji detail screen
- Record daily viewing time per kanji
- Record daily open count for the detail screen
- Store data locally on device

Out of scope for the first version:
- Cloud sync
- Cross-device aggregation
- Background analytics upload
- A dedicated full-screen statistics destination

## User Value

This feature supports:
- showing the user how much time they spent reviewing kanji today
- identifying which kanji the user spent the most time on
- enabling future habit and streak features

## Functional Definition

### Metric definition

The metric is counted only while `KanjiDetailActivity` is in the foreground.

Start timing:
- when the activity enters `onStart`

Stop timing:
- when the activity enters `onStop`

Session duration:
- `stopTimestamp - startTimestamp`

Rules:
- ignore sessions shorter than 1 second
- cap a single session at 10 minutes
- use the device local date for daily grouping

Reasoning:
- short sessions are usually accidental opens or activity transitions
- capping prevents inflated data when the user leaves the screen open and walks away

## UI Behavior

### Current version

No dedicated full statistics screen is required for the first implementation.

Current lightweight UI hooks:
- show today study totals on the detail screen
- expose the stored data through the launcher stats bottom sheet

### Future extension

Possible future views:
- daily total study time
- most-viewed kanji today
- last 7 days chart

## Data Flow

### Activity lifecycle flow

1. User opens `KanjiDetailActivity`
2. `onStart` stores:
   - current timestamp
   - current kanji
3. User keeps the screen open
4. `onStop` computes elapsed time
5. Elapsed time is normalized:
   - discard if below minimum threshold
   - clamp if above max session threshold
6. Persist:
   - daily total time
   - daily time for the current kanji
   - daily open count

## Storage Design

Storage mechanism:
- `SharedPreferences`

Current preference file:
- `kanji_study_stats`

Reason:
- keep study tracking state separate from widget state

### Key format

Daily total:
- `study_total_YYYY-MM-DD`

Daily open count:
- `study_open_count_YYYY-MM-DD`

Daily per-kanji time:
- `study_kanji_YYYY-MM-DD_<kanji>`

Example:
- `study_total_2026-03-10`
- `study_open_count_2026-03-10`
- `study_kanji_2026-03-10_日`

### Stored value type

All time values:
- `Long` in milliseconds

Open count:
- `Int`

## Components

### Component

Primary file:
- `app/src/main/java/com/example/kanjiwidget/stats/StudyTimeTracker.kt`

Responsibilities:
- start a timing session
- stop a timing session
- normalize elapsed time
- persist daily totals
- read daily totals when needed

### Integration point

Main integration target:
- `KanjiDetailActivity`

Current usage:
- call `startSession(kanji)` in `onStart`
- call `stopSession()` in `onStop`

## API Design

Current public API:

```kotlin
object StudyTimeTracker {
    fun startSession(context: Context, kanji: String)
    fun stopSession(context: Context)
    fun getTodayTotalMs(context: Context): Long
    fun getTotalMs(context: Context, date: LocalDate): Long
    fun getTodayKanjiMs(context: Context, kanji: String): Long
    fun getTodayOpenCount(context: Context): Int
    fun getOpenCount(context: Context, date: LocalDate): Int
}
```

Contract notes:
- today-scoped read APIs are used directly by the detail and launcher surfaces
- date-based read APIs support range-based summaries such as the study-time chart

## Edge Cases

### Empty kanji

If the activity is opened without a valid kanji:
- do not start tracking

### Orientation changes

Activity recreation may trigger extra `onStop` and `onStart` pairs.

Mitigation:
- short sessions below the minimum threshold are ignored
- the active session is also guarded through stored active-session state

### App switch / screen off

If the user moves the app to background or turns off the screen:
- timing stops at `onStop`

This behavior is desired because the screen is no longer actively visible.

### Process death

If the app process is killed before `onStop`:
- that session is lost

This is acceptable for local lightweight tracking.

### Day boundary crossing

If a session starts before midnight and ends after midnight:
- split the duration across both dates
- increment the open count once for the date where the session began

Reason:
- daily total time should remain date-accurate without inflating open-count totals across multiple dates

## Privacy

The data stays on device.

No personal identifiers or remote analytics are required for this feature.

## Testing Notes

Manual test cases:
- open detail screen for 5 to 10 seconds and confirm today total increases
- open and close in under 1 second and confirm it is ignored
- leave screen open for more than 10 minutes and confirm stored duration is capped
- open multiple different kanji and confirm per-kanji totals are separated
- send app to background and confirm timing stops
- rotate device and confirm totals do not spike unexpectedly

Suggested unit tests:
- duration normalization
- key generation by date
- accumulation of daily totals
- per-kanji aggregation

## Future Extensions

Potential future improvements:
- add a retention or compaction policy for very old daily keys
- expose per-kanji history beyond today if a richer stats surface is added later
