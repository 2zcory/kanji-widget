# Main Screen V2 First Slice Checklist

Last updated: 2026-03-13
Status: Merged
First released in: `v1.5.0`
Working branch: `feature/main-screen-v2-first-slice`
Base branch: `master`
Expected merge target: `master`

## Purpose

Refresh the launcher main screen so it feels more intentional and actionable while staying lightweight and widget-first. This first slice focuses on stronger visual hierarchy, a clearer primary study action, reduced action duplication, and improved empty-state guidance without expanding the screen into a heavy dashboard.

## Scope

- Replace the current top section with a stronger hero that combines today summary, widget status, and one primary study CTA
- Reduce overlap between the hero, continue-learning actions, and recent-history content
- Improve the recent Kanji section hierarchy so it supports quick re-entry without competing with the primary CTA
- Tighten widget-controls and language sections so they read as lighter utility areas
- Update the main-screen design doc to match the approved behavior and layout direction

## Preconditions

- [x] Confirm `master` is clean and up to date with `origin/master` before creating the working branch
- [x] Confirm the working branch name for this task
- [x] Confirm the initial acceptance criteria and manual verification expectations before implementation starts

## Phase 1: Scope And Design Alignment

- [x] Review the relevant current UX or behavior
- [x] Update or create the necessary design or product docs before implementation when behavior changes materially
- [x] Record key constraints, assumptions, or external dependencies in the progress log

## Phase 2: Implementation

- [x] Rework the main-screen layout for the approved hero-first hierarchy
- [x] Reduce duplicated main-screen actions so the primary CTA appears in one clearly dominant place
- [x] Refine recent-history presentation and utility-card copy without adding new heavy data dependencies
- [x] Keep the implementation aligned with the current design docs and repository rules
- [x] Add or update the narrowest useful automated checks for the changed behavior

## Phase 3: Verification

- [x] Run the narrowest useful automated or build checks first
- [x] Manually verify the key scenarios on the working branch
- [x] Leave implementation items unchecked until user verification passes if the repository workflow requires explicit user confirmation

## Phase 4: Review Flow

- [x] Build the review artifact required for this repository before asking for final review
- [x] Push the working branch to `origin`
- [x] Open a Pull Request from the working branch into the expected merge target
- [x] Ask for user review only after the review artifact has been sent and the Pull Request is open
- [x] Merge into the expected merge target only after user review passes

## Phase 5: Post-Merge

- [x] Sync local `master` or the expected merge target only from a clean worktree after the Pull Request merge
- [x] Update durable project context or status docs if the shipped feature set changed materially

## Android Review Artifact

- [x] Build a clearly labeled review APK or other Android review artifact if the task changes app behavior
- [x] Move the review APK to `/sdcard/Download` when that workflow applies
- [x] Record the delivered filename in the progress log

## Acceptance Criteria For User Verification

- [x] The main screen opens with a clearer top-level hierarchy that emphasizes today summary and a single dominant study CTA
- [x] The latest-study action is not visually duplicated across multiple equally prominent sections
- [x] Recent Kanji remains easy to reopen but does not compete with the primary CTA
- [x] Empty-state and no-widget-state copy remains useful and clearly guides the next step
- [x] Widget controls and language controls feel secondary to learning actions while staying easy to access

## Progress Log

- 2026-03-13: Confirmed `master` was clean and in sync with `origin/master`, created branch `feature/main-screen-v2-first-slice`, reviewed the existing main-screen design and implementation, and scoped the first slice around hierarchy, CTA clarity, and lighter utility sections.
- 2026-03-13: User approved the first-slice scope and acceptance criteria. Updated `docs/detail-design/main-screen.md` to reflect the hero-first hierarchy, a single dominant study CTA, reduced action duplication, and lighter utility emphasis before implementation.
- 2026-03-13: Implemented the first-slice main-screen refresh across layout, copy, and activity binding. Verified the change with `./gradlew :app:compileDebugKotlin` and `./gradlew :app:assembleDebug`, then delivered review APK `/sdcard/Download/kanji_widget_main_screen_v2_review.apk`.
- 2026-03-13: Pushed branch `feature/main-screen-v2-first-slice` to `origin` and opened PR `#5` for review: `https://github.com/2zcory/kanji-widget/pull/5`.
- 2026-03-13: User confirmed the review passed. PR `#5` was merged into `master`, local `master` was fast-forwarded to the merge commit, and the checklist was archived.
- 2026-03-13: The merged work was first shipped in release `v1.5.0`, which also included the widget configuration first slice and the matching curated release notes.
