namespace Mathwrite.Companion.Core;

public sealed class SketchPasteRequestHandler
{
    private readonly IPasteExecutor pasteExecutor;
    private readonly SequenceGuard sequenceGuard = new();

    public SketchPasteRequestHandler(IPasteExecutor pasteExecutor)
    {
        this.pasteExecutor = pasteExecutor;
    }

    public async Task<PasteResponse> HandleAsync(SketchPasteRequest request, CancellationToken cancellationToken)
    {
        var validation = SketchPasteRequestValidator.Validate(request);
        if (!validation.IsValid)
        {
            return new PasteResponse(false, false, request.SequenceId, validation.ErrorCode, validation.Message);
        }

        if (!sequenceGuard.TryAccept(request.SessionId ?? "legacy", request.SequenceId))
        {
            return new PasteResponse(false, false, request.SequenceId, "duplicate_sequence", "This sketch request has already been handled.");
        }

        var pngBytes = Convert.FromBase64String(request.PngBase64);
        var pasteResult = await pasteExecutor.PasteImageAsync(pngBytes, cancellationToken).ConfigureAwait(false);

        return pasteResult.Succeeded
            ? new PasteResponse(true, true, request.SequenceId)
            : new PasteResponse(false, false, request.SequenceId, pasteResult.ErrorCode, pasteResult.Message);
    }
}
