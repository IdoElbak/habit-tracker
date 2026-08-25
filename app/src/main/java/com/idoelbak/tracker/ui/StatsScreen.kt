package com.idoelbak.tracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idoelbak.tracker.core.engine.MoodFinding
import com.idoelbak.tracker.data.StatsPeriod
import com.idoelbak.tracker.data.StatsUi
import com.idoelbak.tracker.data.WeekdayBar
import com.idoelbak.tracker.ui.theme.theme
import java.time.format.TextStyle

/**
 * What all of it added up to. Settled days only -- today is still being lived, and counting it would
 * drag every percentage down all morning and back up all evening.
 */
@Composable
fun StatsScreen(
    ui: StatsUi,
    onPeriod: (StatsPeriod) -> Unit,
    modifier: Modifier = Modifier
) = LazyColumn(
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 28.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Stats", style = MaterialTheme.typography.headlineMedium, color = theme.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatsPeriod.entries.forEach { period ->
                    val on = period == ui.period
                    Text(
                        period.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) theme.onPrimary else theme.ink2,
                        modifier = Modifier
                            .background(if (on) theme.primary else theme.surface, CircleShape)
                            .clickable { onPeriod(period) }
                            .padding(horizontal = 15.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }

    if (!ui.hasHistory) {
        item {
            Card {
                Text("Nothing settled yet", style = MaterialTheme.typography.titleMedium, color = theme.ink)
                Text(
                    "Stats appear once a day has finished. Today is still being lived, so it is not counted here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.muted
                )
            }
        }
        return@LazyColumn
    }

    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigNumber("${ui.completionPercent ?: 0}%", "completed", "${ui.daysTracked} days tracked", Modifier.weight(1f))
            BigNumber("${ui.currentStreak}", "day streak", "best ${ui.bestStreak}", Modifier.weight(1f))
            BigNumber("${ui.perfectDays}", "perfect days", "nothing missed", Modifier.weight(1f))
        }
    }

    if (ui.strength.isNotEmpty()) {
        item {
            Card {
                CardTitle("Habit strength", "Dips when you miss, recovers when you practise. One bad day cannot zero it.")
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ui.strength.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                listOfNotNull(row.emoji, row.name).joinToString(" ").isolated(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = theme.ink2,
                                modifier = Modifier.width(112.dp),
                                maxLines = 1
                            )
                            Box(Modifier.weight(1f)) { ProgressBar(row.percent / 100f) }
                            Text(
                                "${row.percent}",
                                style = MaterialTheme.typography.labelMedium,
                                color = theme.ink,
                                modifier = Modifier.width(24.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }

    item {
        Card {
            CardTitle("Consistency", "The last four weeks. Read down a column to see a weekday failing.")
            HeatGrid(ui)
        }
    }

    if (ui.weekdays.any { it.percent != null }) {
        item {
            Card {
                CardTitle("Which day you drop", "Completion rate by weekday.")
                WeekdayChart(ui.weekdays)
                ui.weakestDayNote?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = theme.primary)
                }
            }
        }
    }

    if (ui.trend.size >= 2) {
        item {
            Card {
                CardTitle("Weekly completion", "The last ${ui.trend.size} weeks.")
                TrendChart(ui)
            }
        }
    }

    if (ui.mood != null || ui.motivation != null) {
        item {
            Card {
                CardTitle("Mood and motivation", "Only shown when there is enough rated history to mean something.")
                ui.mood?.let { Finding("mood", it) }
                ui.motivation?.let { Finding("motivation", it) }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) = Column(
    Modifier
        .fillMaxWidth()
        .background(theme.surface, RoundedCornerShape(16.dp))
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    content = content
)

@Composable
private fun CardTitle(title: String, subtitle: String) = Column(
    verticalArrangement = Arrangement.spacedBy(3.dp)
) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = theme.ink)
    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = theme.muted)
}

@Composable
private fun BigNumber(value: String, label: String, note: String, modifier: Modifier = Modifier) = Column(
    modifier
        .background(theme.surface, RoundedCornerShape(16.dp))
        .padding(14.dp, 16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    Text(value, style = MaterialTheme.typography.displaySmall, color = theme.ink)
    Text(label, style = MaterialTheme.typography.bodySmall, color = theme.ink2)
    Text(note, style = MaterialTheme.typography.bodySmall, color = theme.muted)
}

@Composable
private fun HeatGrid(ui: StatsUi) = Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    val heat = theme.heat
    val locale = currentLocale()
    ui.grid.forEach { week ->
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            week.forEach { cell ->
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .background(
                            if (cell.future) theme.surfaceAlt else heat[cell.level],
                            RoundedCornerShape(6.dp)
                        )
                )
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ui.grid.firstOrNull()?.forEach { cell ->
            Text(
                cell.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                style = MaterialTheme.typography.bodySmall,
                color = theme.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeekdayChart(bars: List<WeekdayBar>) = Row(
    Modifier
        .fillMaxWidth()
        .height(116.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.Bottom
) {
    val worst = bars.filter { it.percent != null }.minByOrNull { it.percent!! }?.day
    val locale = currentLocale()
    bars.forEach { bar ->
        Column(
            Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                bar.percent?.let { "$it" } ?: "–",
                style = MaterialTheme.typography.bodySmall,
                color = theme.muted
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height((((bar.percent ?: 0) * 78) / 100).dp.coerceAtLeast(3.dp))
                    .background(
                        if (bar.day == worst && bar.percent != null) theme.primary else theme.success,
                        RoundedCornerShape(6.dp)
                    )
            )
            Text(
                bar.day.getDisplayName(TextStyle.NARROW, locale),
                style = MaterialTheme.typography.bodySmall,
                color = theme.muted,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
    }
}

@Composable
private fun TrendChart(ui: StatsUi) {
    val line = theme.success
    val rule = theme.rule
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(104.dp)
        ) {
            val points = ui.trend
            val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
            fun y(percent: Int) = size.height - (size.height * percent / 100f)

            drawLine(rule, androidx.compose.ui.geometry.Offset(0f, y(100)), androidx.compose.ui.geometry.Offset(size.width, y(100)), 1f)
            drawLine(rule, androidx.compose.ui.geometry.Offset(0f, y(50)), androidx.compose.ui.geometry.Offset(size.width, y(50)), 1f)
            drawLine(rule, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1f)

            val path = Path()
            points.forEachIndexed { index, point ->
                val x = index * stepX
                if (index == 0) path.moveTo(x, y(point.percent)) else path.lineTo(x, y(point.percent))
            }
            drawPath(path, line, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

            points.lastOrNull()?.let {
                drawCircle(line, 4.5.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width, y(it.percent)))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${ui.trend.size} wks ago",
                style = MaterialTheme.typography.bodySmall,
                color = theme.muted
            )
            Text("this week", style = MaterialTheme.typography.bodySmall, color = theme.muted)
        }
    }
}

@Composable
private fun Finding(what: String, finding: MoodFinding) = Text(
    "On low-$what days you finish ${finding.relativeDropPercent}% less — " +
        "${finding.lowCompletionPercent}% against ${finding.highCompletionPercent}%.",
    style = MaterialTheme.typography.bodyMedium,
    color = theme.ink2
)
