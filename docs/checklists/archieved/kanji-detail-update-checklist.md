# Kanji Detail Update Checklist

Last updated: 2026-03-11
Status: Completed
First released in: `v1.1.0`

## Purpose

Track the phased implementation work for the Kanji Detail screen update across multiple sessions.

Update this checklist whenever a task is completed so the current implementation state remains easy to recover.

## Phase 1: Layout Refactor, Readings, And Stats

- [x] Replace the combined reading summary block with separate Onyomi and Kunyomi sections in the detail layout
- [x] Update `KanjiDetailActivity` binding logic to populate separate Onyomi and Kunyomi fields
- [x] Replace the single today-stats text block with three explicit metrics: Today total, Valid opens, and This kanji
- [x] Update the today-stats binding logic to populate the three metric fields
- [x] Review the revised layout on device or emulator for spacing, hierarchy, and fallback states

## Phase 2: Metadata Fields And Data Pipeline

- [x] Extend the detail-screen input contract to support stroke count, grade, and frequency
- [x] Update the cached kanji data model to carry the new metadata fields
- [x] Update API parsing and cache persistence for the new metadata fields
- [x] Pass the new metadata through all detail-screen entry points from widget, main screen, and stats surfaces
- [x] Add hero metadata rendering with conditional visibility when metadata is missing

## Phase 3: Next Random Kanji Action

- [x] Add a secondary action row for detail-screen actions
- [x] Keep replay as an explicit animation action in the new action row
- [x] Add a Next random kanji action to the detail screen
- [x] Reuse the cached kanji catalog and avoid immediately repeating the current kanji when possible
- [x] Open the next kanji detail screen with cached detail extras when available
- [x] Disable the action or show a clear fallback when the catalog is unavailable

## Phase 4: Documentation Updates

- [x] Create a dedicated detail design document for the Kanji Detail screen
- [x] Document the revised layout structure and data contract
- [x] Document the fallback behavior for missing metadata and stroke-order load failures
- [x] Document the Next random kanji action behavior
- [x] Update `docs/basic-design.md` if the shipped user-facing behavior changes materially

## Progress Log

- 2026-03-11: Created the phased checklist for the Kanji Detail update work.
- 2026-03-11: Completed the Phase 1 layout and binding refactor for separate readings and explicit today-study metrics.
- 2026-03-11: Verified the Phase 1 changes with `./gradlew :app:compileDebugKotlin`.
- 2026-03-11: Phase 1 manual review was confirmed by the user and the full phase is now marked complete.
- 2026-03-11: Implemented the Phase 2 metadata model, cache, intent pipeline, and hero metadata rendering work; waiting for user verification before marking the checklist items complete.
- 2026-03-11: Verified the current Phase 2 changes with `./gradlew :app:compileDebugKotlin`.
- 2026-03-11: Phase 2 verification was confirmed by the user and the full phase is now marked complete.
- 2026-03-11: Implemented the Phase 3 detail action row, shared detail-intent helper, and next-random navigation behavior; waiting for user verification before marking the checklist items complete.
- 2026-03-11: Verified the current Phase 3 changes with `./gradlew :app:compileDebugKotlin`.
- 2026-03-11: Phase 3 verification was confirmed by the user and the full phase is now marked complete.
- 2026-03-11: Implemented the Phase 4 documentation updates, including a dedicated Kanji Detail design document and aligned overview docs.
- 2026-03-11: Phase 4 documentation updates are complete and the full phase is now marked complete.
- 2026-03-11: The phased Kanji Detail update first shipped in release `v1.1.0`.
