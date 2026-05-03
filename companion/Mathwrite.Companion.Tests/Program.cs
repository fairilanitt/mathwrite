using Mathwrite.Companion.App;
using Mathwrite.Companion.Core;

AssertEqual("\\frac{x}{2}", PasteModeFormatter.Format("  \\frac{x}{2}  ", PasteMode.Raw), "raw mode trims LaTeX");
AssertEqual("\\(\\frac{x}{2}\\)", PasteModeFormatter.Format("\\frac{x}{2}", PasteMode.Inline), "inline mode wraps with parentheses delimiters");
AssertEqual("$\\frac{x}{2}$", PasteModeFormatter.Format("\\frac{x}{2}", PasteMode.DollarInline), "dollar inline mode wraps with dollar delimiters");
AssertEqual("\\[\\frac{x}{2}\\]", PasteModeFormatter.Format("\\frac{x}{2}", PasteMode.Display), "display mode wraps with display delimiters");

AssertTrue(PasteRequestValidator.Validate(new PasteRequest(1, "x^2", PasteMode.Raw, "mathwrite-android")).IsValid, "valid paste request is accepted");
AssertFalse(PasteRequestValidator.Validate(new PasteRequest(0, "x^2", PasteMode.Raw, "mathwrite-android")).IsValid, "sequence id must be positive");
AssertFalse(PasteRequestValidator.Validate(new PasteRequest(1, "", PasteMode.Raw, "mathwrite-android")).IsValid, "latex must be non-empty");
AssertFalse(PasteRequestValidator.Validate(new PasteRequest(1, "x^2", PasteMode.Raw, "unknown")).IsValid, "source must match Android app");

var sketchBytes = new byte[] { 137, 80, 78, 71, 13, 10, 26, 10, 1, 2, 3, 4 };
var sketchBase64 = Convert.ToBase64String(sketchBytes);
AssertTrue(SketchPasteRequestValidator.Validate(new SketchPasteRequest(1, "session-a", sketchBase64, "mathwrite-android", "Galaxy Tab S8+")).IsValid, "valid sketch request is accepted");
AssertFalse(SketchPasteRequestValidator.Validate(new SketchPasteRequest(0, "session-a", sketchBase64, "mathwrite-android", "Galaxy Tab S8+")).IsValid, "sketch sequence id must be positive");
AssertFalse(SketchPasteRequestValidator.Validate(new SketchPasteRequest(1, "", sketchBase64, "mathwrite-android", "Galaxy Tab S8+")).IsValid, "sketch session id must be present");
AssertFalse(SketchPasteRequestValidator.Validate(new SketchPasteRequest(1, "session-a", "", "mathwrite-android", "Galaxy Tab S8+")).IsValid, "sketch image payload must be present");
AssertFalse(SketchPasteRequestValidator.Validate(new SketchPasteRequest(1, "session-a", "not-base64", "mathwrite-android", "Galaxy Tab S8+")).IsValid, "sketch image payload must be base64");
AssertFalse(SketchPasteRequestValidator.Validate(new SketchPasteRequest(1, "session-a", sketchBase64, "unknown", "Galaxy Tab S8+")).IsValid, "sketch source must match Android app");

var tabletRegistry = new TabletRegistry();
var firstTablet = tabletRegistry.RegisterOrUpdate("session-a", "Galaxy Tab S8+", "192.168.1.50");
var secondTablet = tabletRegistry.RegisterOrUpdate("session-b", "Other Tablet", "192.168.1.51");
tabletRegistry.Select(firstTablet.SessionId);
AssertTrue(tabletRegistry.IsAllowed(firstTablet.SessionId), "selected tablet is allowed");
AssertFalse(tabletRegistry.IsAllowed(secondTablet.SessionId), "unselected tablet is rejected when a tablet is selected");
AssertSequenceEqual(new[] { "Galaxy Tab S8+", "Other Tablet" }, tabletRegistry.Tablets.Select(tablet => tablet.DisplayName).ToArray(), "tablet registry keeps discovered tablets");

var guard = new SequenceGuard();
AssertTrue(guard.TryAccept(1), "first sequence id is accepted");
AssertFalse(guard.TryAccept(1), "duplicate sequence id is rejected");
AssertFalse(guard.TryAccept(0), "older sequence id is rejected");
AssertTrue(guard.TryAccept(2), "larger sequence id is accepted");

var executor = new CapturingPasteExecutor();
var handler = new PasteRequestHandler(executor);
var pasteResponse = handler.HandleAsync(new PasteRequest(3, "x^2", PasteMode.Inline, "mathwrite-android"), CancellationToken.None).GetAwaiter().GetResult();
AssertTrue(pasteResponse.Ok, "handler accepts valid request");
AssertTrue(pasteResponse.Pasted, "handler reports pasted valid request");
AssertEqual("\\(x^2\\)", executor.LastText, "handler formats text before paste");

var duplicateResponse = handler.HandleAsync(new PasteRequest(3, "y^2", PasteMode.Raw, "mathwrite-android"), CancellationToken.None).GetAwaiter().GetResult();
AssertFalse(duplicateResponse.Ok, "handler rejects duplicate request");
AssertEqual("duplicate_sequence", duplicateResponse.ErrorCode, "handler reports duplicate sequence");

var sessionExecutor = new CapturingPasteExecutor();
var sessionHandler = new PasteRequestHandler(sessionExecutor);
AssertTrue(
    sessionHandler.HandleAsync(new PasteRequest(10, "x", PasteMode.Raw, "mathwrite-android", "session-a"), CancellationToken.None).GetAwaiter().GetResult().Ok,
    "handler accepts first request in first session");
AssertFalse(
    sessionHandler.HandleAsync(new PasteRequest(10, "x", PasteMode.Raw, "mathwrite-android", "session-a"), CancellationToken.None).GetAwaiter().GetResult().Ok,
    "handler rejects duplicate sequence inside the same session");
AssertTrue(
    sessionHandler.HandleAsync(new PasteRequest(1, "y", PasteMode.Raw, "mathwrite-android", "session-b"), CancellationToken.None).GetAwaiter().GetResult().Ok,
    "handler accepts low sequence id in a new session");

var sketchExecutor = new CapturingPasteExecutor();
var sketchHandler = new SketchPasteRequestHandler(sketchExecutor);
var sketchResponse = sketchHandler.HandleAsync(new SketchPasteRequest(7, "session-a", sketchBase64, "mathwrite-android", "Galaxy Tab S8+"), CancellationToken.None).GetAwaiter().GetResult();
AssertTrue(sketchResponse.Ok, "sketch handler accepts valid request");
AssertTrue(sketchResponse.Pasted, "sketch handler reports pasted valid request");
AssertSequenceEqual(sketchBytes, sketchExecutor.LastImage ?? Array.Empty<byte>(), "sketch handler decodes png payload before paste");

var expectedInputSize = IntPtr.Size == 8 ? 40 : 28;
AssertEqual(expectedInputSize, WindowsPasteExecutor.InputStructureSize, "SendInput receives the full Win32 INPUT structure size");

var workflowSteps = MathExercisePasteWorkflow.Steps.Select(step => step.Name).ToArray();
AssertSequenceEqual(
    new[]
    {
        "Ctrl+V",
        "Enter"
    },
    workflowSteps,
    "math exercise website workflow pastes into the already-open LaTeX prompt, then starts the next row");

Console.WriteLine("All companion core tests passed");

static void AssertEqual<T>(T expected, T actual, string name)
{
    if (!EqualityComparer<T>.Default.Equals(expected, actual))
    {
        throw new InvalidOperationException($"{name}: expected '{expected}', got '{actual}'");
    }
}

static void AssertTrue(bool condition, string name)
{
    if (!condition)
    {
        throw new InvalidOperationException($"{name}: expected true");
    }
}

static void AssertFalse(bool condition, string name)
{
    if (condition)
    {
        throw new InvalidOperationException($"{name}: expected false");
    }
}

static void AssertSequenceEqual<T>(IReadOnlyList<T> expected, IReadOnlyList<T> actual, string name)
{
    if (expected.Count != actual.Count)
    {
        throw new InvalidOperationException($"{name}: expected {expected.Count} items, got {actual.Count}");
    }

    for (var index = 0; index < expected.Count; index++)
    {
        if (!EqualityComparer<T>.Default.Equals(expected[index], actual[index]))
        {
            throw new InvalidOperationException($"{name}: item {index} expected '{expected[index]}', got '{actual[index]}'");
        }
    }
}

sealed class CapturingPasteExecutor : IPasteExecutor
{
    public string? LastText { get; private set; }
    public byte[]? LastImage { get; private set; }

    public Task<PasteExecutionResult> PasteTextAsync(string text, CancellationToken cancellationToken)
    {
        LastText = text;
        return Task.FromResult(PasteExecutionResult.Success());
    }

    public Task<PasteExecutionResult> PasteImageAsync(byte[] pngBytes, CancellationToken cancellationToken)
    {
        LastImage = pngBytes;
        return Task.FromResult(PasteExecutionResult.Success());
    }
}
