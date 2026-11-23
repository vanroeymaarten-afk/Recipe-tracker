package com.recipetracker.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.recipetracker.data.PhotoStorage
import com.recipetracker.model.Iteration
import com.recipetracker.model.Photo
import com.recipetracker.model.Recipe
import com.recipetracker.model.Scores
import com.recipetracker.pdf.PageWriter.Companion.PAGE_HEIGHT
import com.recipetracker.pdf.PageWriter.Companion.PAGE_WIDTH
import com.recipetracker.util.Formatting
import com.recipetracker.util.TextLists
import java.io.File
import java.time.LocalDate
import kotlin.math.max

data class PdfOptions(
    val title: String = "Our Recipes",
    val includeCover: Boolean = true,
    val includePhotos: Boolean = true,
    val includeRatings: Boolean = true,
    val includeNotes: Boolean = true,
    val includeHistory: Boolean = true,
)

/**
 * Lays out a cookbook for [recipes]: an optional cover and contents page, then every
 * recipe starting on a new page with its newest iteration in full, and optionally
 * ratings, extra photos and the history of earlier iterations.
 */
class RecipeBookRenderer(
    private val recipes: List<Recipe>,
    private val options: PdfOptions,
    private val today: LocalDate = LocalDate.now(),
    private val loadBitmap: (File, Int) -> Bitmap? = { file, maxSide -> PhotoStorage.decodeScaled(file, maxSide) },
) {
    private val kicker = PdfStyle.paint(7.8f, PdfStyle.ACCENT, PdfStyle.sansBold, 0.16f)
    private val body = PdfStyle.paint(10.2f, PdfStyle.INK, PdfStyle.sans)
    private val bodySmall = PdfStyle.paint(9.2f, PdfStyle.INK, PdfStyle.sans)
    private val muted = PdfStyle.paint(9f, PdfStyle.MUTED, PdfStyle.sans)

    /** Renders into [sink] and returns the number of pages. */
    fun render(sink: PageSink): Int {
        // First pass only measures, to learn on which page each recipe starts (for the contents).
        val measured = pass(MeasuringSink(), null)
        return pass(sink, measured.startPages).pageCount
    }

    private class PassResult(val startPages: Map<String, Int>, val pageCount: Int)

    private fun pass(sink: PageSink, startPages: Map<String, Int>?): PassResult {
        val w = PageWriter(sink)
        val starts = mutableMapOf<String, Int>()
        if (options.includeCover) {
            cover(w)
            if (recipes.size > 1) contents(w, startPages)
        }
        for (recipe in recipes) {
            w.newPage(footer = recipe.title)
            starts[recipe.id] = w.page
            recipePages(w, recipe)
        }
        w.finishPage()
        return PassResult(starts, w.page)
    }

    // ---------------------------------------------------------------- cover & contents

    private fun cover(w: PageWriter) {
        w.newPage(footer = null)
        val c = w.canvas
        c?.drawRect(0f, 0f, PAGE_WIDTH, PAGE_HEIGHT, PdfStyle.fill(PdfStyle.COVER_BG))
        c?.drawRect(0f, 0f, PAGE_WIDTH, 10f, PdfStyle.fill(PdfStyle.ACCENT))
        val sideMargin = 70f
        val textWidth = PAGE_WIDTH - 2 * sideMargin
        var y = 96f

        val label = textLayout(
            "RECIPE COLLECTION", PdfStyle.paint(9f, PdfStyle.ACCENT, PdfStyle.sansBold, 0.32f),
            textWidth, Layout.Alignment.ALIGN_CENTER,
        )
        c?.drawLayoutAt(label, sideMargin, y)
        y += label.height + 18

        val title = textLayout(
            options.title, PdfStyle.paint(40f, PdfStyle.INK, PdfStyle.serifBold), textWidth,
            Layout.Alignment.ALIGN_CENTER, lineSpacing = 1.05f, maxLines = 3,
        )
        c?.drawLayoutAt(title, sideMargin, y)
        y += title.height + 14

        val subtitle = textLayout(
            "${Formatting.times(recipes.size, "recipe")}  ·  ${Formatting.monthYear(today)}",
            PdfStyle.paint(13f, PdfStyle.MUTED, PdfStyle.serifItalic), textWidth, Layout.Alignment.ALIGN_CENTER,
        )
        c?.drawLayoutAt(subtitle, sideMargin, y)
        y += subtitle.height + 26

        c?.let { ornament(it, PAGE_WIDTH / 2, y) }
        y += 30

        val areaBottom = PAGE_HEIGHT - 110f
        val area = RectF(sideMargin, y, PAGE_WIDTH - sideMargin, minOf(areaBottom, y + textWidth * 0.95f))
        val files = recipes.mapNotNull { it.coverPhoto?.file }.filter { it.exists() }.take(4)
        if (c != null) {
            if (files.isEmpty()) coverNames(c, area) else collage(c, area, files)
        }

        val made = textLayout(
            "Made with Recipe Tracker", PdfStyle.paint(8.5f, PdfStyle.MUTED, PdfStyle.sans, 0.08f),
            textWidth, Layout.Alignment.ALIGN_CENTER,
        )
        c?.drawLayoutAt(made, sideMargin, PAGE_HEIGHT - 64f)
    }

    private fun ornament(c: Canvas, cx: Float, cy: Float) {
        val line = PdfStyle.stroke(PdfStyle.ACCENT, 1f)
        c.drawLine(cx - 60f, cy, cx - 12f, cy, line)
        c.drawLine(cx + 12f, cy, cx + 60f, cy, line)
        val diamond = Path().apply {
            moveTo(cx, cy - 5f); lineTo(cx + 5f, cy); lineTo(cx, cy + 5f); lineTo(cx - 5f, cy); close()
        }
        c.drawPath(diamond, PdfStyle.fill(PdfStyle.ACCENT))
    }

    private fun collage(c: Canvas, area: RectF, files: List<File>) {
        val gap = 10f
        val r = 10f
        fun img(file: File, rect: RectF) = c.drawCropped(loadBitmap(file, 1000), rect, r)
        val l = area.left
        val t = area.top
        val rt = area.right
        val b = area.bottom
        val midX = (l + rt) / 2
        val midY = (t + b) / 2
        when (files.size) {
            1 -> img(files[0], area)
            2 -> {
                img(files[0], RectF(l, t, midX - gap / 2, b))
                img(files[1], RectF(midX + gap / 2, t, rt, b))
            }
            3 -> {
                val split = l + area.width() * 0.6f
                img(files[0], RectF(l, t, split - gap / 2, b))
                img(files[1], RectF(split + gap / 2, t, rt, midY - gap / 2))
                img(files[2], RectF(split + gap / 2, midY + gap / 2, rt, b))
            }
            else -> {
                img(files[0], RectF(l, t, midX - gap / 2, midY - gap / 2))
                img(files[1], RectF(midX + gap / 2, t, rt, midY - gap / 2))
                img(files[2], RectF(l, midY + gap / 2, midX - gap / 2, b))
                img(files[3], RectF(midX + gap / 2, midY + gap / 2, rt, b))
            }
        }
    }

    /** Cover without photos: the recipe names, set like a menu. */
    private fun coverNames(c: Canvas, area: RectF) {
        val names = recipes.take(14).joinToString("\n") { it.title } + if (recipes.size > 14) "\n…" else ""
        val layout = textLayout(
            names, PdfStyle.paint(15f, PdfStyle.INK, PdfStyle.serifItalic), area.width(),
            Layout.Alignment.ALIGN_CENTER, lineSpacing = 1.55f,
        )
        c.drawLayoutAt(layout, area.left, area.top + max(0f, (area.height() - layout.height) / 2))
    }

    private fun contents(w: PageWriter, startPages: Map<String, Int>?) {
        w.newPage(footer = options.title)
        val c = w.canvas
        val heading = textLayout("Contents", PdfStyle.paint(28f, PdfStyle.INK, PdfStyle.serifBold), w.width)
        c?.drawLayoutAt(heading, w.left, w.y)
        w.y += heading.height + 6
        c?.drawRect(w.left, w.y, w.left + 32f, w.y + 2.5f, PdfStyle.fill(PdfStyle.ACCENT))
        w.y += 26

        val numberPaint = PdfStyle.paint(10f, PdfStyle.ACCENT, PdfStyle.sansBold)
        val titlePaint = PdfStyle.paint(13.5f, PdfStyle.INK, PdfStyle.serif)
        val pagePaint = PdfStyle.paint(13f, PdfStyle.INK, PdfStyle.serif)
        val dot = PdfStyle.fill(PdfStyle.LINE)
        for ((index, recipe) in recipes.withIndex()) {
            val titleX = w.left + 34f
            val pageColumn = 44f
            val titleLayout = textLayout(recipe.title, titlePaint, w.width - 34f - pageColumn - 40f, maxLines = 2)
            val details = buildList {
                if (recipe.category.isNotBlank()) add(recipe.category)
                if (recipe.iterations.isNotEmpty()) add(Formatting.times(recipe.iterations.size, "iteration"))
                recipe.lastCookedOn?.let { add("last cooked ${Formatting.fullDate(it)}") }
            }.joinToString("  ·  ")
            val detailLayout = textLayout(details, muted, w.width - 34f - pageColumn - 110f, maxLines = 1)
            val rowHeight = titleLayout.height + 4f + detailLayout.height + 16f
            w.ensure(rowHeight)
            w.canvas?.let { canvas ->
                val y = w.y
                canvas.drawText(String.format("%02d", index + 1), w.left, y + titlePaint.textSize, numberPaint)
                canvas.drawLayoutAt(titleLayout, titleX, y)
                canvas.drawLayoutAt(detailLayout, titleX, y + titleLayout.height + 4)
                val pageText = startPages?.get(recipe.id)?.toString() ?: ""
                val pageWidth = pagePaint.measureText(pageText)
                val baseline = y + titleLayout.getLineBaseline(titleLayout.lineCount - 1)
                canvas.drawText(pageText, w.right - pageWidth, baseline, pagePaint)
                // dotted leader from the end of the title to the page number
                val lastLineEnd = titleX + titleLayout.getLineWidth(titleLayout.lineCount - 1) + 8f
                var x = lastLineEnd
                while (x < w.right - pageWidth - 8f) {
                    canvas.drawCircle(x, baseline - 3f, 0.9f, dot)
                    x += 5f
                }
                if (options.includeRatings) {
                    recipe.score?.let { score ->
                        canvas.drawStars(w.right - pageColumn - 62f, y + titleLayout.height + 4 + detailLayout.height / 2, 9f, score, 2f)
                    }
                }
                canvas.drawLine(w.left, y + rowHeight - 7, w.right, y + rowHeight - 7, PdfStyle.stroke(PdfStyle.LINE, 0.5f))
            }
            w.y += rowHeight
        }
    }

    // ---------------------------------------------------------------- recipe

    private fun recipePages(w: PageWriter, recipe: Recipe) {
        val current = recipe.latest
        val heroPhoto: Photo? = if (options.includePhotos) recipe.coverPhoto?.takeIf { it.file.exists() } else null
        if (heroPhoto != null) {
            val h = 250f
            w.canvas?.drawCropped(loadBitmap(heroPhoto.file, 1100), RectF(w.left, w.y, w.right, w.y + h), 12f)
            w.y += h + 22
        }

        val kickerText = buildList {
            if (recipe.category.isNotBlank()) add(recipe.category.uppercase())
            if (current != null) add("ITERATION ${current.number} OF ${recipe.iterations.size}")
            if (current != null) add(Formatting.fullDate(current.cookedOn).uppercase())
        }.joinToString("   ·   ")
        if (kickerText.isNotEmpty()) {
            w.flow(textLayout(kickerText, kicker, w.width))
            w.y += 7
        }
        w.flow(textLayout(recipe.title, PdfStyle.paint(27f, PdfStyle.INK, PdfStyle.serifBold), w.width, lineSpacing = 1.08f))
        w.y += 6
        if (recipe.description.isNotBlank()) {
            w.flow(textLayout(recipe.description.trim(), PdfStyle.paint(11.5f, PdfStyle.MUTED, PdfStyle.serifItalic), w.width))
            w.y += 8
        }

        val meta = buildList {
            recipe.servings?.let { add("Serves $it") }
            recipe.prepMinutes?.let { add("Prep ${Formatting.minutes(it)}") }
            recipe.cookMinutes?.let { add("Cook ${Formatting.minutes(it)}") }
            current?.cookedBy?.takeIf { it.isNotBlank() }?.let { add("Cooked by $it") }
            recipe.source.takeIf { it.isNotBlank() }?.let { add("Source: $it") }
        }.joinToString("    ·    ")
        if (meta.isNotEmpty()) {
            w.flow(textLayout(meta, muted, w.width))
            w.y += 4
        }
        if (recipe.tags.isNotEmpty()) {
            w.flow(textLayout(recipe.tags.joinToString("   ") { "#$it" }, PdfStyle.paint(9f, PdfStyle.ACCENT, PdfStyle.sans), w.width))
            w.y += 4
        }
        if (options.includeRatings && current?.overallScore != null) {
            w.y += 6
            scoreLine(w, current)
        }

        w.y += 12
        w.canvas?.drawLine(w.left, w.y, w.right, w.y, PdfStyle.stroke(PdfStyle.LINE, 0.8f))
        w.y += 20

        if (current == null) return
        ingredientsAndMethod(w, current)

        if (options.includeNotes) {
            callout(w, "WHAT CHANGED THIS TIME", current.whatChanged, PdfStyle.BLUE, PdfStyle.BLUE_SOFT)
            callout(w, "NOTES", current.notes, PdfStyle.ACCENT, PdfStyle.ACCENT_SOFT)
            callout(w, "NEXT TIME", current.nextTime, PdfStyle.OLIVE, PdfStyle.OLIVE_SOFT)
        }
        if (options.includeRatings) ratings(w, current)
        if (options.includePhotos) {
            val extra = current.photos.filter { it.id != heroPhoto?.id && it.file.exists() }
            gallery(w, extra)
        }
        if (options.includeHistory && recipe.iterations.size > 1) history(w, recipe, current)
    }

    private fun scoreLine(w: PageWriter, it: Iteration) {
        val score = it.overallScore ?: return
        val lineHeight = 18f
        w.ensure(lineHeight)
        val c = w.canvas
        val centerY = w.y + lineHeight / 2
        val starsWidth = c.drawStars(w.left, centerY, 13f, score)
        var x = w.left + starsWidth + 10f
        val scorePaint = PdfStyle.paint(14f, PdfStyle.INK, PdfStyle.serifBold)
        val scoreText = Scores.format(score)
        c?.drawText(scoreText, x, centerY + 5f, scorePaint)
        x += scorePaint.measureText(scoreText) + 10f
        val parts = buildList {
            add(if (it.ratingCount == 1) "1 rating" else "average of ${it.ratingCount} ratings")
            it.selfRating?.let { s -> add("cook ${Scores.format(s)}") }
            it.guestAverage?.let { g -> add("guests ${Scores.format(g)}") }
        }.joinToString("  ·  ")
        c?.drawText(parts, x, centerY + 4f, muted)
        w.y += lineHeight
    }

    // ---------------------------------------------------------------- ingredients & method

    private inner class IngredientBox(items: List<String>, val width: Float, columns: Int) {
        private val pad = 14f
        private val columnGap = 16f
        private val itemGap = 5.5f
        private val header = textLayout("INGREDIENTS", kicker, width - 2 * pad)
        private val columnWidth = (width - 2 * pad - (columns - 1) * columnGap) / columns
        private val entries: List<Pair<StaticLayout, Boolean>> = items.map { line ->
            if (TextLists.isSubheading(line)) {
                textLayout(line.trim().trimEnd(':'), PdfStyle.paint(9.6f, PdfStyle.INK, PdfStyle.sansBold), columnWidth) to true
            } else {
                textLayout(line.trim(), PdfStyle.paint(9.9f, PdfStyle.INK, PdfStyle.sans), columnWidth - 11f) to false
            }
        }
        private val columnsContent: List<List<Pair<StaticLayout, Boolean>>>
        val height: Float

        init {
            val heights = entries.map { it.first.height + itemGap + if (it.second) 4f else 0f }
            val total = heights.sum()
            val split = mutableListOf<MutableList<Pair<StaticLayout, Boolean>>>(mutableListOf())
            var acc = 0f
            entries.forEachIndexed { i, e ->
                if (split.size < columns && acc >= total / columns * split.size && split.last().isNotEmpty()) {
                    split += mutableListOf<Pair<StaticLayout, Boolean>>()
                }
                split.last() += e
                acc += heights[i]
            }
            columnsContent = split
            val tallest = split.maxOf { col -> col.sumOf { (it.first.height + itemGap + if (it.second) 4f else 0f).toDouble() }.toFloat() }
            height = pad + header.height + 10f + tallest - itemGap + pad
        }

        fun draw(c: Canvas?, x: Float, y: Float) {
            c ?: return
            c.drawRoundRect(RectF(x, y, x + width, y + height), 10f, 10f, PdfStyle.fill(PdfStyle.BOX))
            c.drawLayoutAt(header, x + pad, y + pad)
            val bullet = PdfStyle.fill(PdfStyle.ACCENT)
            columnsContent.forEachIndexed { col, list ->
                val cx = x + pad + col * (columnWidth + columnGap)
                var cy = y + pad + header.height + 10f
                list.forEachIndexed { i, (layout, isHeading) ->
                    if (isHeading) {
                        if (i > 0) cy += 4f
                        c.drawLayoutAt(layout, cx, cy)
                    } else {
                        val mid = cy + layout.getLineBaseline(0) - layout.paint.textSize * 0.33f
                        c.drawCircle(cx + 2.5f, mid, 1.9f, bullet)
                        c.drawLayoutAt(layout, cx + 11f, cy)
                    }
                    cy += layout.height + itemGap
                }
            }
        }
    }

    private fun ingredientsAndMethod(w: PageWriter, it: Iteration) {
        val ingredients = it.ingredients.filter { line -> line.isNotBlank() }
        val steps = it.steps.filter { line -> line.isNotBlank() }
        if (ingredients.isEmpty() && steps.isEmpty()) return
        val gap = 22f

        if (steps.isEmpty()) {
            val box = IngredientBox(ingredients, w.width, if (ingredients.size > 6) 2 else 1)
            placeBoxFullWidth(w, box, ingredients)
            return
        }
        if (ingredients.isEmpty()) {
            method(w, steps, w.left, w.width)
            w.y += 16
            return
        }

        val sideWidth = w.width * 0.37f
        val side = IngredientBox(ingredients, sideWidth, 1)
        val wide = IngredientBox(ingredients, w.width, 2)
        when {
            side.height <= w.remaining -> sideBySide(w, side, steps, sideWidth, gap)
            wide.height <= w.remaining && ingredients.size > 6 -> {
                wide.draw(w.canvas, w.left, w.y)
                w.y += wide.height + 22
                method(w, steps, w.left, w.width)
                w.y += 16
            }
            side.height <= w.fullHeight -> {
                w.newPage()
                sideBySide(w, side, steps, sideWidth, gap)
            }
            else -> {
                placeBoxFullWidth(w, wide, ingredients)
                method(w, steps, w.left, w.width)
                w.y += 16
            }
        }
    }

    private fun sideBySide(w: PageWriter, box: IngredientBox, steps: List<String>, boxWidth: Float, gap: Float) {
        val top = w.y
        val page = w.page
        box.draw(w.canvas, w.left, top)
        method(w, steps, w.left + boxWidth + gap, w.width - boxWidth - gap)
        if (w.page == page) w.y = max(w.y, top + box.height)
        w.y += 18
    }

    private fun placeBoxFullWidth(w: PageWriter, box: IngredientBox, ingredients: List<String>) {
        if (box.height <= w.fullHeight) {
            w.ensure(box.height)
            box.draw(w.canvas, w.left, w.y)
            w.y += box.height + 22
        } else {
            // Extremely long list: plain flowing text instead of a box.
            w.flow(textLayout("INGREDIENTS", kicker, w.width))
            w.y += 8
            w.flow(textLayout(ingredients.joinToString("\n") { "•  $it" }, body, w.width))
            w.y += 20
        }
    }

    /** Numbered steps in a column; after a page break they continue across the full width. */
    private fun method(w: PageWriter, steps: List<String>, x: Float, width: Float) {
        var columnX = x
        var columnWidth = width
        val header = textLayout("METHOD", kicker, columnWidth)
        w.canvas?.drawLayoutAt(header, columnX, w.y)
        w.y += header.height + 10f
        val numberPaint = PdfStyle.paint(8.6f, 0xFFFFFFFF.toInt(), PdfStyle.sansBold)
        val circle = PdfStyle.fill(PdfStyle.ACCENT)
        steps.forEachIndexed { index, step ->
            var layout = textLayout(step.trim(), body, columnWidth - 28f)
            var height = max(layout.height.toFloat(), 17f)
            if (height > w.remaining && !w.atPageTop) {
                w.newPage()
                columnX = w.left
                columnWidth = w.width
                layout = textLayout(step.trim(), body, columnWidth - 28f)
                height = max(layout.height.toFloat(), 17f)
            }
            w.canvas?.let { c ->
                val cy = w.y + layout.getLineBaseline(0) - body.textSize * 0.35f
                c.drawCircle(columnX + 8.5f, cy, 8.5f, circle)
                val n = (index + 1).toString()
                c.drawText(n, columnX + 8.5f - numberPaint.measureText(n) / 2, cy + numberPaint.textSize * 0.36f, numberPaint)
            }
            if (height <= w.remaining) {
                w.canvas?.drawLayoutAt(layout, columnX + 28f, w.y)
                w.y += height
            } else {
                w.flow(layout, columnX + 28f)
            }
            w.y += 10f
        }
    }

    // ---------------------------------------------------------------- notes, ratings, photos, history

    private fun callout(w: PageWriter, label: String, text: String, color: Int, soft: Int) {
        if (text.isBlank()) return
        val pad = 13f
        val inner = w.width - 2 * pad - 4f
        val labelLayout = textLayout(label, PdfStyle.paint(7.8f, color, PdfStyle.sansBold, 0.16f), inner)
        val bodyLayout = textLayout(text.trim(), body, inner)
        val height = pad + labelLayout.height + 5f + bodyLayout.height + pad
        if (height <= w.fullHeight) {
            w.ensure(height)
            w.canvas?.let { c ->
                val rect = RectF(w.left, w.y, w.right, w.y + height)
                c.save()
                c.clipPath(Path().apply { addRoundRect(rect, 8f, 8f, Path.Direction.CW) })
                c.drawRect(rect, PdfStyle.fill(soft))
                c.drawRect(w.left, w.y, w.left + 4f, w.y + height, PdfStyle.fill(color))
                c.restore()
                c.drawLayoutAt(labelLayout, w.left + pad + 4f, w.y + pad)
                c.drawLayoutAt(bodyLayout, w.left + pad + 4f, w.y + pad + labelLayout.height + 5f)
            }
            w.y += height + 12f
        } else {
            w.flow(labelLayout)
            w.y += 5f
            w.flow(bodyLayout)
            w.y += 14f
        }
    }

    private fun sectionHeading(w: PageWriter, text: String) {
        val layout = textLayout(text, PdfStyle.paint(15.5f, PdfStyle.INK, PdfStyle.serifBold), w.width)
        w.ensure(layout.height + 70f)
        w.y += 6f
        w.canvas?.drawLayoutAt(layout, w.left, w.y)
        w.y += layout.height + 5f
        w.canvas?.drawRect(w.left, w.y, w.left + 26f, w.y + 2f, PdfStyle.fill(PdfStyle.ACCENT))
        w.y += 14f
    }

    private fun ratings(w: PageWriter, it: Iteration) {
        if (it.ratings.isEmpty()) return
        sectionHeading(w, "What everyone thought")
        data class Row(val name: String, val score: Float, val comment: String)
        val rows = buildList {
            it.selfRating?.let { s -> add(Row(if (it.cookedBy.isBlank()) "The cook" else "${it.cookedBy} (cook)", s, "")) }
            it.ratings.forEach { r -> add(Row(r.raterName, r.score, r.comment)) }
        }
        val nameWidth = 128f
        val starsX = w.left + nameWidth
        val commentX = starsX + 98f
        val namePaint = PdfStyle.paint(10f, PdfStyle.INK, PdfStyle.sansBold)
        val commentPaint = PdfStyle.paint(10f, PdfStyle.MUTED, PdfStyle.serifItalic)
        for (row in rows) {
            val name = textLayout(row.name, namePaint, nameWidth - 10f, maxLines = 2)
            val comment = if (row.comment.isBlank()) null else textLayout("“${row.comment}”", commentPaint, w.right - commentX)
            val height = max(max(name.height, comment?.height ?: 0), 14).toFloat() + 12f
            w.ensure(height)
            w.canvas?.let { c ->
                c.drawLayoutAt(name, w.left, w.y)
                val mid = w.y + name.getLineBaseline(0) - 3.5f
                val starsWidth = c.drawStars(starsX, mid, 9.5f, row.score, 1.6f)
                c.drawText(Scores.format(row.score), starsX + starsWidth + 6f, w.y + name.getLineBaseline(0), PdfStyle.paint(9.5f, PdfStyle.INK, PdfStyle.sansBold))
                comment?.let { l -> c.drawLayoutAt(l, commentX, w.y) }
                c.drawLine(w.left, w.y + height - 6f, w.right, w.y + height - 6f, PdfStyle.stroke(PdfStyle.LINE, 0.5f))
            }
            w.y += height
        }
        w.y += 12f
    }

    private fun gallery(w: PageWriter, photos: List<Photo>) {
        if (photos.isEmpty()) return
        sectionHeading(w, "Photos")
        val gap = 10f
        val cell = (w.width - 2 * gap) / 3
        val captionPaint = PdfStyle.paint(8.5f, PdfStyle.MUTED, PdfStyle.serifItalic)
        for (row in photos.chunked(3)) {
            val captions = row.map { p -> p.caption.takeIf { it.isNotBlank() }?.let { textLayout(it, captionPaint, cell, maxLines = 2) } }
            val captionHeight = captions.maxOf { it?.height ?: 0 }.toFloat()
            val height = cell + if (captionHeight > 0) captionHeight + 5f else 0f
            w.ensure(height)
            w.canvas?.let { c ->
                row.forEachIndexed { i, photo ->
                    val x = w.left + i * (cell + gap)
                    c.drawCropped(loadBitmap(photo.file, 520), RectF(x, w.y, x + cell, w.y + cell), 8f)
                    captions[i]?.let { l -> c.drawLayoutAt(l, x, w.y + cell + 5f) }
                }
            }
            w.y += height + gap
        }
        w.y += 8f
    }

    private fun history(w: PageWriter, recipe: Recipe, current: Iteration) {
        sectionHeading(w, "Iteration history")
        val first = recipe.iterations.first().cookedOn
        val last = recipe.iterations.last().cookedOn
        w.flow(textLayout("Cooked ${Formatting.times(recipe.iterations.size, "time")} between ${Formatting.fullDate(first)} and ${Formatting.fullDate(last)}.", muted, w.width))
        w.y += 12f

        val titlePaint = PdfStyle.paint(10.2f, PdfStyle.INK, PdfStyle.sansBold)
        val thumbSize = 52f
        for (it in recipe.iterations.asReversed()) {
            val thumb = if (options.includePhotos) it.photos.firstOrNull()?.file?.takeIf { f -> f.exists() } else null
            val textX = w.left + if (thumb != null) thumbSize + 14f else 0f
            val starsSpace = if (options.includeRatings && it.overallScore != null) 96f else 0f
            val textWidth = w.right - textX
            val title = buildString {
                append("Iteration ${it.number}  ·  ${Formatting.fullDate(it.cookedOn)}")
                if (it.cookedBy.isNotBlank()) append("  ·  ${it.cookedBy}")
                if (it.id == current.id) append("  (this version)")
            }
            val titleLayout = textLayout(title, titlePaint, textWidth - starsSpace)
            val details = SpannableStringBuilder()
            fun addLine(label: String, value: String) {
                if (value.isBlank()) return
                if (details.isNotEmpty()) details.append("\n")
                val start = details.length
                details.append(label)
                details.setSpan(StyleSpan(Typeface.BOLD), start, details.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                details.setSpan(ForegroundColorSpan(PdfStyle.INK), start, details.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                details.append(" ").append(value.trim().replace("\n", " "))
            }
            addLine("Changed:", it.whatChanged)
            addLine("Next time:", it.nextTime)
            if (it.id != current.id && it.whatChanged.isBlank() && it.nextTime.isBlank()) addLine("Notes:", it.notes)
            val detailLayout = if (details.isEmpty()) null else textLayout(details, PdfStyle.paint(9.3f, PdfStyle.MUTED, PdfStyle.sans), textWidth)
            val textHeight = titleLayout.height + (detailLayout?.let { l -> l.height + 4f } ?: 0f)
            val height = max(textHeight, if (thumb != null) thumbSize else 0f) + 16f

            if (height <= w.fullHeight) {
                w.ensure(height)
                w.canvas?.let { c ->
                    thumb?.let { f -> c.drawCropped(loadBitmap(f, 200), RectF(w.left, w.y, w.left + thumbSize, w.y + thumbSize), 6f) }
                    c.drawLayoutAt(titleLayout, textX, w.y)
                    if (starsSpace > 0f) {
                        val mid = w.y + titleLayout.getLineBaseline(0) - 3.5f
                        val scoreText = Scores.format(it.overallScore)
                        val scorePaint = PdfStyle.paint(9.5f, PdfStyle.INK, PdfStyle.sansBold)
                        val scoreWidth = scorePaint.measureText(scoreText)
                        c.drawStars(w.right - scoreWidth - 6f - 58f, mid, 9.5f, it.overallScore!!, 2f)
                        c.drawText(scoreText, w.right - scoreWidth, w.y + titleLayout.getLineBaseline(0), scorePaint)
                    }
                    detailLayout?.let { l -> c.drawLayoutAt(l, textX, w.y + titleLayout.height + 4f) }
                    c.drawLine(w.left, w.y + height - 8f, w.right, w.y + height - 8f, PdfStyle.stroke(PdfStyle.LINE, 0.5f))
                }
                w.y += height
            } else {
                w.flow(titleLayout, textX)
                detailLayout?.let { l -> w.flow(l, textX) }
                w.y += 16f
            }
        }
    }
}
