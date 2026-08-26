package com.idoelbak.tracker.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.idoelbak.tracker.MainActivity
import com.idoelbak.tracker.R
import com.idoelbak.tracker.data.Prefs
import com.idoelbak.tracker.data.TodayUi
import com.idoelbak.tracker.data.TrackerRepository
import com.idoelbak.tracker.data.db.TrackerDatabase
import com.idoelbak.tracker.ui.theme.Palettes
import com.idoelbak.tracker.ui.theme.Tokens
import kotlinx.coroutines.flow.first

private val Small = DpSize(56.dp, 56.dp)
private val Wide = DpSize(130.dp, 56.dp)

/**
 * The home-screen widget: how the day is going, without opening anything.
 *
 * One widget rather than two. It declares a 1x1 target and a responsive wide layout, so dropping it
 * in a single cell gives the ring and the count, and stretching it to two cells adds the streak --
 * no second widget, no rebuild, no second thing to keep in step.
 */
class TrackerWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(Small, Wide))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = Prefs(context).flow.first()
        val repo = TrackerRepository(TrackerDatabase.get(context))
        // A widget refresh is also a chance to settle days that finished while the app was shut.
        repo.settle(prefs.weekStart)
        val date = repo.today()
        val ui = repo.observeToday(date, prefs.weekStart).first()

        val palette = Palettes.byId(prefs.paletteId)
        val night = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val tokens = if (night) palette.dark else palette.light

        provideContent { Content(context, ui, tokens) }
    }
}

@Composable
private fun Content(context: Context, ui: TodayUi, tokens: Tokens) {
    val size = androidx.glance.LocalSize.current
    val ring = ImageProvider(ringBitmap(context, ui, tokens))

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(tokens.surface))
            .cornerRadius(20.dp)
            .padding(6.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(provider = ring, contentDescription = null, modifier = GlanceModifier.size(44.dp))
            Text(
                if (ui.dueCount == 0 || ui.left == 0) "" else "${ui.left}",
                style = TextStyle(
                    color = ColorProvider(tokens.ink),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (size.width >= Wide.width) {
            Column(modifier = GlanceModifier.padding(start = 10.dp)) {
                Text(
                    "${ui.streak}",
                    style = TextStyle(
                        color = ColorProvider(tokens.primary),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    if (ui.left == 0) context.getString(R.string.widget_streak_done)
                    else context.getString(R.string.widget_streak_left, ui.left),
                    style = TextStyle(color = ColorProvider(tokens.muted), fontSize = 11.sp)
                )
            }
        }
    }
}

/**
 * Glance has no drawing API, so the ring is painted into a bitmap the widget shows as an image.
 * A finished day is a full ring with a tick; an empty one is a quiet track, never a red warning.
 */
private fun ringBitmap(context: Context, ui: TodayUi, tokens: Tokens): Bitmap {
    val px = (44 * context.resources.displayMetrics.density).toInt().coerceAtLeast(44)
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val stroke = px * 0.11f
    val box = RectF(stroke / 2f, stroke / 2f, px - stroke / 2f, px - stroke / 2f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }

    paint.color = tokens.track.toArgb()
    canvas.drawOval(box, paint)

    val done = if (ui.dueCount == 0) 0f else ui.doneCount.toFloat() / ui.dueCount
    if (done > 0f) {
        paint.color = tokens.success.toArgb()
        canvas.drawArc(box, -90f, 360f * done, false, paint)
    }

    if (ui.dueCount > 0 && ui.left == 0) {
        paint.strokeWidth = stroke * 0.8f
        val tick = android.graphics.Path().apply {
            moveTo(px * 0.30f, px * 0.52f)
            lineTo(px * 0.44f, px * 0.66f)
            lineTo(px * 0.71f, px * 0.36f)
        }
        canvas.drawPath(tick, paint)
    }

    return bitmap
}

class TrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TrackerWidget()
}
