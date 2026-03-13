# Widget Daily Rotation First Slice Checklist

Last updated: 2026-03-13
Status: Ready for PR review
First released in: `-`
Working branch: `feature/widget-daily-rotation-first-slice`
Base branch: `master`
Expected merge target: `master`

## Purpose

Add a lightweight daily-rotation slice for the home screen widget so returning to the launcher on a new day can feel fresher without turning the widget into a scheduler-heavy background feature.

Keep this checklist focused on one complex task or initiative that may span multiple sessions.

Use `First released in: \`n/a\`` if this task does not affect shipped app behavior and should not be tracked to a release tag.

## Scope

- Add a simple daily-rotation rule so a widget can advance to a fresh Kanji after the local calendar day changes
- Keep the existing reveal-and-next interaction intact and avoid introducing exact alarms or frequent autonomous background work in the first slice
- Define whether the first slice should use one shared setting, one per-widget setting, or a fixed default behavior before implementation starts
- Update the widget design doc before code changes if the proposed behavior is approved

## Preconditions

- [x] Confirm `master` is clean and up to date with `origin/master` before creating the working branch
- [x] Confirm the working branch name for this task
- [x] Confirm the initial acceptance criteria and manual verification expectations before implementation starts

## Phase 1: Scope And Design Alignment

- [x] Review the current widget refresh and placement behavior against the proposed daily-rotation slice
- [x] Decide the first-slice product rule for rotation enablement, such as shared default, per-widget setting, or always-on behavior
- [x] Update or create the necessary design or product docs before implementation when behavior changes materially
- [x] Record key constraints, assumptions, or external dependencies in the progress log

## Phase 2: Implementation

- [x] Implement the approved daily-rotation first slice on the working branch
- [x] Keep the implementation aligned with the current design docs and repository rules
- [x] Add or update the narrowest useful automated checks for the changed behavior

## Phase 3: Verification

- [x] Run the narrowest useful automated or build checks first
- [x] Manually verify the key scenarios on the working branch
- [x] Leave implementation items unchecked until user verification passes if the repository workflow requires explicit user confirmation

## Phase 4: Review Flow

- [x] Build the review artifact required for this repository before asking for final review
- [ ] Push the working branch to `origin`
- [ ] Open a Pull Request from the working branch into the expected merge target
- [ ] Ask for user review only after the review artifact has been sent and the Pull Request is open
- [ ] Merge into the expected merge target only after user review passes

## Phase 5: Post-Merge

- [ ] Sync local `master` or the expected merge target only from a clean worktree after the Pull Request merge
- [ ] Update durable project context or status docs if the shipped feature set changed materially

## Android Review Artifact

- [x] Build a clearly labeled review APK or other Android review artifact if the task changes app behavior
- [x] Move the review APK to `/sdcard/Download` when that workflow applies
- [x] Record the delivered filename in the progress log

## Acceptance Criteria For User Verification

- [x] A widget that has already shown a Kanji on one local day can advance to a fresh Kanji when the next local day is reached without requiring the user to remove and re-add the widget
- [x] The existing reveal-and-next interaction still works after the daily-rotation logic is introduced
- [x] The first slice does not rely on exact alarms, aggressive background scheduling, or a heavier always-running service
- [x] Existing widgets created before this slice continue to behave safely under the approved default rotation rule

## Progress Log

- 2026-03-13: Confirmed `master` was clean and in sync with `origin/master`, created branch `feature/widget-daily-rotation-first-slice`, and created this proposed checklist from the standardized complex-task template.
- 2026-03-13: Promoted `scheduled daily rotation` from `technical-backlog.md` into a dedicated proposed task because it appears to offer the best value-to-scope ratio among the current widget future extensions.
- 2026-03-13: Approved the first-slice direction as `always on` so daily rotation can be validated without adding another shared setting or another per-widget placement control in the same slice.
- 2026-03-13: The first slice will stay host-driven: it may refresh to a new Kanji after the local calendar day changes when the widget is rendered or otherwise refreshed, but it will not introduce exact alarms, high-frequency periodic work, or an always-running background service.
- 2026-03-13: Updated `docs/detail-design/widget.md` for the approved always-on daily-rotation slice, including the host-driven refresh rule, the hidden-answer reset expectation, and the explicit non-goals for alarms or heavy background scheduling.
- 2026-03-13: Implemented a narrow first slice by recording the last local day a widget loaded a Kanji, backfilling a safe baseline for legacy widgets, and enqueueing an advance only when a normal render or interaction happens after the day changes.
- 2026-03-13: Verified the current logic with `./gradlew :app:testDebugUnitTest --tests com.example.kanjiwidget.widget.KanjiWidgetLogicTest --tests com.example.kanjiwidget.widget.KanjiWidgetPrefsTest`, which passed locally on this branch.
- 2026-03-13: Built `./gradlew :app:assembleDebug` successfully and copied the review APK to `/sdcard/Download/kanji_widget_daily_rotation_review.apk` for manual verification on-device.
- 2026-03-13: Found and fixed one scheduling bug in `KanjiAppWidgetProvider.onUpdate()`: a same-cycle default refresh could overwrite the intended `advance = true` request for a new-day rotation. Re-ran the widget unit tests and `./gradlew :app:assembleDebug` after the fix, then refreshed `/sdcard/Download/kanji_widget_daily_rotation_review.apk`.
- 2026-03-13: The user verified the review APK against the first-slice expectations, so the implementation, verification, review-artifact, and acceptance-criteria items can now be treated as complete before the PR is opened.
- 2026-03-13: Update `First released in` after the first tagged release that includes this merged work.
