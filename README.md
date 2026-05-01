# Mathwrite

Mathwrite is a local prototype for writing math on a Samsung tablet and pasting the recognized LaTeX into the active Windows text cursor.

## Pieces

- `android/`: Samsung tablet handwriting surface built with Jetpack Compose.
- `companion/`: Windows companion app that receives paste requests on `127.0.0.1:18765`.
- `docs/`: design notes and implementation plan.

## Local Setup

1. Copy `android/local.properties.example` to `android/local.properties`.
2. Set `sdk.dir`, `mathpix.appId`, and `mathpix.appKey` in `android/local.properties`.
3. Build the Windows companion with `dotnet build Mathwrite.sln`.
4. Build the Android app from `android/` with Gradle.
5. With USB debugging enabled, run `adb reverse tcp:18765 tcp:18765`.
6. Start the Windows companion, then launch the Android app.

The Android app sends strokes to Mathpix `v3/strokes`; the Windows companion pastes the returned LaTeX into the focused app.
