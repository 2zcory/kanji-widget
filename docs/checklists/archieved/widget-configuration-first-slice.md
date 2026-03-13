# Widget Configuration First Slice Checklist

Last updated: 2026-03-13
Status: Merged
Working branch: `feature/widget-configuration-first-slice`
Base branch: `master`
Expected merge target: `master`

## Purpose

Add a lightweight widget configuration activity that appears during widget placement, gives the user one meaningful per-widget setup choice, and establishes the app's durable entry point for future widget-specific customization without forcing a broad settings redesign in the same slice.

## Scope

- Add an Android widget configuration activity wired into the widget provider metadata
- Let the user choose one per-widget background opacity preset during placement
- Save the selected opacity per widget instance instead of relying only on one global opacity value
- Keep the existing widget reveal-and-next behavior unchanged after placement completes
- Update the widget design doc and durable project context if the shipped widget setup flow changes materially

## Preconditions

- [x] Confirm `master` is clean and up to date with `origin/master` before creating the working branch
- [x] Confirm the working branch name for this task
- [x] Confirm the initial acceptance criteria and manual verification expectations before implementation starts

## Phase 1: Scope And Design Alignment

- [x] Review the relevant current widget behavior, backlog notes, and implementation constraints
- [x] Update or create the necessary design or product docs before implementation when behavior changes materially
- [x] Record key constraints, assumptions, or external dependencies in the progress log

## Phase 2: Implementation

- [x] Add the configuration activity and wire it into widget placement
- [x] Implement per-widget opacity persistence with safe fallback behavior for existing widgets
- [x] Keep the existing widget interaction flow and main-screen behavior coherent after the new setup path lands
- [x] Keep the implementation aligned with the current design docs and repository rules
- [x] Add or update the narrowest useful automated checks for the changed behavior

## Phase 3: Verification

- [x] Run the narrowest useful automated or build checks first
- [x] Manually verify widget placement, configured opacity, existing-widget fallback behavior, and post-placement refresh
- [x] Leave implementation items unchecked until user verification passes if the repository workflow requires explicit user confirmation

## Phase 4: Review Flow

- [x] Build the review artifact required for this repository before asking for final review
- [x] Push the working branch to `origin`
- [x] Open a Pull Request from the working branch into the expected merge target
- [x] Ask for user review only after the review artifact has been sent and the Pull Request is open
- [x] Merge into the expected merge target only after user review passes

## Phase 5: Post-Merge And Release

- [x] Sync local `master` or the expected merge target only from a clean worktree after the Pull Request merge
- [x] Update durable project context or status docs if the shipped feature set changed materially
- [ ] Prepare release notes only after the reviewed work has been merged
- [ ] Complete the release steps required by the repository workflow

## Android Review Artifact

- [x] Build a clearly labeled review APK or other Android review artifact if the task changes app behavior
- [x] Move the review APK to `/sdcard/Download` when that workflow applies
- [x] Record the delivered filename in the progress log

## Acceptance Criteria For User Verification

- [x] Adding a new widget opens a lightweight configuration activity before placement completes
- [x] The configuration activity lets the user choose a background-opacity preset for that widget instance
- [x] The selected opacity applies to the configured widget without changing other widget instances unexpectedly
- [x] Existing widgets created before this change continue to render safely with a reasonable opacity fallback
- [x] The reveal, next, resize, and detail-open widget interactions still work after the configuration flow is introduced

## Progress Log

- 2026-03-13: Confirmed `master` was clean and up to date with `origin/master`, created branch `feature/widget-configuration-first-slice`, and reviewed backlog notes plus the current widget provider and preference model.
- 2026-03-13: Chose `configuration activity` over a standalone `per-widget opacity` slice because the placement flow creates a durable extension point for future widget-specific customization while also solving the current per-widget opacity need in one coherent UX entry point.
- 2026-03-13: Approved the first-slice scope for widget configuration and updated `docs/detail-design/widget.md` to define placement-time setup, per-widget opacity persistence, and safe fallback behavior for legacy widgets before implementation.
- 2026-03-13: Implemented the configuration first slice with a placement-time activity, per-widget opacity persistence, widget metadata updates, and shared-default fallback for legacy widgets.
- 2026-03-13: Verified the slice with `./gradlew :app:testDebugUnitTest --tests com.example.kanjiwidget.widget.KanjiWidgetPrefsTest`, `./gradlew :app:compileDebugKotlin`, and `./gradlew :app:assembleDebug`, then delivered review APK `/sdcard/Download/kanji_widget_widget_configuration_review.apk`.
- 2026-03-13: Pushed branch `feature/widget-configuration-first-slice` to `origin` and opened PR `#6` for review: `https://github.com/2zcory/kanji-widget/pull/6`.
- 2026-03-13: User confirmed the review passed. PR `#6` was merged into `master`, local `master` was fast-forwarded to the merge commit, and the checklist was archived.
