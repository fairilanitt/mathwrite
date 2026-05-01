package com.mathwrite.app.format

enum class LatexPasteMode(val label: String, val wireName: String) {
    Raw("Raw", "raw"),
    Inline("\\(...\\)", "inline"),
    DollarInline("$...$", "dollarInline"),
    Display("\\[...\\]", "display"),
}
