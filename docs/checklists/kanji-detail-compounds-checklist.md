# Kanji Detail Compounds Checklist

Last updated: 2026-03-11
Status: Complete

## Purpose

Track the phased work for adding common compound examples to the Kanji Detail screen.

This feature should stay lightweight and fit the current Kanji Detail flow while giving the user more practical vocabulary context for the selected kanji.

## Proposed First Slice

- Show a compact list of common compounds for the current kanji inside the existing Kanji Detail screen
- Limit the first slice to a small number of items such as 3 to 5 compounds, with an upper bound of 5 visible rows
- Require each displayed compound item to include:
  - the written compound
  - the reading or pronunciation
  - a short meaning
  - a short usage field derived as a usage hint label
- Prefer compounds that look common and readable enough for lightweight study use
- Keep the section hidden when no suitable compound data is available after filtering
- Avoid turning the detail screen into a long dictionary view

## Approved Design Decision

- The first slice will use `kanjiapi.dev` as the primary compound source through `/v1/words/{kanji}`
- The `usage` field for v1 will be a lightweight usage hint derived from the available word metadata, priority markers, and meaning data already returned by the source
- The first slice will not add a second remote source for richer example sentences or usage notes
- This keeps the feature lightweight and maintainable while still satisfying the requirement that each visible compound item includes a usage field
- The compounds section should use a simple local cache with a freshness window instead of refetching on every detail open

## Approved Filtering And Presentation Rules

- Show at most 5 compounds for a kanji in the first slice
- Prefer entries that include a visible written form, a pronunciation, and at least one gloss
- Rank entries with priority markers above entries without priority markers
- Prefer shorter, more readable compounds over unusually long or obscure entries when the source returns many choices
- Define the v1 `usage` field as a short label such as `Common word`, `News-heavy`, `Formal term`, or `Study word`
- Derive the usage hint from source priority markers and meaning shape, without inventing full example sentences
- Hide the section completely if no compounds survive filtering
- Cache compound results locally and refresh them only when missing or older than the approved freshness window

## Phase 1: Scope And Data Contract

- [x] Confirm the first-slice item count, ordering rule, and empty-state behavior
- [x] Confirm the exact fields shown for each compound row and how the `usage` field is defined for v1
- [x] Confirm whether compound data should be cached locally alongside the existing kanji detail cache
- [x] Confirm acceptance criteria and manual verification expectations before implementation starts

## Phase 2: Design And Documentation

- [x] Update `docs/detail-design/kanji-detail.md` to describe the compounds section, scope, and fallback behavior
- [x] Keep `docs/project-context.md` aligned if the shipped feature set changes materially
- [x] Update this checklist with implementation notes as decisions are made

## Phase 3: Data Fetching And Storage

- [x] Introduce a lightweight model for compound entries that covers written form, reading, meaning, usage, and any ranking metadata
- [x] Implement compound fetching with the narrowest useful network behavior
- [x] Add or extend local cache storage for compound entries if the approved scope requires it
- [x] Keep the existing kanji detail, widget, and navigation flows stable

## Phase 4: Kanji Detail UI

- [x] Add a compact compounds section to the Kanji Detail screen without making the layout feel heavy
- [x] Render each approved field clearly and keep the section readable on narrow devices
- [x] Show a supportive empty state or hide the section when compound data is unavailable
- [x] Preserve the existing detail-screen behavior for stroke order, metadata, and today stats

## Phase 5: Verification

- [x] Verify the feature with the narrowest useful automated checks first
- [x] Manually verify at least: a kanji with compounds, a kanji with sparse or missing compounds, and repeated open flows using cached data
- [x] Review whether any release note or durable status update is needed after the feature lands

## Acceptance Criteria For User Verification

- [x] The Kanji Detail screen shows a lightweight list of compound examples for supported kanji
- [x] Every visible compound entry includes the written form, reading, meaning, and an approved usage field
- [x] The compounds section does not make the Kanji Detail screen feel cluttered or unstable
- [x] Existing Kanji Detail behavior continues to work after the compounds feature is added

## Progress Log

- 2026-03-11: Created the proposed checklist for adding compound examples to the Kanji Detail screen.
- 2026-03-11: Confirmed the user requirement that each displayed compound must include written form, pronunciation, meaning, and usage.
- 2026-03-11: Reviewed the current Kanji Detail implementation and identified that the existing kanji cache does not yet carry structured compound data.
- 2026-03-11: Reviewed the current `kanjiapi.dev` word endpoint as a likely first data source; it appears suitable for written form, reading, meaning, and priority hints, but not for a rich dedicated usage field.
- 2026-03-11: The approved first-slice approach is to keep `kanjiapi.dev` as the only remote source and derive a lightweight usage hint from the returned metadata instead of adding a second source.
- 2026-03-11: Refined the approved v1 contract to cap the section at 5 filtered compounds, hide the section when no suitable rows exist, and use a simple local cache with a freshness window.
- 2026-03-11: Updated the Kanji Detail design doc to describe the compounds section, filtering rules, usage-hint contract, cache freshness behavior, and hidden-section fallback.
- 2026-03-11: Added a lightweight compounds repository, local cache support, source parsing, filtering, ranking, and usage-hint derivation using `kanjiapi.dev /v1/words/{kanji}`.
- 2026-03-11: Added a compact compounds section to `KanjiDetailActivity` that renders cached compounds immediately, refreshes stale data in the background, and keeps the section hidden when no suitable rows exist.
- 2026-03-11: Verified the current implementation with `./gradlew :app:testDebugUnitTest --tests com.example.kanjiwidget.detail.KanjiCompoundRepositoryTest --tests com.example.kanjiwidget.stats.StudyStatsRepositoryTest`; manual user verification is still pending.
- 2026-03-12: The user verified the compounds feature from a test APK build, so the checklist can now be treated as complete.
