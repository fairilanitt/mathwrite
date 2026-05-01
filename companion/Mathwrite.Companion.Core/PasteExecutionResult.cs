namespace Mathwrite.Companion.Core;

public sealed record PasteExecutionResult(bool Succeeded, string? ErrorCode, string? Message)
{
    public static PasteExecutionResult Success() => new(true, null, null);

    public static PasteExecutionResult Failure(string errorCode, string message) => new(false, errorCode, message);
}
