# Project Context

Last updated: 2026-03-11

## Goal

Build and maintain an Android app and home screen widget for lightweight Kanji review.

## Scope

- Android application and resizable home screen widget
- Kanji detail view with stroke-order playback
- Recent study history and daily study-time tracking
- Product and implementation design documents under `docs/`

## Constraints

- Kotlin + Gradle Kotlin DSL project
- External data sources include `kanjiapi.dev` and KanjiVG
- Keep repository documentation in English
- Keep design docs aligned with feature-level behavior changes

## Current Status

- Main app, widget flow, recent Kanji history, and study statistics are implemented
- Detailed design documents already exist for major shipped features
- The repository currently has active uncommitted feature work; documentation updates should avoid overwriting that work

## Working Notes

- Use this file as the first stop for durable project context inside the repo
- Add or update detailed design docs in `docs/detail-design/` when a feature changes materially
