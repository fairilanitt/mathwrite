namespace Mathwrite.Companion.Core;

public sealed record PasteResponse(
    bool Ok,
    bool Pasted,
    long SequenceId,
    string? ErrorCode = null,
    string? Message = null);
