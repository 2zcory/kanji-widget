# Task Title Checklist

Last updated: YYYY-MM-DD
Status: Proposed
Working branch: `feature/example-task`
Base branch: `master`
Expected merge target: `master`

## Purpose

Describe the task in one short paragraph.

Keep this checklist focused on one complex task or initiative that may span multiple sessions.

## Scope

- Summarize the intended first slice or approved scope
- Note the main feature or behavior changes
- Keep this scoped to one task instead of a broad roadmap

## Preconditions

- [ ] Confirm `master` is clean and up to date with `origin/master` before creating the working branch
- [ ] Confirm the working branch name for this task
- [ ] Confirm the initial acceptance criteria and manual verification expectations before implementation starts

## Phase 1: Scope And Design Alignment

- [ ] Review the relevant current UX or behavior
- [ ] Update or create the necessary design or product docs before implementation when behavior changes materially
- [ ] Record key constraints, assumptions, or external dependencies in the progress log

## Phase 2: Implementation

- [ ] Implement the approved first slice on the working branch
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

- [ ] Criterion 1
- [ ] Criterion 2
- [ ] Criterion 3

## Progress Log

- YYYY-MM-DD: Created the proposed checklist and confirmed the working branch for this task.
