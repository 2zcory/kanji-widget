# Kanji Widget (Android)

An Android app centered around a home screen widget for lightweight Kanji review.

The project currently includes:
- a resizable home screen widget for random Kanji review
- a detail screen with stroke-order playback from KanjiVG data
- daily study-time tracking based on detail-screen foreground time
- a review-hub main screen with continue-learning actions, recent Kanji history, and widget controls
- a study-time chart surface for the last 7 or 30 days

## Main Features

### Widget
- Home screen app widget for quick Kanji review
- Random Kanji switching
- Adaptive layout based on widget size
- Tap the widget card to open the Kanji detail screen

### Kanji Detail Screen
- Large Kanji study card
- Onyomi / Kunyomi / meaning / notes
- Stroke-order playback using KanjiVG SVG data
- Today-only study statistics for the current Kanji
- Next-random continuation action from the detail screen

### Main Screen
- Today summary
- Continue-learning actions for the latest Kanji, a random Kanji, and study stats
- A bounded recent Kanji list for reopening recent study cards
- Widget control and setup actions

### Study Statistics
- Daily study-time tracking
- Valid session counting
- 7-day and 30-day study-time chart

## Tech Notes

- Language: Kotlin
- Target: Android SDK 34
- Min SDK: 24
- Build system: Gradle Kotlin DSL
- Network data source: `kanjiapi.dev`
- Stroke-order source: KanjiVG

## Build

### Android Studio
1. Open the project in Android Studio
2. Use JDK 17
3. Sync Gradle
4. Run the app on a device or emulator

### Command Line
```bash
./gradlew assembleDebug
```

Debug APK output:
- `app/build/outputs/apk/debug/app-debug.apk`

Release APK output:
- `app/build/outputs/apk/release/app-release.apk`

### Debug Install Behavior

- Debug builds install as a separate app because the debug variant uses the application ID suffix `.debug`
- A new debug APK can update an already installed debug APK only when both APKs are signed with the same certificate
- Local debug builds from the same host usually upgrade each other in place because they reuse that host's debug keystore
- The GitHub Actions debug workflow does not inject a shared debug keystore, so remote debug artifacts should not be assumed to upgrade an existing local debug install without uninstalling first
- Treat CI debug APKs as review artifacts unless the installed debug build is known to use the same signing identity

### Local Signed Release

1. Copy the needed keys from `local.properties.example` into your ignored `local.properties`
2. Point `release.storeFile` at your local keystore path
3. Fill `release.storePassword`, `release.keyAlias`, and `release.keyPassword`
4. Run:

```bash
./gradlew assembleRelease
```

Release notes for the current prep target:
- `docs/releases/v1.7.0.md`

## GitHub Actions

The repository includes:
- `.github/workflows/build-debug.yml` for debug APK builds on pull request and manual dispatch
- `.github/workflows/release.yml` for signed release APK builds on tags such as `v1.0.1`

Required GitHub Secrets for the release workflow:
- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Local machine note:
- if your environment needs a custom AAPT2 binary, keep `android.aapt2FromMavenOverride` in a local Gradle config outside Git instead of committing it to the repository

## How To Use

1. Install and launch the app once
2. Add the widget from the Android home screen widget picker
3. Use the widget to reveal and switch Kanji
4. Tap the widget card to open the detail screen
5. Open the app icon to access the main screen and study statistics

## Project Structure

- `app/src/main/java/com/example/kanjiwidget/widget/`: widget, API client, cache, stroke-order fetch
- `app/src/main/java/com/example/kanjiwidget/stats/`: study-time tracking and chart logic
- `app/src/main/java/com/example/kanjiwidget/home/`: main-screen summary assembly
- `app/src/main/java/com/example/kanjiwidget/history/`: recent Kanji history
- `docs/`: design documents

## Design Docs

The source repository keeps code-facing documentation only.

- [`docs/basic-design.md`](docs/basic-design.md)
- [`docs/detail-design/README.md`](docs/detail-design/README.md)
- [`docs/detail-design/daily-study-time-tracking.md`](docs/detail-design/daily-study-time-tracking.md)
- [`docs/detail-design/kanji-study-ranking.md`](docs/detail-design/kanji-study-ranking.md)
- [`docs/detail-design/kanji-detail.md`](docs/detail-design/kanji-detail.md)
- [`docs/detail-design/localization.md`](docs/detail-design/localization.md)
- [`docs/detail-design/main-screen.md`](docs/detail-design/main-screen.md)
- [`docs/detail-design/settings-screen.md`](docs/detail-design/settings-screen.md)
- [`docs/detail-design/study-time-chart.md`](docs/detail-design/study-time-chart.md)
- [`docs/detail-design/theme-system.md`](docs/detail-design/theme-system.md)
- [`docs/detail-design/widget.md`](docs/detail-design/widget.md)
- [`docs/diagram-standards.md`](docs/diagram-standards.md)
- [`docs/releases/README.md`](docs/releases/README.md)

Durable AI workflow instructions, project context, and implementation checklists now live in the private context repository `kanji-widget-ctx`.
