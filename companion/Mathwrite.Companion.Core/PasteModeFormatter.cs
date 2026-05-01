namespace Mathwrite.Companion.Core;

public static class PasteModeFormatter
{
    public static string Format(string latex, PasteMode mode)
    {
        var trimmed = latex.Trim();

        return mode switch
        {
            PasteMode.Raw => trimmed,
            PasteMode.Inline => "\\(" + trimmed + "\\)",
            PasteMode.DollarInline => "$" + trimmed + "$",
            PasteMode.Display => "\\[" + trimmed + "\\]",
            _ => trimmed
        };
    }
}
