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

- Main app, widget flow, review-hub main screen, recent Kanji history, and study statistics are implemented
- Detailed design documents already exist for major shipped features
- The repository has a first public release published as `v1.0.0`
- GitHub Actions workflows now cover debug APK builds and signed release builds
- A phased Kanji Detail screen update is in progress and is tracked in `docs/checklists/kanji-detail-update-checklist.md`

## Working Notes

- Use this file as the first stop for durable project context inside the repo
- Add or update detailed design docs in `docs/detail-design/` when a feature changes materially
- Release signing remains secret-backed and must not be committed into the repository
- The release workflow expects `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` in GitHub Secrets
- Any machine-specific `android.aapt2FromMavenOverride` configuration should stay outside the repository so CI can use the default toolchain
- Current multi-session implementation focus: Kanji Detail screen improvements across layout, metadata, study stats, next-random navigation, and related design docs
- Use `docs/checklists/kanji-detail-update-checklist.md` as the session-to-session source of truth for phased progress on the Kanji Detail work
