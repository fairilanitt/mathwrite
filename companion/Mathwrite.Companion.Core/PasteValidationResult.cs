namespace Mathwrite.Companion.Core;

public sealed record PasteValidationResult(bool IsValid, string? ErrorCode, string? Message)
{
    public static PasteValidationResult Valid() => new(true, null, null);

    public static PasteValidationResult Invalid(string errorCode, string message) => new(false, errorCode, message);
}
