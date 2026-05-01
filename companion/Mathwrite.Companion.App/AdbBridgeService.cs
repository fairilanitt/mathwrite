using System.Diagnostics;
using Mathwrite.Companion.Core;

namespace Mathwrite.Companion.App;

public sealed class AdbBridgeService
{
    private readonly Action<string> log;

    public AdbBridgeService(Action<string> log)
    {
        this.log = log;
    }

    public async Task<bool> ApplyReverseAsync(CancellationToken cancellationToken)
    {
        var adbPath = AdbPathResolver.ResolveCurrent();
        if (adbPath is null)
        {
            log("ADB not found. Install Android SDK Platform Tools or add adb.exe to PATH.");
            return false;
        }

        log("Using ADB: " + adbPath);
        var result = await RunAdbAsync(adbPath, "reverse tcp:18765 tcp:18765", cancellationToken).ConfigureAwait(false);

        if (result.ExitCode == 0)
        {
            log("ADB reverse active: device tcp:18765 -> laptop tcp:18765");
            return true;
        }

        var message = string.IsNullOrWhiteSpace(result.StandardError)
            ? result.StandardOutput
            : result.StandardError;
        log("ADB reverse failed: " + message.Trim());
        return false;
    }

    private static async Task<AdbCommandResult> RunAdbAsync(string adbPath, string arguments, CancellationToken cancellationToken)
    {
        using var process = new Process();
        process.StartInfo = new ProcessStartInfo
        {
            FileName = adbPath,
            Arguments = arguments,
            UseShellExecute = false,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            CreateNoWindow = true
        };

        process.Start();
        var stdoutTask = process.StandardOutput.ReadToEndAsync(cancellationToken);
        var stderrTask = process.StandardError.ReadToEndAsync(cancellationToken);
        await process.WaitForExitAsync(cancellationToken).ConfigureAwait(false);

        return new AdbCommandResult(
            process.ExitCode,
            await stdoutTask.ConfigureAwait(false),
            await stderrTask.ConfigureAwait(false));
    }

    private sealed record AdbCommandResult(int ExitCode, string StandardOutput, string StandardError);
}
