# Kanji Detail

## Purpose

Define the detailed design for the Kanji Detail screen.

This document covers:
- current screen structure
- stroke-order playback behavior
- detail navigation inputs
- today-only study metrics
- the Next random kanji action

## Scope

In scope:
- the `KanjiDetailActivity` screen structure and behavior
- detail-screen entry points from widget, main screen, and study stats
- cached metadata usage for detail rendering
- stroke-order loading and replay behavior
- next-random navigation from the detail screen

Out of scope for the current version:
- editing or favoriting kanji
- server-backed progress sync
- spaced-repetition scheduling
- prefetching the next random kanji in the background

## Current Components

Primary files:
- `app/src/main/java/com/example/kanjiwidget/KanjiDetailActivity.kt`
- `app/src/main/java/com/example/kanjiwidget/KanjiDetailNavigator.kt`
- `app/src/main/java/com/example/kanjiwidget/widget/KanjiStrokeOrderClient.kt`
- `app/src/main/res/layout/activity_kanji_detail.xml`

Supporting sources:
- `app/src/main/java/com/example/kanjiwidget/widget/KanjiWidgetPrefs.kt`
- `app/src/main/java/com/example/kanjiwidget/history/RecentKanjiStore.kt`
- `app/src/main/java/com/example/kanjiwidget/stats/StudyTimeTracker.kt`

## Screen Role

The Kanji Detail screen is the richer study surface opened from lightweight review entry points.

It should let the user:
- inspect one selected kanji in more detail
- replay stroke order on demand
- review readings, meaning, note, source, and available metadata
- see today-only study totals for the current kanji context
- continue to another random kanji without returning to the launcher

## UI Behavior

### Hero section

Current contents:
- large kanji title
- meaning subtitle
- optional hero metadata row
- JLPT badge

Hero metadata currently includes:
- stroke count
- grade
- frequency

Behavior:
- hide the hero metadata row when all three values are missing
- show the JLPT placeholder badge when JLPT data is unavailable

### Stroke-order card

Current contents:
- card title and attribution
- short usage hint
- stroke-order canvas area
- status and loading state
- action row

Current action row:
- `Replay`
- `Next random`

Behavior:
- `Replay` restarts the current animation if stroke-order HTML is already loaded
- if the stroke-order payload is not loaded yet, `Replay` triggers a load attempt instead
- `Next random` opens another kanji detail screen and finishes the current detail activity

### Readings section

Current contents:
- Onyomi label and value
- Kunyomi label and value

Behavior:
- use a shared placeholder when a reading field is unavailable

### Today section

Current contents:
- Today total
- Valid opens
- This kanji

Behavior:
- values are computed from local study tracking only
- the section always renders, even when the values are zero

### Meaning and note sections

Current contents:
- primary meaning
- note or example text
- source line

Behavior:
- meaning and note each fall back to their own placeholder copy when no cached detail is available
- the source line always renders and falls back to the default source label when a specific source is unavailable

## Navigation Inputs

The detail screen accepts explicit extras for:
- kanji
- source
- JLPT level
- Onyomi
- Kunyomi
- meaning
- note
- stroke count
- grade
- frequency

Current shared builder:
- `KanjiDetailNavigator.buildDetailIntent(...)`

Current entry points:
- widget card tap
- main screen latest-kanji action
- main screen random-kanji action
- recent-kanji rows on the main screen
- kanji ranking rows in the study stats bottom sheet
- detail-screen `Next random` action

## Data Flow

### Detail content

Current behavior:
1. An entry point builds a detail intent through `KanjiDetailNavigator`
2. The navigator reads cached kanji detail from `KanjiWidgetPrefs` when available
3. Cached values override fallback values from the calling surface
4. `KanjiDetailActivity` renders the received extras directly

Reason:
- keeps the detail-screen contract stable across multiple launch surfaces
- avoids duplicating detail-intent assembly logic in each caller

### Stroke-order loading

Current behavior:
1. `KanjiDetailActivity` starts a load when a non-empty kanji is present
2. `KanjiStrokeOrderClient.fetchSvg(...)` loads the SVG
3. `KanjiStrokeOrderClient.buildAnimatedHtml(...)` converts the SVG to animated HTML
4. the screen loads the HTML into a `WebView`

### Study tracking

Current behavior:
- on `onStart`, the screen records the viewed kanji in `RecentKanjiStore`
- on `onStart`, the screen starts a study session through `StudyTimeTracker`
- on `onStop`, the screen stops the current study session
- today metrics are refreshed from local study totals

## Random Navigation Behavior

The detail screen supports one-tap continuation to another random kanji.

Current behavior:
- use the cached kanji catalog from `KanjiWidgetPrefs`
- when the catalog has more than one item, avoid choosing the current kanji if possible
- build the next detail intent through `KanjiDetailNavigator`
- rely on cached detail extras when available for the selected kanji
- call `startActivity(...)` for the next detail screen
- immediately call `finish()` on the current detail activity

Reason:
- preserves a lightweight review flow from inside the detail screen
- avoids building a long stack of detail activities

### Disabled state

If the cached catalog is unavailable:
- disable the `Next random` button
- reduce its alpha
- keep the label unchanged

Reason:
- the action should remain visually consistent without failing silently after a tap

## Storage And API Impact

Current local dependencies:
- cached kanji catalog from `KanjiWidgetPrefs`
- cached per-kanji detail entries from `KanjiWidgetPrefs`
- recent-view history from `RecentKanjiStore`
- local study stats from `StudyTimeTracker`

Current remote dependency:
- KanjiVG-derived stroke-order SVG loading through `KanjiStrokeOrderClient`

No new persistent storage keys were added specifically for the detail screen in the current phase.

## Edge Cases

### Missing kanji extra

Behavior:
- render placeholder hero content
- render empty study info placeholders
- show the empty-state error message
- do not attempt to start a valid study session

### Missing metadata

Behavior:
- hide the hero metadata row if stroke count, grade, and frequency are all absent
- keep the rest of the screen visible

### Missing cached detail fields

Behavior:
- render placeholders for readings, meaning, and note
- render the default source label

### Stroke-order load failure

Behavior:
- hide the loading indicator
- show an explicit error message for the current kanji
- keep the replay action enabled so the user can retry

### Empty catalog for Next random

Behavior:
- disable the action
- reduce button alpha
- do not attempt random navigation

## Testing Notes

Recommended checks:
- open detail from widget, main screen, and stats ranking rows
- verify cached meaning, JLPT, and metadata appear when available
- verify placeholders render when cached detail is absent
- verify stroke-order loading, replay, and retry-after-failure behavior
- verify `Next random` avoids the current kanji when the catalog has multiple entries
- verify `Next random` is disabled when the catalog is unavailable
- verify the current detail screen finishes after opening the next random kanji
