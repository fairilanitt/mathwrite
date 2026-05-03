namespace Mathwrite.Companion.Core;

public sealed record TabletInfo(
    string SessionId,
    string DisplayName,
    string RemoteAddress,
    DateTime LastSeenUtc);
