package com.idoelbak.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idoelbak.tracker.R
import com.idoelbak.tracker.data.Backfill
import com.idoelbak.tracker.data.DayDot
import com.idoelbak.tracker.data.WeekRow
import com.idoelbak.tracker.data.WeekUi
import com.idoelbak.tracker.ui.theme.theme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val DayMonth = DateTimeFormatter.ofPattern("d MMM")

/**
 * The week, habit by habit -- and the one place the record can be corrected.
 *
 * Every habit is measured against its own full week (7 for a daily one, the quota for a weekly one,
 * the chosen days for a weekday one) so "3 of 3" and "5 of 7" both read as a finished week and a
 * rest day never looks like a failure. This is the spreadsheet this app replaces, with the same
 * seven boxes per row -- except here they are tappable, so a forgotten tick can be fixed.
 */
@Composable
fun WeekScreen(
    ui: WeekUi,
    onToggleDay: (Long, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = currentLocale()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text(
                    stringResource(R.string.week_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = theme.ink
                )
                Text(
                    stringResource(R.string.week_range, ui.from.format(DayMonth), ui.to.format(DayMonth)),
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
                        stringResource(R.string.week_sessions, ui.done, ui.goal),
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.ink
                    )
                    Text(
                        if (ui.goal == 0) stringResource(R.string.week_nothing)
                        else stringResource(R.string.week_percent, (ui.done * 100) / ui.goal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.muted
                    )
                }
            }
        }

        if (ui.rows.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.week_every_habit))
                    // The weekday header sits outside the cards but on the same grid, so the
                    // letters line up with the columns below them.
                    Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp)) {
                        (0..6).forEach { offset ->
                            Text(
                                ui.from.plusDays(offset.toLong()).dayOfWeek
                                    .getDisplayName(TextStyle.NARROW, locale),
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        items(ui.rows, key = { it.id }) { row ->
            WeekCard(row, ui) { date -> onToggleDay(row.id, date) }
        }

        if (ui.rows.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.week_backfill_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.muted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun WeekCard(row: WeekRow, ui: WeekUi, onToggleDay: (LocalDate) -> Unit) = Column(
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
        Text(
            stringResource(R.string.count_of, row.done, row.goal),
            style = MaterialTheme.typography.labelMedium,
            color = if (row.done >= row.goal) theme.success else theme.muted
        )
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        row.week.forEachIndexed { offset, dot ->
            val date = ui.from.plusDays(offset.toLong())
            DayCell(dot, Backfill.canEdit(date, ui.today), Modifier.weight(1f)) { onToggleDay(date) }
        }
    }
}

/**
 * One box in the grid. Filled when it was done, a quiet outline when it was due and missed, barely
 * there when the habit was not expected -- a rest day is not a failure and must not look like one.
 */
@Composable
private fun RowScope.DayCell(
    dot: DayDot,
    editable: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    val done = dot == DayDot.HIT
    val fill = when {
        done -> theme.success
        dot == DayDot.MISS -> Color.Transparent
        else -> theme.surfaceAlt
    }
    val outline = when {
        done -> Color.Transparent
        dot == DayDot.MISS -> theme.track
        dot == DayDot.TODAY -> theme.primary
        else -> Color.Transparent
    }

    Box(
        modifier
            .aspectRatio(1f)
            .background(fill, RoundedCornerShape(9.dp))
            .border(if (outline == Color.Transparent) 0.dp else 1.6.dp, outline, RoundedCornerShape(9.dp))
            .then(if (editable) Modifier.clickable(onClick = onToggle) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (done) Glyph(Glyphs.CHECK, 14.dp, theme.onPrimary, strokeWidth = 2.4f, viewport = 16f)
    }
}
