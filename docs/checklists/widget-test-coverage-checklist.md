# Widget Test Coverage Checklist

Last updated: 2026-03-12
Status: Proposed
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

- [ ] Confirm `master` is clean and up to date with `origin/master` before creating the working branch
- [ ] Confirm the working branch name for this task
- [ ] Confirm the initial acceptance criteria and manual verification expectations before implementation starts

## Phase 1: Scope And Design Alignment

- [ ] Review the current widget implementation and map the highest-value test seams
- [ ] Confirm which widget helpers can be tested as pure JVM tests without introducing unnecessary framework weight
- [ ] Record any blockers or testability gaps in the progress log before implementation starts

## Phase 2: Implementation

- [ ] Add focused tests for random selection avoiding immediate repeat when possible
- [ ] Add focused tests for size-class resolution and footer meta formatting if those helpers are testable in isolation
- [ ] Add focused tests for per-widget preference read/write and cleanup on widget deletion
- [ ] Keep implementation changes minimal and aligned with the current widget design doc

## Phase 3: Verification

- [ ] Run the narrowest useful automated checks first
- [ ] Review whether any widget logic needed small production changes to become testable
- [ ] Leave implementation items unchecked until user verification passes if the repository workflow requires explicit confirmation

## Phase 4: Review Flow

- [ ] Push the working branch to `origin`
- [ ] Open a Pull Request from the working branch into `master`
- [ ] Ask for user review only after the work is ready and the Pull Request is open
- [ ] Merge into `master` only after user review passes

## Phase 5: Post-Merge And Release

- [ ] Sync local `master` only from a clean worktree after the Pull Request merge
- [ ] Update durable project context or status docs if the shipped behavior or testing posture changed materially
- [ ] Prepare release notes only after the reviewed work has been merged
- [ ] Complete any release steps required by the repository workflow if this task materially changes shipped behavior

## Acceptance Criteria For User Verification

- [ ] The repository has direct automated coverage for the highest-risk widget logic currently called out in the widget design doc
- [ ] The new tests are narrow, maintainable, and runnable with the existing local Gradle test workflow
- [ ] Existing widget behavior remains unchanged unless a separately documented bugfix is required

## Progress Log

- 2026-03-12: Created the proposed checklist for the widget test coverage backlog item identified during technical backlog triage.
