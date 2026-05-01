namespace Mathwrite.Companion.Core;

public interface IPasteExecutor
{
    Task<PasteExecutionResult> PasteAsync(string text, CancellationToken cancellationToken);
}
