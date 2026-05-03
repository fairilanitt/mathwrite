using Mathwrite.Companion.Core;

namespace Mathwrite.Companion.App;

public sealed class InMemoryPasteExecutor : IPasteExecutor
{
    private readonly Action<string> log;

    public InMemoryPasteExecutor(Action<string> log)
    {
        this.log = log;
    }

    public string? LastText { get; private set; }
    public byte[]? LastImage { get; private set; }

    public Task<PasteExecutionResult> PasteTextAsync(string text, CancellationToken cancellationToken)
    {
        LastText = text;
        log("Fake paste: " + text);
        return Task.FromResult(PasteExecutionResult.Success());
    }

    public Task<PasteExecutionResult> PasteImageAsync(byte[] pngBytes, CancellationToken cancellationToken)
    {
        LastImage = pngBytes;
        log($"Fake image paste: {pngBytes.Length} bytes");
        return Task.FromResult(PasteExecutionResult.Success());
    }
}
