namespace Mathwrite.Companion.App;

public static class MathExercisePasteWorkflow
{
    public static IReadOnlyList<PasteAutomationStep> TextSteps { get; } =
    [
        PasteAutomationStep.KeyboardShortcut("Ctrl+V", VirtualKey.Control, VirtualKey.V, 120),
        PasteAutomationStep.KeyPress("Enter", VirtualKey.Enter, 0)
    ];

    public static IReadOnlyList<PasteAutomationStep> ImageSteps { get; } =
    [
        PasteAutomationStep.KeyboardShortcut("Ctrl+V", VirtualKey.Control, VirtualKey.V, 0)
    ];

    public static IReadOnlyList<PasteAutomationStep> Steps => TextSteps;
}

public sealed record PasteAutomationStep(
    string Name,
    PasteAutomationKind Kind,
    VirtualKey? ModifierKey,
    VirtualKey? Key,
    int DelayAfterMilliseconds)
{
    public static PasteAutomationStep KeyboardShortcut(
        string name,
        VirtualKey modifierKey,
        VirtualKey key,
        int delayAfterMilliseconds)
    {
        return new PasteAutomationStep(name, PasteAutomationKind.KeyboardShortcut, modifierKey, key, delayAfterMilliseconds);
    }

    public static PasteAutomationStep KeyPress(
        string name,
        VirtualKey key,
        int delayAfterMilliseconds)
    {
        return new PasteAutomationStep(name, PasteAutomationKind.KeyPress, null, key, delayAfterMilliseconds);
    }

    public static PasteAutomationStep MouseClick(string name, int delayAfterMilliseconds)
    {
        return new PasteAutomationStep(name, PasteAutomationKind.MouseClick, null, null, delayAfterMilliseconds);
    }
}

public enum PasteAutomationKind
{
    KeyboardShortcut,
    KeyPress,
    MouseClick
}

public enum VirtualKey : ushort
{
    Control = 0x11,
    E = 0x45,
    V = 0x56,
    Enter = 0x0D
}
