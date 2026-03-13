# Multilanguage Support Checklist

Last updated: 2026-03-13
Status: In Progress
Working branch: `feature/multilanguage`
Base branch: `master`
Expected merge target: `master`

## Purpose

Implement multilanguage support for the app and widget, starting with EN + VI using Android string resources and adding an in-app language selector, while keeping behavior aligned with current widget-first UX.

## Scope

- Phase 1: System locale-driven localization for all UI strings (app + widget), including pluralization and formatting.
- Phase 2: In-app language selector backed by AppCompat per-app language APIs and persisted user preference, with widget refresh on language change.
- Deliver a glossary to keep translations consistent and support future language expansion.

## Preconditions

- [ ] Confirm `master` is clean and up to date with `origin/master` before creating the working branch
- [ ] Confirm the working branch name for this task
- [ ] Confirm the initial acceptance criteria and manual verification expectations before implementation starts

## Phase 1: Scope And Design Alignment

- [ ] Review current UI strings and widget layouts to identify all hardcoded or missing resource entries
- [ ] Add or update the localization design doc for string keys, glossary, and language-selection behavior
- [ ] Record key constraints, assumptions, or external dependencies in the progress log

## Phase 2: Implementation

- [ ] Move all user-facing strings into resources with consistent naming and grouping
- [ ] Add `values-vi/strings.xml` and populate required translations using the glossary
- [ ] Implement plurals and locale-aware formatting for stats and date/time strings
- [ ] Add in-app language selector UI and persistence
- [ ] Integrate AppCompat per-app language APIs and refresh widget/app surfaces on change
- [ ] Add or update narrow automated checks for localization-sensitive formatting if practical

## Phase 3: Verification

- [ ] Run the narrowest useful automated or build checks first
- [ ] Manually verify EN + VI in app and widget flows
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

- [ ] All user-facing strings in app + widget are localized via resources (no hardcoded UI strings remain).
- [ ] System locale switches between EN and VI are reflected in app + widget after refresh.
- [ ] In-app language selector switches between EN and VI without app crash and updates widget content after change.
- [ ] Pluralized/stat strings and date/time formatting are locale-correct in EN and VI.

## Progress Log

- 2026-03-13: Created the proposed checklist and captured the initial scope and acceptance criteria.
- 2026-03-13: Created working branch `feature/multilanguage` and added localization design doc `docs/detail-design/localization.md`.
- 2026-03-13: Added EN + VI string resources, plural resources, and localization helpers; updated widget layouts to remove hardcoded preview text.
- 2026-03-13: Added main-screen language section, locales config, and AppCompat locale switching flow (pending verification).
- 2026-03-13: Self-review found and fixed Android string escaping plus locale persistence/widget-context issues for pre-Android 13 devices.
- 2026-03-13: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` passed; review APK copied to `/sdcard/Download/kanji-widget-multilanguage-review.apk`.
