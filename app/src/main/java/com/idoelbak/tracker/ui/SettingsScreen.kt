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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idoelbak.tracker.core.engine.DayBoundary
import com.idoelbak.tracker.core.engine.StreakEngine
import com.idoelbak.tracker.data.Settings
import com.idoelbak.tracker.data.ThemeMode
import com.idoelbak.tracker.ui.theme.Palette
import com.idoelbak.tracker.ui.theme.Palettes
import com.idoelbak.tracker.ui.theme.theme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val WeekStartChoices = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY)

/**
 * The choices that change how the whole app behaves. Everything here is stored in DataStore and read
 * back before the first frame, so a palette survives a restart without a flash of the default.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onPalette: (String) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onWeekStart: (DayOfWeek) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) = LazyColumn(
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 28.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
) {
    item {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Done",
                style = MaterialTheme.typography.bodyLarge,
                color = theme.primary,
                modifier = Modifier.clickable(onClick = onBack)
            )
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = theme.ink,
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Section("Theme") {
            Palettes.all.chunked(2).forEach { pair ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { palette ->
                        PaletteCard(
                            palette = palette,
                            selected = palette.id == settings.paletteId,
                            modifier = Modifier.weight(1f)
                        ) { onPalette(palette.id) }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }

    item {
        Section("Appearance") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    val on = mode == settings.themeMode
                    Text(
                        mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) theme.onPrimary else theme.ink2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .background(if (on) theme.primary else theme.surface, CircleShape)
                            .clickable { onThemeMode(mode) }
                            .padding(vertical = 11.dp)
                    )
                }
            }
        }
    }

    item {
        Section("Your day") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(theme.surface, RoundedCornerShape(16.dp))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Week starts",
                        style = MaterialTheme.typography.bodyLarge,
                        color = theme.ink,
                        modifier = Modifier.weight(1f)
                    )
                    WeekStartChoices.forEach { day ->
                        val on = day == settings.weekStart
                        Text(
                            day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (on) theme.onPrimary else theme.ink2,
                            modifier = Modifier
                                .background(if (on) theme.primary else theme.surfaceAlt, CircleShape)
                                .clickable { onWeekStart(day) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(theme.rule))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Day ends at",
                        style = MaterialTheme.typography.bodyLarge,
                        color = theme.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "%02d:00".format(DayBoundary.DEFAULT_ROLLOVER_HOUR),
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.muted
                    )
                }
            }
            Text(
                "A tick at 01:30 still counts for the day before.",
                style = MaterialTheme.typography.bodySmall,
                color = theme.muted
            )
        }
    }

    item {
        Section("Streaks") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(theme.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Miss one, the day still counts",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.ink
                )
                Text(
                    "From ${StreakEngine.ALLOWANCE_MIN_DUE} habits due upward, one miss is forgiven — " +
                        "on a lighter day everything is required. You hold up to " +
                        "${StreakEngine.MAX_FREEZES} freezes, spent automatically on a bad day, and " +
                        "${StreakEngine.CLEAN_DAYS_PER_FREEZE} days that counted earn one back. " +
                        "There is no switch for this: it is the deal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.muted
                )
            }
        }
    }

    item {
        Text(
            "Everything stays on this phone. The app has no internet permission at all, so it could " +
                "not upload your habits even if it wanted to.",
            style = MaterialTheme.typography.bodySmall,
            color = theme.muted
        )
    }
}

@Composable
private fun Section(label: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) =
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(label)
        content()
    }

@Composable
private fun PaletteCard(
    palette: Palette,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onPick: () -> Unit
) = Column(
    modifier
        .background(theme.surface, RoundedCornerShape(13.dp))
        .border(1.5.dp, if (selected) theme.primary else Color.Transparent, RoundedCornerShape(13.dp))
        .clickable(onClick = onPick)
        .padding(12.dp, 11.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(palette.light.primary, palette.light.success, palette.light.background).forEach {
            Box(Modifier.size(13.dp).background(it, RoundedCornerShape(4.dp)))
        }
    }
    Text(palette.name, style = MaterialTheme.typography.bodySmall, color = theme.ink2)
}
