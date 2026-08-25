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
import androidx.compose.ui.res.stringResource
import com.idoelbak.tracker.R
import androidx.compose.ui.unit.dp
import com.idoelbak.tracker.core.model.ScheduleType
import com.idoelbak.tracker.data.db.HabitEntity
import com.idoelbak.tracker.ui.theme.theme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * The definitions, and nothing else.
 *
 * No ticking, no progress, no "due today" -- those belong to Today and to Week. This page is what a
 * habit *is*: its name and how often it is expected. Tap one to change it.
 */
@Composable
fun HabitsScreen(
    habits: List<HabitEntity>,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onUnarchive: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = currentLocale()
    val active = habits.filter { it.isActive }
    val archived = habits.filter { !it.isActive }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text(stringResource(R.string.habits_title), style = MaterialTheme.typography.headlineMedium, color = theme.ink)
                Text(
                    stringResource(R.string.habits_subtitle, active.size),
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
                    Text(stringResource(R.string.habits_empty_title), style = MaterialTheme.typography.titleMedium, color = theme.ink)
                    Text(
                        stringResource(R.string.habits_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.muted
                    )
                }
            }
        }

        items(active, key = { it.id }) { habit ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(theme.surface, RoundedCornerShape(12.dp))
                    .clickable { onOpen(habit.id) }
                    .padding(13.dp, 11.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        label(habit).isolated(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = theme.ink
                    )
                    Text(
                        scheduleLabel(habit, locale),
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.muted
                    )
                }
                Text(stringResource(R.string.habits_edit), style = MaterialTheme.typography.labelMedium, color = theme.primary)
            }
        }

        if (archived.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.habits_archived), Modifier.padding(top = 10.dp)) }
            items(archived, key = { it.id }) { habit ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(theme.surfaceAlt, RoundedCornerShape(12.dp))
                        .padding(13.dp, 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            label(habit).isolated(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = theme.muted
                        )
                        Text(
                            stringResource(R.string.habits_archived_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.muted
                        )
                    }
                    Text(
                        stringResource(R.string.habits_restore),
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.primary,
                        modifier = Modifier.clickable { onUnarchive(habit.id) }
                    )
                }
            }
        }
    }
}

private fun label(habit: HabitEntity) = listOfNotNull(habit.emoji, habit.name).joinToString(" ")

/** "Every day", "3× per week", "Sun Tue Thu". */
@Composable
private fun scheduleLabel(habit: HabitEntity, locale: Locale): String = when (habit.scheduleType) {
    ScheduleType.DAILY -> stringResource(R.string.schedule_daily)
    ScheduleType.TIMES_PER_WEEK -> stringResource(R.string.schedule_times_per_week, habit.timesPerWeek)
    ScheduleType.SPECIFIC_DAYS -> weekdaysFrom(habit.weekdayMask, locale)
}

private fun weekdaysFrom(mask: Int, locale: Locale): String = DayOfWeek.entries
    .filter { mask and (1 shl (it.value - 1)) != 0 }
    .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, locale) }
