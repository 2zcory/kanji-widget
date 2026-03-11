# Kanji Detail Update Checklist

Last updated: 2026-03-11
Status: In progress

## Purpose

Track the phased implementation work for the Kanji Detail screen update across multiple sessions.

Update this checklist whenever a task is completed so the current implementation state remains easy to recover.

## Phase 1: Layout Refactor, Readings, And Stats

- [ ] Replace the combined reading summary block with separate Onyomi and Kunyomi sections in the detail layout
- [ ] Update `KanjiDetailActivity` binding logic to populate separate Onyomi and Kunyomi fields
- [ ] Replace the single today-stats text block with three explicit metrics: Today total, Valid opens, and This kanji
- [ ] Update the today-stats binding logic to populate the three metric fields
- [ ] Review the revised layout on device or emulator for spacing, hierarchy, and fallback states

## Phase 2: Metadata Fields And Data Pipeline

- [ ] Extend the detail-screen input contract to support stroke count, grade, and frequency
- [ ] Update the cached kanji data model to carry the new metadata fields
- [ ] Update API parsing and cache persistence for the new metadata fields
- [ ] Pass the new metadata through all detail-screen entry points from widget, main screen, and stats surfaces
- [ ] Add hero metadata rendering with conditional visibility when metadata is missing

## Phase 3: Next Random Kanji Action

- [ ] Add a secondary action row for detail-screen actions
- [ ] Keep replay as an explicit animation action in the new action row
- [ ] Add a Next random kanji action to the detail screen
- [ ] Reuse the cached kanji catalog and avoid immediately repeating the current kanji when possible
- [ ] Open the next kanji detail screen with cached detail extras when available
- [ ] Disable the action or show a clear fallback when the catalog is unavailable

## Phase 4: Documentation Updates

- [ ] Create a dedicated detail design document for the Kanji Detail screen
- [ ] Document the revised layout structure and data contract
- [ ] Document the fallback behavior for missing metadata and stroke-order load failures
- [ ] Document the Next random kanji action behavior
- [ ] Update `docs/basic-design.md` if the shipped user-facing behavior changes materially

## Progress Log

- 2026-03-11: Created the phased checklist for the Kanji Detail update work.
