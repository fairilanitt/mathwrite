namespace Mathwrite.Companion.Core;

public static class SketchPasteRequestValidator
{
    public static PasteValidationResult Validate(SketchPasteRequest request)
    {
        if (request.SequenceId < 1)
        {
            return PasteValidationResult.Invalid("invalid_sequence", "The sequence id must be positive.");
        }

        if (string.IsNullOrWhiteSpace(request.SessionId))
        {
            return PasteValidationResult.Invalid("empty_session", "The tablet session id is empty.");
        }

        if (string.IsNullOrWhiteSpace(request.PngBase64))
        {
            return PasteValidationResult.Invalid("empty_image", "The sketch image payload is empty.");
        }

        if (!string.Equals(request.Source, PasteRequestValidator.ExpectedSource, StringComparison.Ordinal))
        {
            return PasteValidationResult.Invalid("invalid_source", "The request source is not trusted.");
        }

        try
        {
            _ = Convert.FromBase64String(request.PngBase64);
        }
        catch (FormatException)
        {
            return PasteValidationResult.Invalid("invalid_image", "The sketch image payload is not valid base64.");
        }

        return PasteValidationResult.Valid();
    }
}
