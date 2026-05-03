using System.ComponentModel;
using System.Runtime.InteropServices;
using Mathwrite.Companion.Core;

namespace Mathwrite.Companion.App;

public sealed class WindowsPasteExecutor : IPasteExecutor
{
    private const uint InputMouse = 0;
    private const uint InputKeyboard = 1;
    private const uint KeyEventKeyUp = 0x0002;
    private const uint MouseEventLeftDown = 0x0002;
    private const uint MouseEventLeftUp = 0x0004;

    private readonly Control dispatcher;
    private readonly Action<string> log;

    public static int InputStructureSize => Marshal.SizeOf<Input>();

    public WindowsPasteExecutor(Control dispatcher, Action<string> log)
    {
        this.dispatcher = dispatcher;
        this.log = log;
    }

    public Task<PasteExecutionResult> PasteTextAsync(string text, CancellationToken cancellationToken)
    {
        if (dispatcher.IsDisposed)
        {
            return Task.FromResult(PasteExecutionResult.Failure("app_closed", "The companion window is closed."));
        }

        var completion = new TaskCompletionSource<PasteExecutionResult>(TaskCreationOptions.RunContinuationsAsynchronously);

        if (cancellationToken.CanBeCanceled)
        {
            cancellationToken.Register(() => completion.TrySetCanceled(cancellationToken));
        }

        dispatcher.BeginInvoke(() =>
        {
            try
            {
                Clipboard.SetText(text, TextDataFormat.UnicodeText);
                var result = RunWorkflow(MathExercisePasteWorkflow.TextSteps);

                if (!result.Succeeded)
                {
                    completion.TrySetResult(PasteExecutionResult.Failure("send_input_failed", result.Message));
                    return;
                }

                log("Pasted: " + text);
                completion.TrySetResult(PasteExecutionResult.Success());
            }
            catch (ExternalException exception)
            {
                completion.TrySetResult(PasteExecutionResult.Failure("clipboard_busy", exception.Message));
            }
            catch (Exception exception)
            {
                completion.TrySetResult(PasteExecutionResult.Failure("paste_failed", exception.Message));
            }
        });

        return completion.Task;
    }

    public Task<PasteExecutionResult> PasteImageAsync(byte[] pngBytes, CancellationToken cancellationToken)
    {
        if (dispatcher.IsDisposed)
        {
            return Task.FromResult(PasteExecutionResult.Failure("app_closed", "The companion window is closed."));
        }

        var completion = new TaskCompletionSource<PasteExecutionResult>(TaskCreationOptions.RunContinuationsAsynchronously);

        if (cancellationToken.CanBeCanceled)
        {
            cancellationToken.Register(() => completion.TrySetCanceled(cancellationToken));
        }

        dispatcher.BeginInvoke(() =>
        {
            try
            {
                using var stream = new MemoryStream(pngBytes);
                using var image = Image.FromStream(stream);
                using var bitmap = new Bitmap(image);
                Clipboard.SetImage(bitmap);
                var result = RunWorkflow(MathExercisePasteWorkflow.ImageSteps);

                if (!result.Succeeded)
                {
                    completion.TrySetResult(PasteExecutionResult.Failure("send_input_failed", result.Message));
                    return;
                }

                log($"Pasted sketch image ({pngBytes.Length} bytes)");
                completion.TrySetResult(PasteExecutionResult.Success());
            }
            catch (ExternalException exception)
            {
                completion.TrySetResult(PasteExecutionResult.Failure("clipboard_busy", exception.Message));
            }
            catch (Exception exception)
            {
                completion.TrySetResult(PasteExecutionResult.Failure("image_paste_failed", exception.Message));
            }
        });

        return completion.Task;
    }

    private static SendInputResult RunWorkflow(IReadOnlyList<PasteAutomationStep> steps)
    {
        foreach (var step in steps)
        {
            var result = SendStep(step);
            if (!result.Succeeded)
            {
                return result;
            }

            if (step.DelayAfterMilliseconds > 0)
            {
                Thread.Sleep(step.DelayAfterMilliseconds);
            }
        }

        return SendInputResult.Success();
    }

    private static SendInputResult SendStep(PasteAutomationStep step)
    {
        var inputs = step.Kind switch
        {
            PasteAutomationKind.KeyboardShortcut => KeyboardShortcut(step.ModifierKey!.Value, step.Key!.Value),
            PasteAutomationKind.KeyPress => KeyPress(step.Key!.Value),
            PasteAutomationKind.MouseClick => LeftClick(),
            _ => Array.Empty<Input>()
        };

        var sent = SendInput((uint)inputs.Length, inputs, InputStructureSize);
        if (sent == inputs.Length)
        {
            return SendInputResult.Success();
        }

        var error = Marshal.GetLastWin32Error();
        return SendInputResult.Failure(
            $"Windows did not accept step '{step.Name}'. SendInput sent {sent}/{inputs.Length}; Win32 error {error}; INPUT size {InputStructureSize}.");
    }

    private static Input[] KeyboardShortcut(VirtualKey modifierKey, VirtualKey key)
    {
        return
        [
            KeyboardInput((ushort)modifierKey, 0),
            KeyboardInput((ushort)key, 0),
            KeyboardInput((ushort)key, KeyEventKeyUp),
            KeyboardInput((ushort)modifierKey, KeyEventKeyUp)
        ];
    }

    private static Input[] KeyPress(VirtualKey key)
    {
        return
        [
            KeyboardInput((ushort)key, 0),
            KeyboardInput((ushort)key, KeyEventKeyUp)
        ];
    }

    private static Input[] LeftClick()
    {
        return
        [
            MouseInput(MouseEventLeftDown),
            MouseInput(MouseEventLeftUp)
        ];
    }

    private static Input KeyboardInput(ushort key, uint flags)
    {
        return new Input
        {
            Type = InputKeyboard,
            Data = new InputUnion
            {
                Keyboard = new KeyboardInputData
                {
                    VirtualKey = key,
                    ScanCode = 0,
                    Flags = flags,
                    Time = 0,
                    ExtraInfo = IntPtr.Zero
                }
            }
        };
    }

    private static Input MouseInput(uint flags)
    {
        return new Input
        {
            Type = InputMouse,
            Data = new InputUnion
            {
                Mouse = new MouseInputData
                {
                    Dx = 0,
                    Dy = 0,
                    MouseData = 0,
                    Flags = flags,
                    Time = 0,
                    ExtraInfo = IntPtr.Zero
                }
            }
        };
    }

    [DllImport("user32.dll", SetLastError = true)]
    private static extern uint SendInput(uint inputCount, Input[] inputs, int inputSize);

    [StructLayout(LayoutKind.Sequential)]
    private struct Input
    {
        public uint Type;
        public InputUnion Data;
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct InputUnion
    {
        [FieldOffset(0)]
        public MouseInputData Mouse;

        [FieldOffset(0)]
        public KeyboardInputData Keyboard;

        [FieldOffset(0)]
        public HardwareInputData Hardware;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MouseInputData
    {
        public int Dx;
        public int Dy;
        public uint MouseData;
        public uint Flags;
        public uint Time;
        public IntPtr ExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct KeyboardInputData
    {
        public ushort VirtualKey;
        public ushort ScanCode;
        public uint Flags;
        public uint Time;
        public IntPtr ExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct HardwareInputData
    {
        public uint Message;
        public ushort ParamL;
        public ushort ParamH;
    }

    private sealed record SendInputResult(bool Succeeded, string Message)
    {
        public static SendInputResult Success() => new(true, string.Empty);

        public static SendInputResult Failure(string message) => new(false, message);
    }
}
