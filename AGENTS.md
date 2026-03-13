# Agent Instructions

## Purpose

This file defines how an AI coding agent should operate in this repository.

## Startup

1. Read `docs/project-context.md` first for durable project context.
2. Read `docs/basic-design.md` when the task may affect product behavior or UX.
3. Read the relevant file in `docs/detail-design/` before changing an implemented feature.
4. Read any relevant in-progress checklist in `docs/checklists/` when continuing multi-session implementation work.
5. Read `docs/checklists/technical-backlog.md` when the user asks for backlog direction or the next technical priority.

## Repository Rules

- Keep repository documentation in English.
- Keep code changes aligned with the current design documents.
- Update the relevant design doc when a feature-level behavior changes materially.
- Do not commit secrets, signing files, or machine-specific local overrides.
- Keep any machine-specific Android build configuration outside the repository.

## Engineering Preferences

- Prefer clear, low-maintenance Kotlin and Android implementations.
- Avoid unnecessary dependencies.
- Preserve existing project structure unless there is a clear reason to change it.
- Validate behavior with the narrowest useful command first, then broaden if needed.

## Release And Security Notes

- Release signing is secret-backed and must stay out of the repository.
- The release workflow depends on repository secrets configured in GitHub Actions.

## Working Agreement

- Use `docs/project-context.md` as the durable project summary.
- If the current task changes scope, constraints, or status in a lasting way, update `docs/project-context.md`.
- If the current task changes a shipped feature materially, update the corresponding detail design document under `docs/detail-design/`.
- Use `docs/checklists/` for temporary multi-session work tracking.
- For a new complex task, start from `docs/checklists/TEMPLATE-complex-task-checklist.md` unless a more specific checklist already exists.
- Keep active checklists in `docs/checklists/` and move completed ones into `docs/checklists/archieved/`.
- Update the relevant checklist whenever a tracked task is completed so progress is recoverable in later sessions.
