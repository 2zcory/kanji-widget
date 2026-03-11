# Stats Improvement Checklist

Last updated: 2026-03-11
Status: In progress

## Purpose

Track the phased implementation work for improving the study stats experience across multiple sessions.

This checklist focuses on a lightweight but more useful stats surface by adding clearer insights, better empty states, and supporting repository and test updates without expanding into a full analytics dashboard.

## Phase 1: Scope And Design Alignment

- [ ] Review the current stats UX and confirm the first implementation slice stays limited to streak, active-days insight, clearer summary copy, and improved empty-state behavior
- [ ] Update the relevant design docs to describe the new stats insights, scope boundaries, and bottom-sheet behavior
- [ ] Confirm the acceptance criteria and manual verification expectations for the improved stats experience before code changes begin

## Phase 2: Stats Data Model And Repository Updates

- [ ] Extend the stats-layer models to support additional derived insights such as active study days and current streak
- [ ] Implement repository logic for computing the new insights from the existing local study-time data
- [ ] Keep the new logic aligned with the current local-only storage contract and avoid introducing unnecessary persistence changes
- [ ] Add or refine narrow unit-test coverage for range generation, summary calculation, and streak or active-day computation

## Phase 3: Stats Bottom Sheet UX Improvements

- [ ] Update the stats bottom-sheet UI to present the new insight fields alongside the existing chart summary
- [ ] Improve summary copy so the selected range and resulting metrics are easier to understand at a glance
- [ ] Improve the empty-state experience so no-data scenarios still feel informative and encouraging
- [ ] Preserve the lightweight main-screen flow and keep the stats experience inside the existing bottom sheet

## Phase 4: Verification And Documentation

- [ ] Verify the updated stats behavior with the narrowest useful build or test command first
- [ ] Manually verify the key scenarios: no data, sparse data, mixed recent data, and range switching
- [ ] Update the checklist progress log with implementation and verification notes
- [ ] Review whether `docs/project-context.md` needs a durable status update after the feature work lands

## Acceptance Criteria For User Verification

- [ ] The stats bottom sheet clearly communicates the selected chart range and related summary metrics
- [ ] The stats surface shows additional lightweight insights derived from existing data without feeling like a heavy dashboard
- [ ] Empty-state stats remain readable and useful when no study data exists
- [ ] Existing chart and ranking behavior continues to work after the stats improvements

## Progress Log

- 2026-03-11: Created the proposed phased checklist for the stats improvement initiative.
- 2026-03-11: Updated the study-time-chart and main-screen design docs to define the first stats-improvement slice around active days, current streak, and clearer empty-state behavior.
- 2026-03-11: Extended `StudyStatsRepository` with derived summary metrics for active study days and the current streak, while keeping the existing local storage contract unchanged.
- 2026-03-11: Added focused unit tests for ordered range output, zero-data handling, active-day counts, and current-streak calculation.
- 2026-03-11: Attempted verification with `./gradlew.bat :app:testDebugUnitTest`, but the sandbox blocked Gradle distribution download because network access is unavailable.
