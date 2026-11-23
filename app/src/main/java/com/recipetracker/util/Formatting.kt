package com.recipetracker.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatting {
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val full = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val long = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.getDefault())
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun friendlyDate(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> if (date.year == today.year) date.format(dayMonth) else date.format(full)
    }

    fun fullDate(date: LocalDate): String = date.format(full)

    fun longDate(date: LocalDate): String = date.format(long)

    fun monthYear(date: LocalDate): String = date.format(monthYear)

    fun minutes(total: Int): String {
        val h = total / 60
        val m = total % 60
        return when {
            h == 0 -> "$m min"
            m == 0 -> "$h h"
            else -> "$h h $m min"
        }
    }

    fun times(count: Int, singular: String, plural: String = singular + "s"): String =
        "$count ${if (count == 1) singular else plural}"

    /** A file-system friendly version of a title, for exported file names. */
    fun fileSlug(text: String): String =
        text.trim().replace(Regex("""[^\p{L}\p{N}]+"""), "-").trim('-').take(60).ifEmpty { "recipes" }
}
