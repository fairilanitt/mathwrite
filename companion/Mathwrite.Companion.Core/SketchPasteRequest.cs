namespace Mathwrite.Companion.Core;

public sealed record SketchPasteRequest(
    long SequenceId,
    string? SessionId,
    string PngBase64,
    string Source,
    string? TabletName = null);
