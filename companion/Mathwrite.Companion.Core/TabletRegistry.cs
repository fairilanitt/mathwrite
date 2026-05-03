namespace Mathwrite.Companion.Core;

public sealed class TabletRegistry
{
    private readonly object sync = new();
    private readonly Dictionary<string, TabletInfo> tablets = new(StringComparer.Ordinal);
    private string? selectedSessionId;

    public IReadOnlyList<TabletInfo> Tablets
    {
        get
        {
            lock (sync)
            {
                return tablets.Values
                    .OrderBy(tablet => tablet.DisplayName, StringComparer.OrdinalIgnoreCase)
                    .ToArray();
            }
        }
    }

    public string? SelectedSessionId
    {
        get
        {
            lock (sync)
            {
                return selectedSessionId;
            }
        }
    }

    public TabletInfo RegisterOrUpdate(string? sessionId, string? tabletName, string remoteAddress)
    {
        var normalizedSessionId = string.IsNullOrWhiteSpace(sessionId) ? "unknown" : sessionId.Trim();
        var normalizedName = string.IsNullOrWhiteSpace(tabletName) ? "Mathwrite Tablet" : tabletName.Trim();
        var tablet = new TabletInfo(normalizedSessionId, normalizedName, remoteAddress, DateTime.UtcNow);

        lock (sync)
        {
            tablets[normalizedSessionId] = tablet;
            selectedSessionId ??= normalizedSessionId;
        }

        return tablet;
    }

    public bool Select(string sessionId)
    {
        lock (sync)
        {
            if (!tablets.ContainsKey(sessionId))
            {
                return false;
            }

            selectedSessionId = sessionId;
            return true;
        }
    }

    public bool IsAllowed(string? sessionId)
    {
        lock (sync)
        {
            if (selectedSessionId is null)
            {
                return true;
            }

            return string.Equals(selectedSessionId, sessionId, StringComparison.Ordinal);
        }
    }
}
