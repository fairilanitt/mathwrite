namespace Mathwrite.Companion.Core;

public static class PasteRequestValidator
{
    public const string ExpectedSource = "mathwrite-android";

    public static PasteValidationResult Validate(PasteRequest request)
    {
        if (request.SequenceId < 1)
        {
            return PasteValidationResult.Invalid("invalid_sequence", "The sequence id must be positive.");
        }

        if (string.IsNullOrWhiteSpace(request.Latex))
        {
            return PasteValidationResult.Invalid("empty_latex", "The LaTeX payload is empty.");
        }

        if (!string.Equals(request.Source, ExpectedSource, StringComparison.Ordinal))
        {
            return PasteValidationResult.Invalid("invalid_source", "The request source is not trusted.");
        }

        return PasteValidationResult.Valid();
    }
}
