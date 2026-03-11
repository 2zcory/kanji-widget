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

## GitHub Actions

The repository includes:
- `.github/workflows/build-debug.yml` for debug APK builds on push and pull request
- `.github/workflows/release.yml` for signed release APK builds on tags such as `v1.0.1`

Required GitHub Secrets for the release workflow:
- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

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

- [`docs/basic-design.md`](docs/basic-design.md)
- [`docs/detail-design/daily-study-time-tracking.md`](docs/detail-design/daily-study-time-tracking.md)
- [`docs/detail-design/main-screen.md`](docs/detail-design/main-screen.md)
- [`docs/detail-design/study-time-chart.md`](docs/detail-design/study-time-chart.md)
