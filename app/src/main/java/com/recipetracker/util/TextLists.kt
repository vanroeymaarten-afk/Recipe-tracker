package com.recipetracker.util

/**
 * Helpers for ingredient and step lists.
 */
object TextLists {
    private val bullet = Regex("""^\s*([-•*·▪●◦–—]|\[\s?[xX ]?\s?\])\s*""")
    private val numbering = Regex("""^\s*(step\s*)?\d{1,2}\s*[.):\-]\s*""", RegexOption.IGNORE_CASE)

    /** Removes list bullets and "1." / "Step 2:" numbering that come along when pasting from a website. */
    fun clean(line: String): String = line.replace(bullet, "").replace(numbering, "").trim()

    /** Splits pasted text into list items, dropping empty lines. */
    fun splitPasted(text: String): List<String> =
        text.split('\n', '\r').map { clean(it) }.filter { it.isNotEmpty() }

    /** "For the sauce:" style lines are shown as small headings inside the ingredient list. */
    fun isSubheading(line: String): Boolean {
        val t = line.trim()
        return t.length in 2..60 && t.endsWith(":")
    }
}
