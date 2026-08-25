package com.idoelbak.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.idoelbak.tracker.data.HabitRow
import com.idoelbak.tracker.data.TodayUi
import com.idoelbak.tracker.ui.theme.theme
import java.time.format.DateTimeFormatter

/**
 * The day, and only the day.
 *
 * Habits that are not expected today are deliberately absent -- not greyed out, not collapsed at the
 * bottom. They live on the Habits page and come back here on the day they are due. A short list is
 * the whole point: what is on this screen is what has to happen.
 */
@Composable
fun TodayScreen(
    ui: TodayUi,
    onToggle: (Long) -> Unit,
    onRate: (Int?, Int?) -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 20.dp, 20.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Today", style = MaterialTheme.typography.headlineMedium, color = theme.ink)
                    Text(
                        ui.date.format(dateFormat),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.muted
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Pill(theme.primary) {
                        Glyph(Glyphs.FLAME, 15.dp, theme.onPrimary)
                        Text(
                            "${ui.streak}",
                            style = MaterialTheme.typography.labelLarge,
                            color = theme.onPrimary
                        )
                    }
                    // Settings is a rare visit, so it gets a corner rather than a fifth tab.
                    Box(Modifier.clickable(onClick = onSettings).padding(4.dp)) {
                        Glyph(Glyphs.GEAR, 21.dp, theme.muted, strokeWidth = 2f)
                    }
                }
            }
        }

        item { SummaryCard(ui) }

        if (ui.due.isEmpty() && ui.weekly.isEmpty()) {
            item { EmptyState(onAdd) }
        }

        if (ui.due.isNotEmpty()) {
            item { SectionLabel("Due today") }
            items(ui.due, key = { it.id }) { row ->
                HabitTickRow(row, onToggle = { onToggle(row.id) })
            }
        }

        if (ui.weekly.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SectionLabel("Optional")
                    Text(
                        "Doesn't count toward the day — ticking one takes it off a later day",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.muted
                    )
                }
            }
            items(ui.weekly, key = { it.id }) { row ->
                HabitTickRow(row, onToggle = { onToggle(row.id) }, quiet = true)
            }
        }

        if (ui.due.isNotEmpty() || ui.weekly.isNotEmpty()) {
            item { RatingCard(ui, onRate) }
        }
    }
}

@Composable
private fun SummaryCard(ui: TodayUi) = Row(
    Modifier
        .fillMaxWidth()
        .background(theme.surface, RoundedCornerShape(16.dp))
        .padding(16.dp, 14.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    ProgressRing(ui.doneCount, ui.dueCount)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "${ui.doneCount} of ${ui.dueCount} done",
            style = MaterialTheme.typography.titleLarge,
            color = theme.ink
        )
        Text(
            when {
                ui.dueCount == 0 -> "Nothing due today"
                ui.left == 0 -> "All done — streak safe"
                else -> "${ui.left} left"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = theme.muted
        )
    }
    Pill(theme.success.copy(alpha = 0.18f)) {
        Glyph(Glyphs.FREEZE, 13.dp, theme.success, strokeWidth = 2f)
        Text("${ui.freezes}", style = MaterialTheme.typography.labelMedium, color = theme.success)
    }
}

@Composable
private fun HabitTickRow(row: HabitRow, onToggle: () -> Unit, quiet: Boolean = false) = Row(
    Modifier
        .fillMaxWidth()
        .height(48.dp)
        .background(if (quiet) theme.surfaceAlt else theme.surface, RoundedCornerShape(12.dp))
        .then(if (quiet) Modifier.border(1.dp, theme.rule, RoundedCornerShape(12.dp)) else Modifier)
        .clickable(onClick = onToggle)
        .padding(horizontal = 13.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    TickBox(row.done)
    Column(Modifier.weight(1f)) {
        Text(
            listOfNotNull(row.emoji, row.name).joinToString(" ").isolated(),
            style = MaterialTheme.typography.bodyLarge,
            color = if (row.done) theme.muted else theme.ink,
            textDecoration = if (row.done) TextDecoration.LineThrough else null
        )
        row.quota?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = theme.muted)
        }
    }
    WeekStrip(row.week)
}

@Composable
private fun EmptyState(onAdd: () -> Unit) = Column(
    Modifier
        .fillMaxWidth()
        .background(theme.surface, RoundedCornerShape(16.dp))
        .clickable(onClick = onAdd)
        .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp)
) {
    Text("Nothing defined yet", style = MaterialTheme.typography.titleMedium, color = theme.ink)
    Text(
        "Add the first habit and it shows up here on the days it is due.",
        style = MaterialTheme.typography.bodyMedium,
        color = theme.muted
    )
    Box(Modifier.padding(top = 8.dp)) {
        Pill(theme.primary) {
            Text("Add a habit", style = MaterialTheme.typography.labelMedium, color = theme.onPrimary)
        }
    }
}

/** Mood and motivation, both optional. A skipped rating stays null rather than becoming a zero. */
@Composable
private fun RatingCard(ui: TodayUi, onRate: (Int?, Int?) -> Unit) = Column(
    Modifier
        .fillMaxWidth()
        .background(theme.surface, RoundedCornerShape(16.dp))
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
) {
    SectionLabel("How was today")
    RatingSlider("Mood", ui.mood) { onRate(it, ui.motivation) }
    RatingSlider("Motivation", ui.motivation) { onRate(ui.mood, it) }
}

@Composable
private fun RatingSlider(label: String, value: Int?, onChange: (Int) -> Unit) = Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = theme.ink2,
        modifier = Modifier.width(76.dp)
    )
    Slider(
        value = (value ?: 5).toFloat(),
        onValueChange = { onChange(it.toInt()) },
        valueRange = 1f..10f,
        steps = 8,
        modifier = Modifier.weight(1f),
        colors = SliderDefaults.colors(
            thumbColor = theme.primary,
            activeTrackColor = theme.primary,
            inactiveTrackColor = theme.track
        )
    )
    Text(
        value?.toString() ?: "—",
        style = MaterialTheme.typography.labelLarge,
        color = if (value == null) theme.muted else theme.ink,
        fontWeight = FontWeight.W700,
        modifier = Modifier.width(22.dp)
    )
}
