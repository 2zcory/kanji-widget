# Main Screen

## Purpose

Define the detailed design for the app main screen launched from the app icon.

This document covers:
- current behavior
- desired future behavior if the main screen is expanded beyond a launcher stub

## Current State

Current implementation:
- `MainActivity` opens and immediately finishes
- the app is currently widget-first
- users are expected to interact mainly through the home screen widget

Current file:
- `app/src/main/java/com/example/kanjiwidget/MainActivity.kt`

## Design Goal

The main screen should evolve from a launcher stub into a lightweight control center for the widget-based learning flow.

The screen should help users:
- understand what the app does
- install or use the widget
- open the latest kanji detail screen
- review today’s learning activity

## Scope

In scope:
- define the structure and behavior of the main screen
- align the screen with existing widget and detail-screen features
- allow future integration with daily study statistics

Out of scope for the first version:
- user accounts
- remote sync
- advanced charts
- heavy onboarding flow

## Screen Role

The main screen should act as:
- an entry point for users who open the app from the launcher
- a fallback surface when widget interaction is not enough
- a summary screen for today’s study usage

## Proposed UI Structure

### 1. Header

Contents:
- app title
- short one-line description
- optional subtitle such as `Kanji widget học nhanh mỗi ngày`

Purpose:
- immediately explain that this is a widget-centered learning app

### 2. Today Summary Card

Contents:
- total study time today
- valid detail-screen opens today
- last kanji opened or most-viewed kanji today

Purpose:
- make the app launch feel meaningful even before deeper navigation exists

Data source:
- local stats from `StudyTimeTracker`

### 3. Quick Actions

Suggested actions:
- `Mở kanji gần nhất`
- `Xem thống kê hôm nay`
- `Hướng dẫn thêm widget`

Purpose:
- give the launcher screen immediate utility

### 4. Widget Help Section

Contents:
- short explanation of how to add the widget
- visual hint or short 2-step instruction

Purpose:
- support users who install the app but have not added the widget yet

### 5. Recent Kanji Section

Contents:
- latest viewed kanji items
- each item opens the detail screen

Purpose:
- make the app usable as a lightweight review hub

This section can be deferred until recent-history tracking exists.

## User Flow

### Flow A: First app launch

1. User taps app icon
2. Main screen explains the widget-centric concept
3. User sees how to add the home screen widget
4. User returns to the launcher and adds the widget

### Flow B: Returning user

1. User taps app icon
2. Main screen shows today summary
3. User opens the latest kanji detail screen or reviews stats

### Flow C: User without widget

1. User opens app
2. Main screen detects no meaningful usage yet
3. Widget help is emphasized over stats

## Behavior Rules

### Initial version

If implemented now, the main screen should:
- stay lightweight
- avoid replacing the widget as the main learning surface
- not duplicate all detail-screen functionality

### Empty state

If there is no recorded study data:
- show zero-value summary
- show setup guidance for the widget

### With data

If study data exists:
- show today summary first
- prioritize quick access to recent kanji detail

## Navigation Design

Suggested destinations from the main screen:
- detail screen for the most recently viewed kanji
- future statistics screen
- widget setup instructions

Navigation style:
- simple explicit buttons or cards
- no bottom navigation is needed

## Data Dependencies

Required local data:
- daily total study time
- daily open count
- optional latest viewed kanji
- optional recent kanji list

Existing reusable source:
- `StudyTimeTracker` for today totals

Likely new storage for future extension:
- recent kanji history store

## Technical Notes

Suggested implementation path:
- replace the current finish-only `MainActivity` with a simple layout-based activity
- populate summary fields from local storage
- keep business logic separate from the activity

Suggested future files:
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/java/com/example/kanjiwidget/MainActivity.kt`
- optional `app/src/main/java/com/example/kanjiwidget/home/HomeSummaryRepository.kt`

## Edge Cases

### No widget installed

The app should still be usable and should explain how to add the widget.

### No tracked study data

The screen should not feel empty.
Use setup guidance and concise educational copy instead.

### Latest kanji missing

If recent-history data is unavailable:
- hide the `Mở kanji gần nhất` action
- keep summary and widget help visible

## Testing Notes

Manual test cases:
- open app on a fresh install and verify the empty state
- open app after using the detail screen and verify today summary is shown
- verify main-screen actions open the expected destinations
- verify layout works on both narrow and tall devices

## Open Questions

- Should the main screen remain very small and informational, or become a true dashboard over time?
- Should widget setup guidance be static text, or should it include visuals?
- Do we want a recent-kanji history feature before building the full main screen?
