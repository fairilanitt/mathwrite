namespace Mathwrite.Companion.Core;

public sealed class PasteRequestHandler
{
    private readonly IPasteExecutor pasteExecutor;
    private readonly SequenceGuard sequenceGuard = new();

    public PasteRequestHandler(IPasteExecutor pasteExecutor)
    {
        this.pasteExecutor = pasteExecutor;
    }

    public async Task<PasteResponse> HandleAsync(PasteRequest request, CancellationToken cancellationToken)
    {
        var validation = PasteRequestValidator.Validate(request);
        if (!validation.IsValid)
        {
            return new PasteResponse(false, false, request.SequenceId, validation.ErrorCode, validation.Message);
        }

        if (!sequenceGuard.TryAccept(request.SessionId ?? "legacy", request.SequenceId))
        {
            return new PasteResponse(false, false, request.SequenceId, "duplicate_sequence", "This paste request has already been handled.");
        }

        var formattedText = PasteModeFormatter.Format(request.Latex, request.Mode);
        var pasteResult = await pasteExecutor.PasteAsync(formattedText, cancellationToken).ConfigureAwait(false);

        return pasteResult.Succeeded
            ? new PasteResponse(true, true, request.SequenceId)
            : new PasteResponse(false, false, request.SequenceId, pasteResult.ErrorCode, pasteResult.Message);
    }
}
