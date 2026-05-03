# Mathwrite

Mathwrite is a local prototype for writing math on a Samsung tablet and pasting the recognized LaTeX into the active Windows text cursor.

## Pieces

- `android/`: Samsung tablet handwriting surface built with Jetpack Compose.
- `companion/`: Windows companion app that receives LaTeX and sketch paste requests over the local network.
- `docs/`: design notes and implementation plan.

## Local Setup

1. Copy `android/local.properties.example` to `android/local.properties`.
2. Set `sdk.dir`, `mathpix.appId`, and `mathpix.appKey` in `android/local.properties`.
3. Build the Windows companion with `dotnet build Mathwrite.sln`.
4. Build the Android app from `android/` with Gradle.
5. Start the Windows companion and allow it through Windows Firewall on private networks if prompted.
6. Connect the tablet and laptop to the same network.
7. In the tablet app, scan for the companion or enter the laptop IP shown in the companion window.

The Android app sends math strokes to Mathpix `v3/strokes`; the Windows companion pastes the returned LaTeX into the focused app. Sketch mode renders the tablet drawing board to a PNG and asks the companion to paste that image into the active Windows field.
