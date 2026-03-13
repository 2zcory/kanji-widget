# Widget Test Coverage Checklist

Last updated: 2026-03-12
Status: Merged
First released in: `n/a`
Working branch: `feature/widget-test-coverage`
Base branch: `master`
Expected merge target: `master`

## Purpose

Expand automated coverage around the widget-specific logic that currently has no direct tests, especially selection behavior, size/formatting helpers, persisted widget state, and cleanup behavior.

Keep this slice narrow. The goal is confidence around the existing widget flow, not a broad refactor.

## Scope

- Add focused automated tests for widget logic already called out in the widget design doc
- Prefer narrow JVM tests around pure or mostly pure logic before considering heavier Android or integration coverage
- Avoid changing shipped widget behavior unless a test exposes a real bug that needs a small follow-up fix

## Preconditions

- [x] Confirm `master` is clean and up to date with `origin/master` before creating the working branch
- [x] Confirm the working branch name for this task
- [x] Confirm the initial acceptance criteria and manual verification expectations before implementation starts

## Phase 1: Scope And Design Alignment

- [x] Review the current widget implementation and map the highest-value test seams
- [x] Confirm which widget helpers can be tested as pure JVM tests without introducing unnecessary framework weight
- [x] Record any blockers or testability gaps in the progress log before implementation starts

## Phase 2: Implementation

- [x] Add focused tests for random selection avoiding immediate repeat when possible
- [x] Add focused tests for size-class resolution and footer meta formatting if those helpers are testable in isolation
- [x] Add focused tests for per-widget preference read/write and cleanup on widget deletion
- [x] Keep implementation changes minimal and aligned with the current widget design doc

## Phase 3: Verification

- [x] Run the narrowest useful automated checks first
- [x] Review whether any widget logic needed small production changes to become testable
- [x] Leave implementation items unchecked until user verification passes if the repository workflow requires explicit confirmation

## Phase 4: Review Flow

- [x] Push the working branch to `origin`
- [x] Open a Pull Request from the working branch into `master`
- [x] Ask for user review only after the work is ready and the Pull Request is open
- [x] Merge into `master` only after user review passes

## Phase 5: Post-Merge

- [x] Sync local `master` only from a clean worktree after the Pull Request merge
- [x] Update durable project context or status docs if the shipped behavior or testing posture changed materially

## Acceptance Criteria For User Verification

- [x] The repository has direct automated coverage for the highest-risk widget logic currently called out in the widget design doc
- [x] The new tests are narrow, maintainable, and runnable with the existing local Gradle test workflow
- [x] Existing widget behavior remains unchanged unless a separately documented bugfix is required

## Progress Log

- 2026-03-12: Created the proposed checklist for the widget test coverage backlog item identified during technical backlog triage.
- 2026-03-12: Confirmed `master` was clean and in sync with `origin/master`, then created branch `feature/widget-test-coverage`.
- 2026-03-12: Identified the highest-value seams as widget random selection in `KanjiRefreshWorker`, widget size/meta helpers in `KanjiAppWidgetProvider`, and persisted state plus cleanup behavior in `KanjiWidgetPrefs`.
- 2026-03-12: The current testability plan is to start with pure or mostly pure JVM coverage and avoid heavier framework setup unless a helper cannot be isolated cleanly.
- 2026-03-12: Noted a separate cache-layer risk outside this task: `KanjiWidgetPrefs.getCachedCompounds(...)` still drops rows with blank readings even though shipped detail behavior now keeps those rows visible.
- 2026-03-12: Extracted pure widget helpers for size classification, metadata formatting, and next-index selection so they can be covered by local JVM tests without pulling the whole widget provider into test setup.
- 2026-03-12: Added widget-focused unit tests for selection logic, size/meta helpers, widget-scoped preference state, alpha clamping, and cleanup behavior.
- 2026-03-12: Tried a Robolectric-based path for `SharedPreferences` coverage first, but dropped it after native-link failures on the current Android/Linux arm64 host and replaced it with a pure in-memory test context.
- 2026-03-12: Verified the new widget tests with `./gradlew testDebugUnitTest --tests com.example.kanjiwidget.widget.KanjiWidgetLogicTest --tests com.example.kanjiwidget.widget.KanjiWidgetPrefsTest`.
- 2026-03-12: Re-ran the full local unit test suite with `./gradlew testDebugUnitTest` and it passed on branch `feature/widget-test-coverage`.
- 2026-03-12: Opened PR #3 from `feature/widget-test-coverage` into `master` after the test work and verification were ready for review.
- 2026-03-12: The user confirmed PR #3 passed review, so the branch was merged into `master` and local `master` was synced cleanly afterward.
- 2026-03-12: `First released in` stays `n/a` because this task only adds automated coverage and does not change shipped user-facing behavior.
