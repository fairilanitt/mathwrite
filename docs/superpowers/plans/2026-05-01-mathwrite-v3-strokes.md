# Mathwrite v3/strokes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first working Mathwrite MVP using Mathpix `v3/strokes` without live updates, USB transport through `adb reverse`, and a Windows companion that pastes LaTeX into the focused application.

**Architecture:** The repo contains a Windows companion and an Android app. The Android app captures S Pen strokes, calls Mathpix `v3/strokes`, formats the returned LaTeX, and posts it to the companion. The Windows companion exposes a loopback HTTP endpoint, maintains the ADB reverse bridge, validates paste requests, sets the clipboard, and sends `Ctrl+V`.

**Tech Stack:** .NET 8, Windows Forms, `HttpListener`, Win32 `SendInput`, Android Kotlin, Gradle 8.14.3, Android Gradle Plugin 8.7.3, Jetpack Compose, Kotlin coroutines, OkHttp, kotlinx.serialization.

---

## File Structure

- `Mathwrite.sln`: .NET solution for the Windows companion.
- `companion/Mathwrite.Companion.Core/`: testable request models, formatting, duplicate detection, ADB path discovery, and service interfaces.
- `companion/Mathwrite.Companion.App/`: Windows tray app, HTTP listener, clipboard paste executor, ADB bridge monitor.
- `companion/Mathwrite.Companion.Tests/`: dependency-free console test runner for core behavior.
- `android/settings.gradle.kts`: Android Gradle settings.
- `android/build.gradle.kts`: Android root build config.
- `android/app/build.gradle.kts`: Android application build config.
- `android/app/src/main/AndroidManifest.xml`: app manifest with internet permission.
- `android/app/src/main/java/com/mathwrite/app/`: Android app source for stroke capture, Mathpix client, bridge client, settings, and UI.

## Task 1: Windows Companion Solution Skeleton

**Files:**
- Create: `Mathwrite.sln`
- Create: `companion/Mathwrite.Companion.Core/Mathwrite.Companion.Core.csproj`
- Create: `companion/Mathwrite.Companion.App/Mathwrite.Companion.App.csproj`
- Create: `companion/Mathwrite.Companion.Tests/Mathwrite.Companion.Tests.csproj`

- [ ] **Step 1: Create .NET projects**

Run:

```powershell
dotnet new sln -n Mathwrite
dotnet new classlib -n Mathwrite.Companion.Core -o companion/Mathwrite.Companion.Core
dotnet new winforms -n Mathwrite.Companion.App -o companion/Mathwrite.Companion.App
dotnet new console -n Mathwrite.Companion.Tests -o companion/Mathwrite.Companion.Tests
dotnet sln Mathwrite.sln add companion/Mathwrite.Companion.Core/Mathwrite.Companion.Core.csproj
dotnet sln Mathwrite.sln add companion/Mathwrite.Companion.App/Mathwrite.Companion.App.csproj
dotnet sln Mathwrite.sln add companion/Mathwrite.Companion.Tests/Mathwrite.Companion.Tests.csproj
dotnet add companion/Mathwrite.Companion.App/Mathwrite.Companion.App.csproj reference companion/Mathwrite.Companion.Core/Mathwrite.Companion.Core.csproj
dotnet add companion/Mathwrite.Companion.Tests/Mathwrite.Companion.Tests.csproj reference companion/Mathwrite.Companion.Core/Mathwrite.Companion.Core.csproj
```

Expected: solution and three projects are created without NuGet-only test dependencies.

- [ ] **Step 2: Build the empty solution**

Run:

```powershell
dotnet build Mathwrite.sln
```

Expected: `Build succeeded`.

## Task 2: Companion Core Contracts And Formatting

**Files:**
- Create: `companion/Mathwrite.Companion.Core/PasteMode.cs`
- Create: `companion/Mathwrite.Companion.Core/PasteRequest.cs`
- Create: `companion/Mathwrite.Companion.Core/PasteResponse.cs`
- Create: `companion/Mathwrite.Companion.Core/PasteModeFormatter.cs`
- Create: `companion/Mathwrite.Companion.Core/PasteRequestValidator.cs`
- Create: `companion/Mathwrite.Companion.Core/SequenceGuard.cs`
- Modify: `companion/Mathwrite.Companion.Tests/Program.cs`

- [ ] **Step 1: Add failing tests for paste modes and validation**

Replace `companion/Mathwrite.Companion.Tests/Program.cs` with assertions that verify:

```csharp
AssertEqual("\\frac{x}{2}", PasteModeFormatter.Format("\\frac{x}{2}", PasteMode.Raw));
AssertEqual("\\(\\frac{x}{2}\\)", PasteModeFormatter.Format("\\frac{x}{2}", PasteMode.Inline));
AssertEqual("$\\frac{x}{2}$", PasteModeFormatter.Format("\\frac{x}{2}", PasteMode.DollarInline));
AssertEqual("\\[\\frac{x}{2}\\]", PasteModeFormatter.Format("\\frac{x}{2}", PasteMode.Display));
AssertTrue(PasteRequestValidator.Validate(new PasteRequest(1, "x^2", PasteMode.Raw, "mathwrite-android")).IsValid);
AssertFalse(PasteRequestValidator.Validate(new PasteRequest(0, "x^2", PasteMode.Raw, "mathwrite-android")).IsValid);
AssertFalse(PasteRequestValidator.Validate(new PasteRequest(1, "", PasteMode.Raw, "mathwrite-android")).IsValid);
```

Run:

```powershell
dotnet run --project companion/Mathwrite.Companion.Tests/Mathwrite.Companion.Tests.csproj
```

Expected: compile failure because core types do not exist yet.

- [ ] **Step 2: Implement core contracts**

Implement `PasteMode`, immutable request/response records, `PasteModeFormatter.Format`, `PasteRequestValidator.Validate`, and `SequenceGuard.TryAccept`.

Required behavior:

- `Raw` returns the trimmed LaTeX unchanged.
- `Inline` wraps with `\(` and `\)`.
- `DollarInline` wraps with `$`.
- `Display` wraps with `\[` and `\]`.
- Validation rejects `sequenceId < 1`.
- Validation rejects null, empty, or whitespace LaTeX.
- Validation accepts only `source == "mathwrite-android"` for app-originated requests.
- `SequenceGuard` rejects any sequence id less than or equal to the last accepted id.

- [ ] **Step 3: Run core tests**

Run:

```powershell
dotnet run --project companion/Mathwrite.Companion.Tests/Mathwrite.Companion.Tests.csproj
```

Expected: all assertions pass and the process prints `All companion core tests passed`.

## Task 3: Companion HTTP Server With Fake Paste Executor

**Files:**
- Create: `companion/Mathwrite.Companion.Core/IPasteExecutor.cs`
- Create: `companion/Mathwrite.Companion.App/PasteHttpServer.cs`
- Create: `companion/Mathwrite.Companion.App/InMemoryPasteExecutor.cs`
- Modify: `companion/Mathwrite.Companion.App/Program.cs`

- [ ] **Step 1: Define paste executor interface**

Add:

```csharp
public interface IPasteExecutor
{
    Task<PasteExecutionResult> PasteAsync(string text, CancellationToken cancellationToken);
}
```

`PasteExecutionResult` must include `Succeeded`, `ErrorCode`, and `Message`.

- [ ] **Step 2: Implement `/paste` endpoint**

Use `HttpListener` bound to `http://127.0.0.1:18765/`. On `POST /paste`, deserialize JSON into `PasteRequest`, validate it, reject duplicate sequence ids, format text, call `IPasteExecutor`, and return `PasteResponse` JSON.

- [ ] **Step 3: Add app fake mode**

When launched with `--fake-paste`, use `InMemoryPasteExecutor` and print the pasted text to diagnostics instead of touching the clipboard.

- [ ] **Step 4: Manual fake endpoint verification**

Run:

```powershell
dotnet run --project companion/Mathwrite.Companion.App/Mathwrite.Companion.App.csproj -- --fake-paste
```

In another terminal, post:

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:18765/paste -ContentType application/json -Body '{"sequenceId":1,"latex":"x^2","mode":"inline","source":"mathwrite-android"}'
```

Expected response:

```json
{"ok":true,"pasted":true,"sequenceId":1}
```

## Task 4: Windows Clipboard And SendInput Paste Executor

**Files:**
- Create: `companion/Mathwrite.Companion.App/WindowsPasteExecutor.cs`
- Modify: `companion/Mathwrite.Companion.App/Program.cs`

- [ ] **Step 1: Implement clipboard write**

Use `System.Windows.Forms.Clipboard.SetText(text, TextDataFormat.UnicodeText)` on an STA thread.

- [ ] **Step 2: Implement `Ctrl+V` injection**

Use P/Invoke to call `SendInput` with key down/up events for `VK_CONTROL` and `V`.

Required sequence:

```text
Ctrl down
V down
V up
Ctrl up
```

- [ ] **Step 3: Manual paste verification**

Open Notepad, place the cursor in the document, then run the companion normally and post:

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:18765/paste -ContentType application/json -Body '{"sequenceId":2,"latex":"x^2 + y^2 = z^2","mode":"display","source":"mathwrite-android"}'
```

Expected: Notepad receives `\[x^2 + y^2 = z^2\]`.

## Task 5: ADB Bridge Discovery And Reverse Mapping

**Files:**
- Create: `companion/Mathwrite.Companion.Core/AdbPathResolver.cs`
- Create: `companion/Mathwrite.Companion.App/AdbBridgeService.cs`
- Modify: `companion/Mathwrite.Companion.Tests/Program.cs`

- [ ] **Step 1: Test ADB path resolution**

Add tests that verify the resolver checks:

```text
PATH adb.exe
%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
%ANDROID_HOME%\platform-tools\adb.exe
%ANDROID_SDK_ROOT%\platform-tools\adb.exe
```

- [ ] **Step 2: Implement resolver**

Return the first existing `adb.exe` path. On this machine, the expected fallback path is:

```text
C:\Users\emili\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

- [ ] **Step 3: Implement reverse command**

Run:

```powershell
adb reverse tcp:18765 tcp:18765
```

through `ProcessStartInfo` using the resolved `adb.exe` path. Capture stdout, stderr, and exit code.

- [ ] **Step 4: Add companion startup bridge attempt**

On app startup, start the HTTP server, then attempt ADB reverse. If ADB is missing or no device is connected, keep the HTTP server running and show bridge status as degraded.

## Task 6: Android Project Skeleton

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/app/build.gradle.kts`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/java/com/mathwrite/app/MainActivity.kt`
- Create: `android/app/src/main/java/com/mathwrite/app/ui/MathwriteApp.kt`

- [ ] **Step 1: Create Gradle Android project**

Use package `com.mathwrite.app`, min SDK 26, target SDK 35, Compose enabled.

- [ ] **Step 2: Add permissions**

Add:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

- [ ] **Step 3: Add first Compose screen**

Create a screen with:

- Large drawing canvas.
- Preview text area.
- Send button.
- Clear button.
- Undo button.
- Paste mode segmented buttons.
- Status text.

- [ ] **Step 4: Build Android debug app**

Run:

```powershell
gradle -p android assembleDebug
```

Expected: debug APK builds.

## Task 7: Android Stroke Capture And Serialization

**Files:**
- Create: `android/app/src/main/java/com/mathwrite/app/ink/InkStroke.kt`
- Create: `android/app/src/main/java/com/mathwrite/app/ink/StrokeStore.kt`
- Create: `android/app/src/main/java/com/mathwrite/app/ink/StrokeCanvas.kt`
- Create: `android/app/src/test/java/com/mathwrite/app/ink/StrokeSerializationTest.kt`

- [ ] **Step 1: Add stroke model**

`InkStroke` stores ordered points with x and y floats. `StrokeStore` supports add point, finish stroke, undo, clear, and Mathpix serialization.

- [ ] **Step 2: Add canvas capture**

Use Compose pointer input. Start a stroke on down, append points on move, finish on up/cancel, and render all strokes on the canvas.

- [ ] **Step 3: Test JSON shape**

Expected Mathpix request body:

```json
{
  "strokes": {
    "strokes": {
      "x": [[10, 20]],
      "y": [[30, 40]]
    }
  }
}
```

## Task 8: Android Mathpix v3/strokes Client

**Files:**
- Create: `android/app/src/main/java/com/mathwrite/app/mathpix/MathpixClient.kt`
- Create: `android/app/src/main/java/com/mathwrite/app/mathpix/MathpixModels.kt`
- Create: `android/app/src/main/java/com/mathwrite/app/mathpix/LatexExtractor.kt`

- [ ] **Step 1: Implement request**

POST to:

```text
https://api.mathpix.com/v3/strokes
```

Headers:

```text
app_id: configured app id
app_key: configured app key
Content-Type: application/json
```

- [ ] **Step 2: Parse response**

Prefer `latex_styled`. If absent, search `data` for the first item with `type == "latex"`. If still absent, strip Mathpix inline delimiters from `text` when it starts with `\(` and ends with `\)`.

- [ ] **Step 3: Surface confidence**

Expose `latex`, `confidence`, `confidence_rate`, and a user-readable error.

## Task 9: Android Companion Bridge Client

**Files:**
- Create: `android/app/src/main/java/com/mathwrite/app/bridge/CompanionBridgeClient.kt`
- Create: `android/app/src/main/java/com/mathwrite/app/bridge/PasteRequest.kt`
- Create: `android/app/src/main/java/com/mathwrite/app/format/LatexPasteMode.kt`

- [ ] **Step 1: Implement paste POST**

POST to:

```text
http://127.0.0.1:18765/paste
```

with:

```json
{
  "sequenceId": 1,
  "latex": "x^2",
  "mode": "inline",
  "source": "mathwrite-android"
}
```

- [ ] **Step 2: Increment sequence id locally**

Use a monotonic in-memory counter for MVP. Persisting sequence ids can be added after basic end-to-end behavior works.

## Task 10: End-To-End Wiring

**Files:**
- Modify: `android/app/src/main/java/com/mathwrite/app/ui/MathwriteApp.kt`
- Modify: `android/app/src/main/java/com/mathwrite/app/MainActivity.kt`

- [ ] **Step 1: Wire Send button**

On Send:

1. Reject empty canvas.
2. Send strokes to Mathpix.
3. Show recognized LaTeX preview.
4. Send LaTeX to Windows companion.
5. Show paste success or failure.

- [ ] **Step 2: Add simple settings source**

Read Mathpix credentials from Android local properties during debug builds or from visible fields in the settings UI. Do not hardcode credentials in committed source files.

- [ ] **Step 3: Run final verification**

Run:

```powershell
dotnet build Mathwrite.sln
dotnet run --project companion/Mathwrite.Companion.Tests/Mathwrite.Companion.Tests.csproj
gradle -p android assembleDebug
```

Expected: companion builds, companion tests pass, Android debug APK builds.

Manual end-to-end expected result:

1. Start companion.
2. Plug in Samsung Tab S8+ with USB debugging enabled.
3. Confirm companion applies `adb reverse`.
4. Install and open Android app.
5. Write `x^2 + y^2 = z^2`.
6. Tap Send.
7. Focused Windows app receives the configured LaTeX format.
