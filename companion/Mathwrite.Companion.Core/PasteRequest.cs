namespace Mathwrite.Companion.Core;

public sealed record PasteRequest(
    long SequenceId,
    string Latex,
    PasteMode Mode,
    string Source,
    string? SessionId = null);
