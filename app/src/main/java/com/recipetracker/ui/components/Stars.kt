package com.recipetracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.recipetracker.model.Scores
import com.recipetracker.ui.theme.StarColor
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

private fun starPath(center: Offset, outer: Float, inner: Float = outer * 0.48f): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val angle = Math.toRadians(-90.0 + i * 36.0)
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat() + outer * 0.06f
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.drawStarRow(rating: Float, gapPx: Float, emptyColor: Color) {
    val starSize = (size.width - 4 * gapPx) / 5
    for (i in 0 until 5) {
        val left = i * (starSize + gapPx)
        val path = starPath(Offset(left + starSize / 2, size.height / 2), starSize / 2)
        drawPath(path, emptyColor)
        val fraction = (rating - i).coerceIn(0f, 1f)
        if (fraction > 0f) {
            clipRect(left = left, top = 0f, right = left + starSize * fraction, bottom = size.height) {
                drawPath(path, StarColor)
            }
        }
    }
}

/** Read-only stars; fractions such as an average of 3.7 are shown partly filled. */
@Composable
fun StarRatingDisplay(rating: Float?, modifier: Modifier = Modifier, starSize: Dp = 16.dp, gap: Dp = 2.dp) {
    val empty = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier
            .size(width = starSize * 5 + gap * 4, height = starSize)
            .semantics { contentDescription = if (rating == null) "Not rated" else "${Scores.format(rating)} out of 5 stars" },
    ) {
        drawStarRow(rating ?: 0f, gap.toPx(), empty)
    }
}

/** Stars plus the number, e.g. ★★★★☆ 4.3 */
@Composable
fun ScoreRow(score: Float?, modifier: Modifier = Modifier, starSize: Dp = 16.dp, showNumber: Boolean = true) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StarRatingDisplay(score, starSize = starSize)
        if (showNumber && score != null) {
            Spacer(Modifier.width(6.dp))
            Text(Scores.format(score), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Tap or slide over the stars to rate in half steps. Tapping the left half of a star
 * gives a half star.
 */
@Composable
fun StarRatingInput(
    rating: Float?,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 40.dp,
    gap: Dp = 6.dp,
) {
    val empty = MaterialTheme.colorScheme.outlineVariant
    fun valueAt(x: Float, width: Float): Float {
        val raw = (x / width * 5f).coerceIn(0.01f, 5f)
        return (ceil(raw * 2f) / 2f).coerceIn(0.5f, 5f)
    }
    Canvas(
        modifier
            .size(width = starSize * 5 + gap * 4, height = starSize)
            .pointerInput(Unit) {
                detectTapGestures { offset -> onRatingChange(valueAt(offset.x, size.width.toFloat())) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    onRatingChange(valueAt(change.position.x, size.width.toFloat()))
                }
            }
            .semantics {
                contentDescription = "Rating"
                progressBarRangeInfo = ProgressBarRangeInfo(rating ?: 0f, 0f..5f, steps = 9)
                setProgress { value ->
                    onRatingChange((Math.round(value * 2f) / 2f).coerceIn(0.5f, 5f))
                    true
                }
            },
    ) {
        drawStarRow(rating ?: 0f, gap.toPx(), empty)
    }
}

/** Rating input with the friendly word underneath ("Really good"). */
@Composable
fun LabeledStarInput(
    rating: Float?,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 40.dp,
) {
    androidx.compose.foundation.layout.Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StarRatingInput(rating, onRatingChange, starSize = starSize)
        Text(
            text = if (rating == null) Scores.label(null) else "${Scores.format(rating)} · ${Scores.label(rating)}",
            style = MaterialTheme.typography.titleSmall,
            color = if (rating == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
        )
    }
}
