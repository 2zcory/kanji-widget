# Checklist Template Standardization

Last updated: 2026-03-12
Status: In review
Working branch: `chore/checklist-template`
Base branch: `master`
Expected merge target: `master`

## Purpose

Standardize a reusable checklist template for complex tasks in this repository.

The template should reduce repeated setup work and make the current branch, PR, review APK, merge, and release flow explicit in every future complex task.

## Proposed Scope

- Add one reusable checklist template file under `docs/checklists/`
- Update the checklist guidance so future tasks know when and how to copy the template
- Keep the template focused on the current repository workflow instead of creating a generic project-management system
- Avoid rewriting completed historical checklists unless a minimal note is needed

## Phase 1: Template Contract

- [x] Confirm the template sections and fields for branch name, base branch, expected merge target, acceptance criteria, progress log, PR, APK review, merge, and release
- [x] Confirm how the template should distinguish implementation completion from user verification and release readiness
- [x] Confirm whether the template should include optional sections for Android-specific review APK and release flow

## Phase 2: Repository Docs

- [x] Add the reusable template file in `docs/checklists/`
- [x] Update `docs/checklists/README.md` to explain how to start a new task from the template
- [x] Update `AGENTS.md` only if the repository-level instructions need an explicit note about using the new template

## Phase 3: Verification

- [x] Verify the template reads clearly and matches the branch-review-merge-release workflow already used in this repository
- [ ] Review whether `docs/project-context.md` needs a durable note about the standardized checklist template

## Acceptance Criteria

- [x] A future complex task can start by copying one checklist template instead of rebuilding workflow sections manually
- [x] The template makes branch, PR, APK review, merge, and release steps explicit
- [x] The template matches the current repository workflow without forcing unnecessary process on simple tasks

## Progress Log

- 2026-03-12: Created the proposed checklist for standardizing a reusable complex-task checklist template in this repository.
- 2026-03-12: Added the reusable template file, updated the checklist README, and added a short AGENTS note to point new complex tasks toward the template.
- 2026-03-12: The user reviewed the documentation changes and confirmed they pass review on branch `chore/checklist-template`.
