package com.idoelbak.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.idoelbak.tracker.core.model.DueState
import com.idoelbak.tracker.core.model.ScheduleType
import com.idoelbak.tracker.data.DefinedHabit
import com.idoelbak.tracker.data.db.HabitEntity
import com.idoelbak.tracker.ui.theme.theme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Every habit ever defined, whatever today happens to be.
 *
 * This is where a habit that is resting today can still be found, edited, or ticked off out of turn.
 * Today's screen stays short precisely because this page exists.
 */
@Composable
fun HabitsScreen(
    habits: List<DefinedHabit>,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onUnarchive: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = habits.filter { it.habit.isActive }
    val archived = habits.filter { !it.habit.isActive }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("Habits", style = MaterialTheme.typography.headlineMedium, color = theme.ink)
                Text(
                    "Everything you defined — ${active.size} active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.muted
                )
            }
        }

        if (active.isEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(theme.surface, RoundedCornerShape(16.dp))
                        .clickable(onClick = onAdd)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("No habits yet", style = MaterialTheme.typography.titleMedium, color = theme.ink)
                    Text(
                        "Tap to define the first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.muted
                    )
                }
            }
        }

        items(active, key = { it.habit.id }) { item ->
            HabitCard(item) { onOpen(item.habit.id) }
        }

        if (archived.isNotEmpty()) {
            item { SectionLabel("Archived", Modifier.padding(top = 10.dp)) }
            items(archived, key = { it.habit.id }) { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(theme.surfaceAlt, RoundedCornerShape(12.dp))
                        .padding(13.dp, 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label(item.habit).isolated(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = theme.muted,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Restore",
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.primary,
                        modifier = Modifier.clickable { onUnarchive(item.habit.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitCard(item: DefinedHabit, onOpen: () -> Unit) = Row(
    Modifier
        .fillMaxWidth()
        .background(theme.surface, RoundedCornerShape(12.dp))
        .clickable(onClick = onOpen)
        .padding(13.dp, 11.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label(item.habit).isolated(),
            style = MaterialTheme.typography.bodyLarge,
            color = theme.ink
        )
        Text(
            scheduleLabel(item),
            style = MaterialTheme.typography.bodySmall,
            color = theme.muted
        )
    }
    StatusChip(item)
}

@Composable
private fun StatusChip(item: DefinedHabit) {
    val (text, colour) = when {
        item.doneToday -> "Done" to theme.success
        item.state == DueState.DUE -> "Due today" to theme.primary
        item.state == DueState.OPEN -> "Open" to theme.ink2
        else -> "Resting" to theme.muted
    }
    Pill(colour.copy(alpha = 0.14f)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = colour)
    }
}

private fun label(habit: HabitEntity) =
    listOfNotNull(habit.emoji, habit.name).joinToString(" ")

/** "Every day", "3× per week · 1 done", "Sun Tue Thu". */
private fun scheduleLabel(item: DefinedHabit): String = when (item.habit.scheduleType) {
    ScheduleType.DAILY -> "Every day"
    ScheduleType.TIMES_PER_WEEK ->
        "${item.habit.timesPerWeek}× per week · ${item.doneThisWeek} done"
    ScheduleType.SPECIFIC_DAYS -> weekdaysFrom(item.habit.weekdayMask)
}

private fun weekdaysFrom(mask: Int): String = DayOfWeek.entries
    .filter { mask and (1 shl (it.value - 1)) != 0 }
    .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
