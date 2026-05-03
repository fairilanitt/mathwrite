namespace Mathwrite.Companion.Core;

public interface IPasteExecutor
{
    Task<PasteExecutionResult> PasteTextAsync(string text, CancellationToken cancellationToken);

    Task<PasteExecutionResult> PasteImageAsync(byte[] pngBytes, CancellationToken cancellationToken);
}
