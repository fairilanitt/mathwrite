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

    public Task<PasteExecutionResult> PasteAsync(string text, CancellationToken cancellationToken)
    {
        LastText = text;
        log("Fake paste: " + text);
        return Task.FromResult(PasteExecutionResult.Success());
    }
}
