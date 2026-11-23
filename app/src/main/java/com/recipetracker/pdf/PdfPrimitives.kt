package com.recipetracker.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.cos
import kotlin.math.sin

/** Where finished pages go: a real PDF, bitmaps in tests, or nowhere when only measuring. */
interface PageSink {
    /** Starts page [number] (1-based) and returns the canvas to draw on, or null when only measuring. */
    fun startPage(number: Int, width: Int, height: Int): Canvas?
    fun finishPage()
}

class MeasuringSink : PageSink {
    override fun startPage(number: Int, width: Int, height: Int): Canvas? = null
    override fun finishPage() = Unit
}

class PdfDocumentSink(private val document: PdfDocument) : PageSink {
    private var page: PdfDocument.Page? = null

    override fun startPage(number: Int, width: Int, height: Int): Canvas {
        val p = document.startPage(PdfDocument.PageInfo.Builder(width, height, number).create())
        page = p
        return p.canvas
    }

    override fun finishPage() {
        page?.let { document.finishPage(it) }
        page = null
    }
}

/** Colours and type for the exported cookbook. */
object PdfStyle {
    const val INK = 0xFF2B2220.toInt()
    const val MUTED = 0xFF7D6C66.toInt()
    const val ACCENT = 0xFFA0461F.toInt()
    const val ACCENT_SOFT = 0xFFF7E6DD.toInt()
    const val BOX = 0xFFFBF6F2.toInt()
    const val LINE = 0xFFE9DCD4.toInt()
    const val STAR = 0xFFE3A21A.toInt()
    const val STAR_EMPTY = 0xFFE8DED7.toInt()
    const val OLIVE = 0xFF5F6A2A.toInt()
    const val OLIVE_SOFT = 0xFFF0F1DF.toInt()
    const val BLUE = 0xFF3D6378.toInt()
    const val BLUE_SOFT = 0xFFE7F0F4.toInt()
    const val COVER_BG = 0xFFFBF4EE.toInt()
    const val PLACEHOLDER = 0xFFF0E3DA.toInt()

    val serifBold: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    val serif: Typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    val serifItalic: Typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    val sans: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    val sansBold: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    fun paint(size: Float, color: Int = INK, typeface: Typeface = sans, letterSpacing: Float = 0f) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            this.typeface = typeface
            this.letterSpacing = letterSpacing
        }

    fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
    }
}

fun textLayout(
    text: CharSequence,
    paint: TextPaint,
    width: Float,
    align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    lineSpacing: Float = 1.25f,
    maxLines: Int = Int.MAX_VALUE,
): StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(10))
    .setAlignment(align)
    .setLineSpacing(0f, lineSpacing)
    .setIncludePad(false)
    .setMaxLines(maxLines)
    .setEllipsize(if (maxLines == Int.MAX_VALUE) null else android.text.TextUtils.TruncateAt.END)
    .build()

fun Canvas.drawLayoutAt(layout: StaticLayout, x: Float, y: Float) {
    save()
    translate(x, y)
    layout.draw(this)
    restore()
}

/** Five stars, filled up to [rating] (fractions allowed). Returns the drawn width. */
fun Canvas?.drawStars(x: Float, centerY: Float, size: Float, rating: Float, gap: Float = size * 0.18f): Float {
    val width = 5 * size + 4 * gap
    val canvas = this ?: return width
    val empty = PdfStyle.fill(PdfStyle.STAR_EMPTY)
    val full = PdfStyle.fill(PdfStyle.STAR)
    for (i in 0 until 5) {
        val left = x + i * (size + gap)
        val path = starPath(left + size / 2, centerY, size / 2)
        canvas.drawPath(path, empty)
        val fraction = (rating - i).coerceIn(0f, 1f)
        if (fraction > 0f) {
            canvas.save()
            canvas.clipRect(left, centerY - size, left + size * fraction, centerY + size)
            canvas.drawPath(path, full)
            canvas.restore()
        }
    }
    return width
}

fun starPath(cx: Float, cy: Float, outer: Float, inner: Float = outer * 0.48f): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val angle = Math.toRadians(-90.0 + i * 36.0)
        val px = cx + (r * cos(angle)).toFloat()
        val py = cy + (r * sin(angle)).toFloat() + outer * 0.06f
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    close()
}

/** Draws [bitmap] filling [dst] (cropping the overflow, like centerCrop) with rounded corners. */
fun Canvas.drawCropped(bitmap: Bitmap?, dst: RectF, radius: Float) {
    save()
    clipPath(Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) })
    if (bitmap == null) {
        drawRect(dst, PdfStyle.fill(PdfStyle.PLACEHOLDER))
    } else {
        val srcAspect = bitmap.width.toFloat() / bitmap.height
        val dstAspect = dst.width() / dst.height()
        val src = if (srcAspect > dstAspect) {
            val w = (bitmap.height * dstAspect).toInt()
            val l = (bitmap.width - w) / 2
            Rect(l, 0, l + w, bitmap.height)
        } else {
            val h = (bitmap.width / dstAspect).toInt()
            val t = (bitmap.height - h) / 2
            Rect(0, t, bitmap.width, t + h)
        }
        drawBitmap(bitmap, src, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }
    restore()
}
