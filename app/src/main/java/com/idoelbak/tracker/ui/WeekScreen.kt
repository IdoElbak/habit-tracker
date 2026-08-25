package com.idoelbak.tracker.ui

import androidx.compose.foundation.background
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
import com.idoelbak.tracker.data.WeekRow
import com.idoelbak.tracker.data.WeekUi
import com.idoelbak.tracker.ui.theme.theme
import java.time.format.DateTimeFormatter

private val DayMonth = DateTimeFormatter.ofPattern("d MMM")

/**
 * The week, habit by habit.
 *
 * Every habit is measured against its own full week -- 7 for a daily one, the quota for a weekly
 * one, the chosen days for a weekday one -- so "3 of 3" and "5 of 7" both read as a finished week
 * and a rest day never looks like a failure. This is the spreadsheet view the app replaces.
 */
@Composable
fun WeekScreen(ui: WeekUi, modifier: Modifier = Modifier) = LazyColumn(
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 28.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
) {
    item {
        Column {
            Text("This week", style = MaterialTheme.typography.headlineMedium, color = theme.ink)
            Text(
                "${ui.from.format(DayMonth)} – ${ui.to.format(DayMonth)}",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.muted
            )
        }
    }

    item {
        Row(
            Modifier
                .fillMaxWidth()
                .background(theme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp, 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(ui.done, ui.goal)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "${ui.done} of ${ui.goal} sessions",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.ink
                )
                Text(
                    if (ui.goal == 0) "Nothing scheduled this week"
                    else "${(ui.done * 100) / ui.goal}% of the week's plan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.muted
                )
            }
        }
    }

    if (ui.rows.isNotEmpty()) {
        item { SectionLabel("Every habit") }
    }

    items(ui.rows, key = { it.id }) { row -> WeekCard(row) }
}

@Composable
private fun WeekCard(row: WeekRow) = Column(
    Modifier
        .fillMaxWidth()
        .background(theme.surface, RoundedCornerShape(12.dp))
        .padding(13.dp, 12.dp),
    verticalArrangement = Arrangement.spacedBy(9.dp)
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            listOfNotNull(row.emoji, row.name).joinToString(" ").isolated(),
            style = MaterialTheme.typography.bodyLarge,
            color = theme.ink,
            modifier = Modifier.weight(1f)
        )
        WeekStrip(row.week)
        Text(
            "${row.done} of ${row.goal}",
            style = MaterialTheme.typography.labelMedium,
            color = if (row.done >= row.goal) theme.success else theme.muted
        )
    }
    ProgressBar(row.fraction)
}
