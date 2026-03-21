# Kanji Roadmap

## Purpose

Define the detailed design baseline for a static Kanji roadmap inside the app.

This document covers:
- roadmap role
- fixed progression model
- progress rules
- data dependencies
- integration points with the current launcher flow

## Scope

In scope for the first slice:
- a static JLPT-first roadmap
- Grade as supporting metadata
- roadmap progress based on completed Kanji
- a dedicated roadmap surface or entry point in the app
- recommended next batch from the current roadmap stage

Out of scope for the first slice:
- adaptive curriculum generation
- placement scoring logic beyond the separate placement-test feature
- prerequisite graphs between individual Kanji
- spaced-repetition scheduling changes

## Product Direction

The roadmap is a guidance surface, not a full course system.

The first slice should answer:
- where the learner is on the path
- what stage comes next
- what milestone remains
- what Kanji batch should be studied next

## Roadmap Model

Primary roadmap type:
- JLPT progression

Supporting metadata:
- Grade band

Recommended initial stage set:
1. `N5 Foundation`
2. `N4 Expansion`
3. `N3 Core`
4. `N2 Advanced`
5. `N1 Expert`

Each stage may expose a supporting Grade band for context, but JLPT remains the primary user-facing path model.

## Progress Model

The roadmap must use `completed Kanji` as the progress basis.

Rules:
- every roadmap stage has a fixed Kanji set
- a Kanji contributes to roadmap progress only when it meets the app-defined completion rule
- roadmap progress is `completed / total` inside the current stage
- overall roadmap progress is derived from completed Kanji across all earlier and current stages

The roadmap must not use:
- raw open count as the progress signal
- placement-test score as the progress signal

## Completion Rule

The first implementation should keep the completion rule explicit and lightweight.

Recommended implementation direction:
- define one stable completion threshold for a Kanji
- store completion status in app-owned local persistence
- avoid hidden multi-factor scoring in the first slice

If the current data model cannot express completion directly, add the narrowest new persistence required instead of trying to infer mastery from open count alone.

## Required User-Facing Elements

- current JLPT stage
- supporting Grade band
- current stage progress
- next milestone
- next JLPT stage
- recommended next batch

## Integration Direction

The roadmap should integrate with the existing app hierarchy without replacing the current lightweight launcher role.

Recommended first-slice integration:
- keep `MainActivity` lightweight
- add a clear entry point into a dedicated roadmap surface
- treat the roadmap as the durable path view
- keep the existing hero and recent-study loop intact

## Data Dependencies

Likely required data:
- fixed roadmap stage definitions
- fixed Kanji membership per stage
- per-Kanji completion state
- supporting metadata such as JLPT and Grade label

Existing useful dependencies:
- Room database for study-related persistence
- cached Kanji metadata already used by launcher, widget, and detail flows
- recent-history and study-summary surfaces already exposed through `HomeSummaryRepository`

## Implementation Notes

Likely implementation slices:
1. define the stage dataset and lookup model
2. add or expose completion persistence
3. create a roadmap repository or provider
4. add roadmap UI entry and dedicated surface
5. connect the recommended next batch to detail or review flow

## Testing Focus

First useful checks:
- stage lookup returns stable JLPT-first grouping
- progress uses completed Kanji rather than opens
- roadmap entry renders sane empty, partial, and near-complete states
- recommended next batch excludes already completed Kanji
