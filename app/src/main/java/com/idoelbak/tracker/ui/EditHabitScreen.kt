package com.idoelbak.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.idoelbak.tracker.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idoelbak.tracker.core.model.Schedule
import com.idoelbak.tracker.core.model.ScheduleType
import com.idoelbak.tracker.core.model.bit
import com.idoelbak.tracker.data.db.HabitEntity
import com.idoelbak.tracker.ui.theme.theme
import java.time.format.TextStyle
import java.time.DayOfWeek

/** A small set that covers most habits; anything else goes in with the keyboard. */
private val CommonEmoji = listOf(
    "🏋️", "🚶", "😴", "📖", "🧠", "🧘", "🚿", "💧", "🥗", "✍️", "🎨", "🎧", "💊", "☎️"
)

/** Sunday first, matching the week start. */
private val WeekDays = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
)

/**
 * Add or edit one habit. Editing changes what is expected from today onward -- it never touches a
 * tick already banked or a day already settled.
 */
@Composable
fun EditHabitScreen(
    existing: HabitEntity?,
    onSave: (String, String?, Schedule) -> Unit,
    onArchive: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var emoji by remember(existing) { mutableStateOf(existing?.emoji) }
    var type by remember(existing) { mutableStateOf(existing?.scheduleType ?: ScheduleType.DAILY) }
    var times by remember(existing) { mutableIntStateOf(existing?.timesPerWeek?.takeIf { it > 0 } ?: 3) }
    var mask by remember(existing) { mutableIntStateOf(existing?.weekdayMask ?: 0) }
    val locale = currentLocale()

    val valid = name.isNotBlank() && (type != ScheduleType.SPECIFIC_DAYS || mask != 0)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.edit_cancel),
                    style = MaterialTheme.typography.bodyLarge,
                    color = theme.muted,
                    modifier = Modifier.clickable(onClick = onCancel)
                )
                Text(
                    if (existing == null) stringResource(R.string.edit_new_habit)
                    else stringResource(R.string.edit_edit_habit),
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.ink
                )
                Text(
                    stringResource(R.string.edit_save),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (valid) theme.primary else theme.muted,
                    modifier = Modifier.clickable(enabled = valid) {
                        onSave(name, emoji, scheduleOf(type, times, mask))
                    }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.edit_name))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = theme.surface,
                        unfocusedContainerColor = theme.surface,
                        focusedBorderColor = theme.primary,
                        unfocusedBorderColor = theme.rule,
                        cursorColor = theme.primary,
                        focusedTextColor = theme.ink,
                        unfocusedTextColor = theme.ink
                    )
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.edit_icon))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EmojiChip(null, emoji == null) { emoji = null }
                    CommonEmoji.take(6).forEach { candidate ->
                        EmojiChip(candidate, emoji == candidate) { emoji = candidate }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CommonEmoji.drop(6).take(7).forEach { candidate ->
                        EmojiChip(candidate, emoji == candidate) { emoji = candidate }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.edit_how_often))

                Option(
                    title = stringResource(R.string.edit_every_day),
                    subtitle = stringResource(R.string.edit_every_day_note),
                    selected = type == ScheduleType.DAILY,
                    onSelect = { type = ScheduleType.DAILY }
                )

                Option(
                    title = stringResource(R.string.edit_times_per_week),
                    subtitle = stringResource(R.string.edit_times_per_week_note),
                    selected = type == ScheduleType.TIMES_PER_WEEK,
                    onSelect = { type = ScheduleType.TIMES_PER_WEEK }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Stepper("−") { if (times > 1) times-- }
                        Text(
                            "$times",
                            style = MaterialTheme.typography.labelLarge,
                            color = theme.ink
                        )
                        Stepper("+") { if (times < 7) times++ }
                    }
                }

                Option(
                    title = stringResource(R.string.edit_specific_days),
                    subtitle = stringResource(R.string.edit_specific_days_note),
                    selected = type == ScheduleType.SPECIFIC_DAYS,
                    onSelect = { type = ScheduleType.SPECIFIC_DAYS }
                )

                if (type == ScheduleType.SPECIFIC_DAYS) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WeekDays.forEach { day ->
                            val on = mask and day.bit() != 0
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(
                                        if (on) theme.primary else theme.surfaceAlt,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { mask = mask xor day.bit() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.getDisplayName(TextStyle.NARROW, locale),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (on) theme.onPrimary else theme.muted
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                stringResource(R.string.edit_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = theme.muted
            )
        }

        if (existing != null) {
            item {
                Text(
                    stringResource(R.string.edit_archive),
                    style = MaterialTheme.typography.bodyLarge,
                    color = theme.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.surfaceAlt, RoundedCornerShape(14.dp))
                        .clickable(onClick = onArchive)
                        .padding(vertical = 14.dp)
                )
            }
        }
    }
}

private fun scheduleOf(type: ScheduleType, times: Int, mask: Int) = when (type) {
    ScheduleType.DAILY -> Schedule.daily()
    ScheduleType.TIMES_PER_WEEK -> Schedule.timesPerWeek(times)
    ScheduleType.SPECIFIC_DAYS -> Schedule(ScheduleType.SPECIFIC_DAYS, weekdayMask = mask)
}

@Composable
private fun EmojiChip(value: String?, selected: Boolean, onPick: () -> Unit) = Box(
    Modifier
        .size(40.dp)
        .background(if (selected) theme.primary else theme.surface, RoundedCornerShape(12.dp))
        .border(1.dp, if (selected) theme.primary else theme.rule, RoundedCornerShape(12.dp))
        .clickable(onClick = onPick),
    contentAlignment = Alignment.Center
) {
    Text(
        value ?: stringResource(R.string.edit_no_icon),
        style = MaterialTheme.typography.bodyLarge,
        color = if (selected && value == null) theme.onPrimary else theme.ink
    )
}

@Composable
private fun Option(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) = Row(
    Modifier
        .fillMaxWidth()
        .background(theme.surface, RoundedCornerShape(14.dp))
        .clickable(onClick = onSelect)
        .padding(16.dp, 14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Box(
        Modifier
            .size(20.dp)
            .border(2.dp, if (selected) theme.primary else theme.track, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Box(Modifier.size(10.dp).background(theme.primary, CircleShape))
    }
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = theme.ink)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = theme.muted)
    }
    trailing?.invoke()
}

@Composable
private fun Stepper(symbol: String, onClick: () -> Unit) = Box(
    Modifier
        .size(32.dp)
        .background(theme.surfaceAlt, CircleShape)
        .border(1.dp, theme.rule, CircleShape)
        .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
) {
    Text(symbol, style = MaterialTheme.typography.bodyLarge, color = theme.ink2)
}
