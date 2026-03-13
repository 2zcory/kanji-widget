# Kanji Detail Audio Checklist

Last updated: 2026-03-12
Status: Merged
First released in: `v1.3.0`
Working branch: `feature/kanji-detail-audio`

## Purpose

Track the phased work for adding pronunciation playback to the Kanji Detail experience.

This feature should stay lightweight, fit the current local-first app model, and avoid introducing a second remote media source for the first slice.

## Proposed First Slice

- Add pronunciation playback controls to the existing Kanji Detail screen
- Use Android `TextToSpeech` as the v1 audio engine instead of fetching remote audio files
- Support playback for the current kanji detail reading and compound-example readings when available
- Keep the first slice resilient when Japanese TTS data is unavailable on the device
- Avoid adding cloud sync, remote audio hosting, or downloadable voice assets in v1

## Approved Direction To Confirm

- Preferred v1 engine: Android `TextToSpeech`
- Preferred language target: Japanese voice data when available on-device
- Preferred fallback: show a clear unavailable state or disable the action when Japanese TTS cannot be initialized
- Main reading playback rule for v1: prefer `onyomi` when it is available; if `onyomi` is missing, fall back to `kunyomi`
- Audio interaction rule for v1: only one playback may run at a time, and starting a new playback should stop the current one first
- Playback controls should stay hidden or disabled when the target reading is blank or only showing placeholder text

## Phase 1: Scope And UX Contract

- [x] Confirm the exact playback targets for v1, such as main reading playback, compound-row playback, or both
- [x] Confirm where the play controls should appear in the Kanji Detail UI
- [x] Confirm the fallback behavior when Japanese TTS is unavailable or initialization fails
- [x] Confirm the main-reading selection rule for v1 playback
- [x] Confirm the single-playback interaction rule and hidden-or-disabled behavior for missing readings
- [x] Confirm acceptance criteria and manual verification expectations before implementation starts

## Phase 2: Design And Documentation

- [x] Update `docs/detail-design/kanji-detail.md` to describe the audio controls, supported playback targets, and failure handling
- [x] Keep `docs/project-context.md` aligned if the shipped feature set changes materially
- [x] Update this checklist with implementation notes and review status as work progresses

## Phase 3: Audio Engine And Lifecycle

- [x] Add a lightweight audio helper or repository around Android `TextToSpeech`
- [x] Handle initialization, lifecycle cleanup, and failure states safely inside the current activity flow
- [x] Avoid blocking stroke-order, study tracking, and compounds rendering while audio initializes
- [x] Keep the implementation dependency-light and local-only

## Phase 4: Kanji Detail UI

- [x] Add playback controls for the approved v1 targets without cluttering the screen
- [x] Keep disabled or unavailable states clear and stable
- [x] Preserve the existing Kanji Detail behavior for stroke order, compounds, today stats, and next-random navigation

## Phase 5: Verification And Review Flow

- [x] Verify the feature with the narrowest useful automated or build checks first
- [x] Build a review APK and send it for user review before merge
- [x] Create a Pull Request only after the work is ready for review and the review APK has already been sent
- [x] Merge into `master` only after user review passes
- [x] Prepare release notes only after the reviewed work is merged

## Acceptance Criteria For User Verification

- [x] The Kanji Detail screen exposes clear pronunciation playback controls for the approved v1 targets
- [x] Audio playback works when Japanese TTS support is available on the device
- [x] The screen handles unavailable TTS support gracefully without breaking the rest of the detail flow
- [x] Existing Kanji Detail behavior continues to work after audio playback is added

## Progress Log

- 2026-03-12: Created the proposed checklist for adding pronunciation playback to Kanji Detail.
- 2026-03-12: Created the dedicated working branch `feature/kanji-detail-audio` for this complex task.
- 2026-03-12: The initial implementation direction is to use Android `TextToSpeech` for a lightweight local-first audio feature instead of a remote audio source.
- 2026-03-12: Refined the v1 contract to prefer `onyomi` over `kunyomi` for the main reading playback target, allow only one active playback at a time, and hide or disable controls when a reading is unavailable.
- 2026-03-12: Refined the review flow so the review APK must be sent before the Pull Request is opened for final review.
- 2026-03-12: The approved v1 scope keeps both main-reading playback and compound-row playback in the same feature slice.
- 2026-03-12: The approved control placement is one main reading play action inside the readings section plus one small play action on each eligible compound row.
- 2026-03-12: Updated the Kanji Detail design doc for the approved audio contract and added a lightweight `TextToSpeech` controller with single-playback and lifecycle cleanup behavior.
- 2026-03-12: Added icon-based audio controls for the main reading target and eligible compound rows while keeping the rest of the detail flow stable.
- 2026-03-12: Verified the implementation with `./gradlew :app:testDebugUnitTest --tests com.example.kanjiwidget.detail.KanjiCompoundRepositoryTest --tests com.example.kanjiwidget.stats.StudyStatsRepositoryTest`.
- 2026-03-12: Built and sent the review APK `kanji-widget-debug-v1.2.0-audio-review.apk` to the user, and the user confirmed the review passed.
- 2026-03-12: Opened Pull Request #1 for `feature/kanji-detail-audio` after the review APK had already been sent and approved.
- 2026-03-12: Pull Request #1 was merged into `master` after the reviewed work passed user verification.
- 2026-03-12: The pronunciation playback feature first shipped in release `v1.3.0`.
- 2026-03-12: Prepared the follow-up `v1.3.1` release notes on `master` for the shipped Kanji Detail reading-availability fix.
- 2026-03-12: Confirmed `docs/project-context.md` reflects the shipped pronunciation playback behavior and the follow-up reading-availability fix on `master`.
