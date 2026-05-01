namespace Mathwrite.Companion.Core;

public static class AdbPathResolver
{
    public static string? ResolveCurrent()
    {
        var pathEntries = (Environment.GetEnvironmentVariable("PATH") ?? string.Empty)
            .Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);

        var env = new Dictionary<string, string?>(StringComparer.OrdinalIgnoreCase)
        {
            ["LOCALAPPDATA"] = Environment.GetEnvironmentVariable("LOCALAPPDATA"),
            ["ANDROID_HOME"] = Environment.GetEnvironmentVariable("ANDROID_HOME"),
            ["ANDROID_SDK_ROOT"] = Environment.GetEnvironmentVariable("ANDROID_SDK_ROOT")
        };

        return Resolve(pathEntries, env, File.Exists);
    }

    public static string? Resolve(
        IEnumerable<string> pathEntries,
        IReadOnlyDictionary<string, string?> environment,
        Func<string, bool> fileExists)
    {
        foreach (var entry in pathEntries)
        {
            var candidate = entry.EndsWith("adb.exe", StringComparison.OrdinalIgnoreCase)
                ? entry
                : Path.Combine(entry, "adb.exe");

            if (fileExists(candidate))
            {
                return candidate;
            }
        }

        foreach (var candidate in FallbackCandidates(environment))
        {
            if (fileExists(candidate))
            {
                return candidate;
            }
        }

        return null;
    }

    private static IEnumerable<string> FallbackCandidates(IReadOnlyDictionary<string, string?> environment)
    {
        if (environment.TryGetValue("LOCALAPPDATA", out var localAppData) && !string.IsNullOrWhiteSpace(localAppData))
        {
            yield return Path.Combine(localAppData, "Android", "Sdk", "platform-tools", "adb.exe");
        }

        if (environment.TryGetValue("ANDROID_HOME", out var androidHome) && !string.IsNullOrWhiteSpace(androidHome))
        {
            yield return Path.Combine(androidHome, "platform-tools", "adb.exe");
        }

        if (environment.TryGetValue("ANDROID_SDK_ROOT", out var androidSdkRoot) && !string.IsNullOrWhiteSpace(androidSdkRoot))
        {
            yield return Path.Combine(androidSdkRoot, "platform-tools", "adb.exe");
        }
    }
}
