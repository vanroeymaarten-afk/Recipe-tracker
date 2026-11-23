package com.recipetracker.pdf

import android.graphics.Canvas
import android.text.StaticLayout

/**
 * Lays content out top to bottom over A4 pages (in PDF points), starting new pages as
 * needed and drawing a footer on every page that has one.
 */
class PageWriter(private val sink: PageSink) {
    var canvas: Canvas? = null
        private set
    var page = 0
        private set
    var y = 0f

    /** Shown at the bottom left of pages; null means no footer for this page. */
    var footer: String? = null
    private var pageOpen = false

    val left = MARGIN
    val right = PAGE_WIDTH - MARGIN
    val width = right - left
    val top = TOP
    val bottom = PAGE_HEIGHT - BOTTOM
    val fullHeight = bottom - top
    val remaining: Float get() = bottom - y
    val atPageTop: Boolean get() = y <= top + 0.5f

    fun newPage(footer: String? = this.footer) {
        finishPage()
        page++
        canvas = sink.startPage(page, PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt())
        pageOpen = true
        this.footer = footer
        y = top
    }

    fun finishPage() {
        if (!pageOpen) return
        footer?.let { drawFooter(it) }
        sink.finishPage()
        pageOpen = false
        canvas = null
    }

    /** Moves to a new page unless [height] still fits on this one. */
    fun ensure(height: Float) {
        if (height > remaining && !atPageTop) newPage()
    }

    /** Draws a layout at the current position, continuing on following pages when it is too long. */
    fun flow(layout: StaticLayout, x: Float = left) {
        var line = 0
        while (line < layout.lineCount) {
            val lineHeight = (layout.getLineBottom(line) - layout.getLineTop(line)).toFloat()
            if (lineHeight > remaining && !atPageTop) newPage()
            val startTop = layout.getLineTop(line)
            var end = line
            while (end + 1 < layout.lineCount && layout.getLineBottom(end + 1) - startTop <= remaining) end++
            val endBottom = layout.getLineBottom(end)
            canvas?.let { c ->
                c.save()
                c.translate(x, y - startTop)
                c.clipRect(0f, startTop.toFloat(), layout.width.toFloat(), endBottom.toFloat())
                layout.draw(c)
                c.restore()
            }
            y += (endBottom - startTop)
            line = end + 1
        }
    }

    private fun drawFooter(text: String) {
        val c = canvas ?: return
        val lineY = PAGE_HEIGHT - 40f
        c.drawLine(left, lineY, right, lineY, PdfStyle.stroke(PdfStyle.LINE, 0.6f))
        val small = PdfStyle.paint(7.5f, PdfStyle.MUTED, PdfStyle.sans, 0.06f)
        val label = textLayout(text.uppercase(), small, width * 0.75f, maxLines = 1, lineSpacing = 1f)
        c.drawLayoutAt(label, left, lineY + 8f)
        val number = page.toString()
        val numberPaint = PdfStyle.paint(8.5f, PdfStyle.ACCENT, PdfStyle.sansBold)
        c.drawText(number, right - numberPaint.measureText(number), lineY + 16f, numberPaint)
    }

    companion object {
        const val PAGE_WIDTH = 595f
        const val PAGE_HEIGHT = 842f
        const val MARGIN = 48f
        const val TOP = 46f
        const val BOTTOM = 62f
    }
}
