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
- A full statistics screen

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

No dedicated statistics screen is required for the first implementation.

Optional lightweight UI hooks:
- show `Hôm nay bạn đã xem X phút` near the bottom of the detail screen
- expose the data later in a dedicated stats screen

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

Suggested preference file:
- reuse existing widget preference file or create a dedicated file such as `kanji_study_stats`

Recommended approach:
- use a dedicated file to separate widget state from analytics-like data

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

### New component

Suggested file:
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

Expected usage:
- call `startSession(kanji)` in `onStart`
- call `stopSession()` in `onStop`

## API Design

Suggested API:

```kotlin
object StudyTimeTracker {
    fun startSession(context: Context, kanji: String)
    fun stopSession(context: Context)
    fun getTodayTotalMs(context: Context): Long
    fun getTodayKanjiMs(context: Context, kanji: String): Long
    fun getTodayOpenCount(context: Context): Int
}
```

## Edge Cases

### Empty kanji

If the activity is opened without a valid kanji:
- do not start tracking

### Orientation changes

Activity recreation may trigger extra `onStop` and `onStart` pairs.

Mitigation:
- this is acceptable if session threshold filtering is present
- alternatively, keep in-memory session state and only count valid elapsed windows

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
- first version may attribute the whole session to the day when `onStop` occurs

Future improvement:
- split the duration across both dates if more accuracy is required

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

## Open Questions

- Should the first version show the tracked time in UI immediately, or only store it for future use?
- Should open count increase on every valid session, or on every screen entry including sub-1-second opens?
- Do we want a retention policy for old daily keys, or keep all local history indefinitely?
