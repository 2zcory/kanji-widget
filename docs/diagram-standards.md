# Diagram Standards

Last updated: 2026-03-13

## Purpose

Define the minimum diagram expectations for project documentation so feature behavior, architecture, and release flow stay easy to recover across sessions.

## Format

- Prefer Mermaid diagrams embedded directly in Markdown.
- Keep diagrams small and task-oriented rather than trying to describe the whole product in one graphic.
- Update the nearest relevant diagram when behavior changes materially.

## Required Diagram Types By Document

### `docs/basic-design.md`

Minimum diagrams:
- one `flowchart` for the top-level user journey across widget, main screen, detail screen, and stats

Add more diagrams only when the product structure changes enough that prose becomes hard to follow.

### `docs/detail-design/*.md`

Minimum diagrams:
- one `sequenceDiagram` or feature-level `flowchart` for the main interaction path

Recommended additions when relevant:
- storage interaction diagram for local persistence-heavy features
- state diagram for features with explicit loading, empty, revealed, or error states

Use the smallest diagram that makes the feature behavior unambiguous.

### `docs/releases/*.md`

No diagram required by default.

Exception:
- add a small rollout or migration flow only if the release includes operational steps that are easy to perform incorrectly

## Practical Rules

- Do not add diagrams just to satisfy the rule when the document is extremely small or temporary.
- Prefer one maintained diagram over several stale diagrams.
- Keep node labels short and use surrounding prose for detail.
- Avoid duplicating the same diagram across multiple documents; instead, keep the canonical version in the most relevant document.

## Adoption Rule

- New durable design documents should follow this standard immediately.
- Existing documents should be updated gradually when they are next revised for meaningful feature work.
