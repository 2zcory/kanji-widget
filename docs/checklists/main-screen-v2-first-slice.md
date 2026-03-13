# Main Screen V2 First Slice Checklist

Last updated: 2026-03-13
Status: Proposed
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
- [ ] Confirm the initial acceptance criteria and manual verification expectations before implementation starts

## Phase 1: Scope And Design Alignment

- [x] Review the relevant current UX or behavior
- [ ] Update or create the necessary design or product docs before implementation when behavior changes materially
- [x] Record key constraints, assumptions, or external dependencies in the progress log

## Phase 2: Implementation

- [ ] Rework the main-screen layout for the approved hero-first hierarchy
- [ ] Reduce duplicated main-screen actions so the primary CTA appears in one clearly dominant place
- [ ] Refine recent-history presentation and utility-card copy without adding new heavy data dependencies
- [ ] Keep the implementation aligned with the current design docs and repository rules
- [ ] Add or update the narrowest useful automated checks for the changed behavior

## Phase 3: Verification

- [ ] Run the narrowest useful automated or build checks first
- [ ] Manually verify the key scenarios on the working branch
- [ ] Leave implementation items unchecked until user verification passes if the repository workflow requires explicit user confirmation

## Phase 4: Review Flow

- [ ] Build the review artifact required for this repository before asking for final review
- [ ] Push the working branch to `origin`
- [ ] Open a Pull Request from the working branch into the expected merge target
- [ ] Ask for user review only after the review artifact has been sent and the Pull Request is open
- [ ] Merge into the expected merge target only after user review passes

## Phase 5: Post-Merge And Release

- [ ] Sync local `master` or the expected merge target only from a clean worktree after the Pull Request merge
- [ ] Update durable project context or status docs if the shipped feature set changed materially
- [ ] Prepare release notes only after the reviewed work has been merged
- [ ] Complete the release steps required by the repository workflow

## Android Review Artifact

- [ ] Build a clearly labeled review APK or other Android review artifact if the task changes app behavior
- [ ] Move the review APK to `/sdcard/Download` when that workflow applies
- [ ] Record the delivered filename in the progress log

## Acceptance Criteria For User Verification

- [ ] The main screen opens with a clearer top-level hierarchy that emphasizes today summary and a single dominant study CTA
- [ ] The latest-study action is not visually duplicated across multiple equally prominent sections
- [ ] Recent Kanji remains easy to reopen but does not compete with the primary CTA
- [ ] Empty-state and no-widget-state copy remains useful and clearly guides the next step
- [ ] Widget controls and language controls feel secondary to learning actions while staying easy to access

## Progress Log

- 2026-03-13: Confirmed `master` was clean and in sync with `origin/master`, created branch `feature/main-screen-v2-first-slice`, reviewed the existing main-screen design and implementation, and scoped the first slice around hierarchy, CTA clarity, and lighter utility sections.
