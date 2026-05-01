namespace Mathwrite.Companion.Core;

public sealed class SequenceGuard
{
    private readonly object gate = new();
    private readonly Dictionary<string, long> lastAcceptedBySession = new(StringComparer.Ordinal);

    public bool TryAccept(long sequenceId)
    {
        return TryAccept("legacy", sequenceId);
    }

    public bool TryAccept(string sessionId, long sequenceId)
    {
        lock (gate)
        {
            var normalizedSessionId = string.IsNullOrWhiteSpace(sessionId) ? "legacy" : sessionId;
            if (lastAcceptedBySession.TryGetValue(normalizedSessionId, out var lastAccepted) &&
                sequenceId <= lastAccepted)
            {
                return false;
            }

            lastAcceptedBySession[normalizedSessionId] = sequenceId;
            return true;
        }
    }
}
