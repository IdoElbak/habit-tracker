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
import androidx.compose.ui.res.stringResource
import com.idoelbak.tracker.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.idoelbak.tracker.BuildConfig
import com.idoelbak.tracker.core.engine.DayBoundary
import com.idoelbak.tracker.core.engine.StreakEngine
import com.idoelbak.tracker.data.Settings
import com.idoelbak.tracker.data.ThemeMode
import com.idoelbak.tracker.notify.Reminders
import com.idoelbak.tracker.ui.theme.Palette
import com.idoelbak.tracker.ui.theme.Palettes
import com.idoelbak.tracker.ui.theme.theme
import java.time.DayOfWeek
import java.time.format.TextStyle

private val WeekStartChoices = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY)

private val Languages = listOf(
    "en" to R.string.settings_language_english,
    "iw" to R.string.settings_language_hebrew
)

/**
 * The strings that break bidirectional text when it is done wrong: Hebrew with a trailing question
 * mark, an English word inside Hebrew, grouped digits, parentheses, a sentence ending in the other
 * language. Every one must read correctly in both app languages, which is what `isolated()` and
 * `TextDirection.Content` are for. Debug builds only.
 */
private val BidiCorpus = listOf(
    "האם קראתי היום?",
    "לקרוא 10 pages ביום",
    "ללכת 10,000 צעדים",
    "מדיטציה (5 min) בבוקר",
    "ללמוד Russian",
    "Workout אימון 3x",
    "Read ספר daily!"
)

/** The app language as the user has chosen it, or the system's if they have not. */
@Composable
private fun currentLanguageTag(): String =
    AppCompatDelegate.getApplicationLocales()[0]?.language
        ?: currentLocale().language

private fun setLanguage(tag: String) =
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))

private fun ThemeMode.labelRes() = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_mode_system
    ThemeMode.LIGHT -> R.string.settings_mode_light
    ThemeMode.DARK -> R.string.settings_mode_dark
}

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
    onReminders: (Boolean) -> Unit,
    onBatterySettings: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onRestore: () -> Unit,
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
                stringResource(R.string.settings_done),
                style = MaterialTheme.typography.bodyLarge,
                color = theme.primary,
                modifier = Modifier.clickable(onClick = onBack)
            )
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = theme.ink,
                modifier = Modifier.weight(1f)
            )
        }
    }

    item {
        Section(stringResource(R.string.settings_theme)) {
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
        Section(stringResource(R.string.settings_appearance)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    val on = mode == settings.themeMode
                    Text(
                        stringResource(mode.labelRes()),
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
        Section(stringResource(R.string.settings_reminders)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(theme.surface, RoundedCornerShape(16.dp))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onReminders(!settings.remindersEnabled) }
                        .padding(16.dp, 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.settings_nudge_me), style = MaterialTheme.typography.bodyLarge, color = theme.ink)
                        Text(
                            Reminders.SLOTS.joinToString(" · ") { "%02d:%02d".format(it.hour, it.minute) },
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.muted
                        )
                    }
                    Toggle(settings.remindersEnabled)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(theme.rule))
                Text(
                    stringResource(R.string.settings_reminders_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.muted,
                    modifier = Modifier.padding(16.dp, 12.dp)
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(theme.surfaceAlt, RoundedCornerShape(16.dp))
                    .border(1.dp, theme.rule, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.settings_battery_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.ink
                )
                Text(
                    stringResource(R.string.settings_battery_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.muted
                )
                Text(
                    stringResource(R.string.settings_battery_action),
                    style = MaterialTheme.typography.labelMedium,
                    color = theme.onPrimary,
                    modifier = Modifier
                        .background(theme.primary, CircleShape)
                        .clickable(onClick = onBatterySettings)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }

    item {
        Section(stringResource(R.string.settings_your_day)) {
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
                        stringResource(R.string.settings_week_starts),
                        style = MaterialTheme.typography.bodyLarge,
                        color = theme.ink,
                        modifier = Modifier.weight(1f)
                    )
                    val locale = currentLocale()
                    WeekStartChoices.forEach { day ->
                        val on = day == settings.weekStart
                        Text(
                            day.getDisplayName(TextStyle.SHORT, locale),
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
                        stringResource(R.string.settings_day_ends),
                        style = MaterialTheme.typography.bodyLarge,
                        color = theme.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.settings_day_ends_value, DayBoundary.DEFAULT_ROLLOVER_HOUR),
                        style = MaterialTheme.typography.labelMedium,
                        color = theme.muted
                    )
                }
            }
            Text(
                stringResource(R.string.settings_rollover_note),
                style = MaterialTheme.typography.bodySmall,
                color = theme.muted
            )
        }
    }

    item {
        Section(stringResource(R.string.settings_streaks)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(theme.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    stringResource(R.string.settings_streaks_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.ink
                )
                Text(
                    stringResource(
                        R.string.settings_streaks_body,
                        StreakEngine.ALLOWANCE_MIN_DUE,
                        StreakEngine.MAX_FREEZES,
                        StreakEngine.CLEAN_DAYS_PER_FREEZE
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.muted
                )
            }
        }
    }

    item {
        Section(stringResource(R.string.settings_language)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Languages.forEach { (tag, label) ->
                    val on = tag == currentLanguageTag()
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) theme.onPrimary else theme.ink2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .background(if (on) theme.primary else theme.surface, CircleShape)
                            .clickable { setLanguage(tag) }
                            .padding(vertical = 11.dp)
                    )
                }
            }
        }
    }

    item {
        Section(stringResource(R.string.settings_data)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(theme.surface, RoundedCornerShape(16.dp))
            ) {
                DataRow(
                    title = stringResource(R.string.settings_export_json),
                    note = stringResource(R.string.settings_export_json_note),
                    onClick = onExportJson
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(theme.rule))
                DataRow(
                    title = stringResource(R.string.settings_export_csv),
                    note = stringResource(R.string.settings_export_csv_note),
                    onClick = onExportCsv
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(theme.rule))
                DataRow(
                    title = stringResource(R.string.settings_restore),
                    note = stringResource(R.string.settings_restore_note),
                    onClick = onRestore
                )
            }
            Text(
                stringResource(R.string.settings_data_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = theme.muted
            )
        }
    }

    if (BuildConfig.DEBUG) {
        item {
            Section(stringResource(R.string.settings_bidi_check)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(theme.surface, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.settings_bidi_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.muted
                    )
                    BidiCorpus.forEach { line ->
                        Text(
                            line.isolated(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = theme.ink
                        )
                    }
                }
            }
        }
    }

    item {
        Text(
            stringResource(R.string.settings_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = theme.muted
        )
    }
}

@Composable
private fun DataRow(title: String, note: String, onClick: () -> Unit) = Column(
    Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(16.dp, 14.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp)
) {
    Text(title, style = MaterialTheme.typography.bodyLarge, color = theme.ink)
    Text(note, style = MaterialTheme.typography.bodySmall, color = theme.muted)
}

/** The design's pill switch, drawn rather than pulled from Material so it matches the palette. */
@Composable
private fun Toggle(on: Boolean) = Box(
    Modifier
        .size(46.dp, 27.dp)
        .background(if (on) theme.success else theme.track, CircleShape)
        .padding(3.dp),
    contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
) {
    Box(Modifier.size(21.dp).background(Color.White, CircleShape))
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
