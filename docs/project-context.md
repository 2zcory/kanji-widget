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
- The lightweight stats-improvement slice is complete, including active-day insight, current-streak feedback, clearer range-aware summary copy, and a supportive no-data summary state in the existing stats bottom sheet
- Detailed design documents now exist for the major shipped features, including the Kanji Detail screen
- The repository has `v1.0.0` and `v1.1.0` published, and `v1.1.1` is the current patch release for the improved study stats bottom sheet
- GitHub Actions workflows now cover debug APK builds and signed release builds
- The phased Kanji Detail screen update is complete, including layout, metadata, study stats, next-random navigation, and related design docs

## Working Notes

- Use this file as the first stop for durable project context inside the repo
- Add or update detailed design docs in `docs/detail-design/` when a feature changes materially
- Release signing remains secret-backed and must not be committed into the repository
- The release workflow expects `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` in GitHub Secrets
- Any machine-specific `android.aapt2FromMavenOverride` configuration should stay outside the repository so CI can use the default toolchain
- The Kanji Detail checklist remains useful as the implementation record for the completed phased update
