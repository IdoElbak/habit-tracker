package com.idoelbak.tracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.text.BidiFormatter
import com.idoelbak.tracker.data.DayDot
import com.idoelbak.tracker.ui.theme.theme

/**
 * Wraps a user-entered string in Unicode isolate marks before it is drawn.
 *
 * Without this, a Hebrew habit name containing an English word or a trailing "?" leaks its direction
 * into the surrounding text and the sentence visibly reorders -- the WhatsApp/Outlook bug. Pair it
 * with `TextDirection.Content`, which every style in the typography already carries.
 */
fun String.isolated(): String = BidiFormatter.getInstance().unicodeWrap(this)

/**
 * The locale as Compose sees it.
 *
 * `Locale.getDefault()` is read once and never observed, so weekday names would keep their old
 * language after a switch to Hebrew until the whole activity was recreated.
 */
@Composable
fun currentLocale(): java.util.Locale = LocalConfiguration.current.locales[0]

/** The SVG path data from the design canvas, reused verbatim so the app matches the artboards. */
object Glyphs {
    const val FLAME =
        "M12 2.8c2.6 3.1 4.7 5.5 4.7 8.4a4.7 4.7 0 1 1-9.4 0c0-1.5.5-2.7 1.4-3.8.2 1.5.9 2.4 1.9 2.7-.3-2.7.4-5.2 1.4-7.3z"
    const val CHECK = "M3.5 8.4 6.6 11.5 12.5 4.8"
    const val FREEZE = "M12 3v18M4.5 7.5l15 9M19.5 7.5l-15 9"
    const val TODAY =
        "M4 6.5 5.6 8.1 8.5 5M4 12.5 5.6 14.1 8.5 11M4 18.5 5.6 20.1 8.5 17M12 6.5h8M12 12.5h8M12 18.5h8"
    const val BARS = "M4 20V13M10 20V4M16 20V9M22 20V15"
    const val LIST = "M4 6.5h16M4 12.5h16M4 18.5h16"
    const val WEEK =
        "M4 7.5h16M4 7.5v11h16v-11M4 7.5l0-2.5h16v2.5M8.5 3.5v3M15.5 3.5v3M8 12h2M14 12h2M8 15.5h2M14 15.5h2"
    const val GEAR =
        "M12 8.8a3.2 3.2 0 1 1 0 6.4a3.2 3.2 0 1 1 0-6.4M12 2.8v3M12 18.2v3M21.2 12h-3M5.8 12h-3" +
            "M18.5 5.5l-2.1 2.1M7.6 16.4l-2.1 2.1M18.5 18.5l-2.1-2.1M7.6 7.6 5.5 5.5"
    const val PLUS = "M12 5v14M5 12h14"
}

/**
 * Draws one of [Glyphs]. Filled when [strokeWidth] is null, stroked otherwise -- the design's icons
 * are strokes and its flame is a fill.
 */
@Composable
fun Glyph(
    pathData: String,
    size: Dp,
    tint: Color,
    strokeWidth: Float? = null,
    viewport: Float = 24f
) {
    val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
    Canvas(Modifier.size(size)) {
        val factor = this.size.minDimension / viewport
        scale(factor, pivot = Offset.Zero) {
            if (strokeWidth == null) {
                drawPath(path, tint)
            } else {
                drawPath(
                    path,
                    tint,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/** A section overline: DUE TODAY, THIS WEEK. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) = Text(
    text = text.uppercase(),
    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
    color = theme.muted,
    modifier = modifier
)

/** The day's progress, as a ring. Empty rings read as "nothing done yet", not as an error. */
@Composable
fun ProgressRing(done: Int, total: Int, modifier: Modifier = Modifier, diameter: Dp = 54.dp) {
    val track = theme.track
    val fill = theme.success
    Canvas(modifier.size(diameter)) {
        val stroke = size.minDimension * 0.093f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(stroke)
        )
        if (total > 0 && done > 0) {
            drawArc(
                color = fill,
                startAngle = -90f,
                sweepAngle = 360f * done / total,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
    }
}

/** Seven dots: how this habit went across the current week. */
@Composable
fun WeekStrip(dots: List<DayDot>) {
    val hit = theme.success
    val miss = theme.track
    val today = theme.primary
    val idle = theme.rule
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        dots.forEach { dot ->
            if (dot == DayDot.TODAY) {
                Box(Modifier.size(5.dp).border(1.5.dp, today, CircleShape))
            } else {
                val colour = when (dot) {
                    DayDot.HIT -> hit
                    DayDot.MISS -> miss
                    else -> idle
                }
                Box(Modifier.size(5.dp).background(colour, CircleShape))
            }
        }
    }
}

/** The 22dp tick box from the design: filled sage when done, quiet outline when not. */
@Composable
fun TickBox(done: Boolean, modifier: Modifier = Modifier) {
    val fill = theme.success
    Box(
        modifier
            .size(22.dp)
            .background(if (done) fill else Color.Transparent, RoundedCornerShape(7.dp))
            .border(if (done) 0.dp else 1.8.dp, if (done) Color.Transparent else theme.track, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (done) Glyph(Glyphs.CHECK, 12.dp, Color.White, strokeWidth = 2.4f, viewport = 16f)
    }
}

/** A thin progress bar: how much of a week's plan is banked. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) = Box(
    modifier
        .fillMaxWidth()
        .height(6.dp)
        .background(theme.track, CircleShape)
) {
    if (fraction > 0f) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .background(theme.success, CircleShape)
        )
    }
}

/** A rounded pill of text -- the streak counter, the freeze count, a status chip. */
@Composable
fun Pill(
    background: Color,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) = Row(
    modifier = Modifier
        .background(background, CircleShape)
        .padding(horizontal = 12.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    content = content
)
