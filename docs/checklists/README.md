# Checklists

This folder stores temporary work checklists for tasks that may span multiple sessions.

## Structure

- Keep in-progress or not-yet-finished checklists directly in this folder.
- Move completed checklists into `archieved/`.
- Keep reusable templates such as `TEMPLATE-complex-task-checklist.md` in the top-level `docs/checklists/` folder.
- Treat feature checklists as implementation-and-merge records, not as the primary release tracker.

## Usage

- Use one checklist file per task or initiative.
- For a new complex task, start by copying `TEMPLATE-complex-task-checklist.md` and rename it to match the task.
- For release-tracked feature work, keep `First released in` as `-` until the first tagged release that includes the merged work is published.
- Use `First released in: \`n/a\`` for merged tasks that do not affect shipped app behavior, such as internal test-only, docs-only, or process-only work.
- Replace the placeholder title, branch fields, scope, acceptance criteria, and progress log entries before implementation starts.
- Update the checklist after each completed task so progress is easy to recover in a later session.
- After the work is merged, move the checklist into `archieved/` instead of deleting it, even if the next release has not been cut yet.
- Use `docs/releases/vX.Y.Z.md` as the source of truth for what shipped in each tagged release.
- After a release is published, backfill `First released in` in each related archived checklist.
