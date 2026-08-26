package com.idoelbak.tracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idoelbak.tracker.core.model.Schedule
import com.idoelbak.tracker.data.Backfill
import com.idoelbak.tracker.data.Backup
import com.idoelbak.tracker.data.BackupSummary
import com.idoelbak.tracker.data.Backups
import com.idoelbak.tracker.data.Prefs
import com.idoelbak.tracker.data.Settings
import com.idoelbak.tracker.data.StatsPeriod
import com.idoelbak.tracker.data.StatsUi
import com.idoelbak.tracker.data.TodayUi
import com.idoelbak.tracker.data.WeekUi
import com.idoelbak.tracker.data.db.HabitEntity
import com.idoelbak.tracker.data.ThemeMode
import com.idoelbak.tracker.data.TrackerRepository
import com.idoelbak.tracker.data.db.TrackerDatabase
import androidx.glance.appwidget.updateAll
import com.idoelbak.tracker.notify.Reminders
import com.idoelbak.tracker.widget.TrackerWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * One view model for the whole app. It is three screens over one database -- splitting it per screen
 * would only mean building the same repository three times.
 */
/** What to tell the user after a backup or a restore. The UI turns these into sentences. */
enum class BackupMessage { Exported, ExportFailed, Restored, RestoreFailed }

@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TrackerRepository(TrackerDatabase.get(app))
    private val prefs = Prefs(app)

    val settings: StateFlow<Settings> = prefs.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    /** Which day the app is showing. Re-read on every resume, so a phone left open rolls over. */
    private val date = MutableStateFlow(repo.today())

    /** The day and the week start together decide what is due; every screen reads both. */
    private val day = combine(date, prefs.flow) { date, settings -> date to settings.weekStart }

    val today: StateFlow<TodayUi> = day
        .flatMapLatest { (date, weekStart) -> repo.observeToday(date, weekStart) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUi(date.value))

    val week: StateFlow<WeekUi> = day
        .flatMapLatest { (date, weekStart) -> repo.observeWeek(date, weekStart) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            WeekUi(date.value, date.value.plusDays(6))
        )

    val habits: StateFlow<List<HabitEntity>> = repo.observeAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val period = MutableStateFlow(StatsPeriod.MONTH)

    /** A backup that has been read and checked, waiting for the user to say yes. */
    private val pending = MutableStateFlow<Backup?>(null)

    val pendingRestore: StateFlow<BackupSummary?> = pending
        .map { it?.let(Backups::summarise) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _backupMessage = MutableStateFlow<BackupMessage?>(null)
    val backupMessage: StateFlow<BackupMessage?> = _backupMessage

    val stats: StateFlow<StatsUi> = combine(day, period) { (date, weekStart), chosen ->
        Triple(date, weekStart, chosen)
    }
        .flatMapLatest { (date, weekStart, chosen) -> repo.observeStats(date, weekStart, chosen) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUi())

    fun pickPeriod(chosen: StatsPeriod) { period.value = chosen }

    /** Settle whatever finished while the app was away, then show the day we are actually in. */
    fun refresh() = viewModelScope.launch {
        repo.settle(settings.value.weekStart)
        date.value = repo.today()
        val app = getApplication<Application>()
        Reminders.ensureChannels(app)
        if (settings.value.remindersEnabled) Reminders.scheduleNext(app)
    }

    /** Correct a forgotten tick on an earlier day. Ignored outside the back-fill window. */
    fun toggleOn(habitId: Long, day: LocalDate) = viewModelScope.launch {
        if (!Backfill.canEdit(day, date.value)) return@launch
        repo.toggle(habitId, day)
        Reminders.refresh(getApplication())
        TrackerWidget().updateAll(getApplication())
    }

    fun toggle(habitId: Long) = viewModelScope.launch {
        repo.toggle(habitId, date.value)
        // The evening countdown has to shrink as things get ticked, and disappear when the day is
        // done; the widget has to agree with the app the instant you look at the home screen.
        Reminders.refresh(getApplication())
        TrackerWidget().updateAll(getApplication())
    }

    fun rate(mood: Int?, motivation: Int?) = viewModelScope.launch {
        repo.rate(date.value, mood, motivation)
    }

    fun save(id: Long?, name: String, emoji: String?, schedule: Schedule) = viewModelScope.launch {
        repo.saveHabit(id, name, emoji, schedule)
    }

    fun archive(id: Long) = viewModelScope.launch { repo.archive(id) }

    fun unarchive(id: Long) = viewModelScope.launch { repo.unarchive(id) }

    fun setPalette(id: String) = viewModelScope.launch { prefs.setPalette(id) }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(mode) }

    fun setWeekStart(day: java.time.DayOfWeek) = viewModelScope.launch { prefs.setWeekStart(day) }

    fun setReminders(on: Boolean) = viewModelScope.launch {
        prefs.setReminders(on)
        val app = getApplication<Application>()
        if (on) Reminders.scheduleNext(app) else Reminders.cancel(app)
    }

    /**
     * Sends the user to the battery-optimisation exemption dialog. On Samsung this is the difference
     * between reminders that fire and reminders that silently stop after a few quiet days.
     */
    fun requestBatteryExemption() {
        val app = getApplication<Application>()
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            android.net.Uri.parse("package:" + app.packageName)
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { app.startActivity(intent) }.onFailure {
            // Some OEM builds hide the dialog; the app-details page always exists.
            app.startActivity(
                android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + app.packageName)
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * Writes a backup to wherever the user pointed the file picker. Everything goes through the
     * Storage Access Framework, so the app needs no storage permission at all.
     */
    fun exportTo(uri: android.net.Uri, csv: Boolean) = viewModelScope.launch {
        val app = getApplication<Application>()
        val db = TrackerDatabase.get(app)
        val text = if (csv) Backups.toCsv(db) else Backups.toJson(db, System.currentTimeMillis())
        runCatching {
            app.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        }.onFailure { _backupMessage.value = BackupMessage.ExportFailed }
            .onSuccess { _backupMessage.value = BackupMessage.Exported }
    }

    /** Reads and checks a backup, but changes nothing yet -- the user still has to confirm. */
    fun previewRestore(uri: android.net.Uri) = viewModelScope.launch {
        val app = getApplication<Application>()
        val text = runCatching {
            app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()

        if (text == null) {
            _backupMessage.value = BackupMessage.RestoreFailed
            return@launch
        }
        Backups.read(text)
            .onSuccess { pending.value = it }
            .onFailure { _backupMessage.value = BackupMessage.RestoreFailed }
    }

    fun cancelRestore() { pending.value = null }

    fun confirmRestore() = viewModelScope.launch {
        val backup = pending.value ?: return@launch
        pending.value = null
        val app = getApplication<Application>()
        runCatching { Backups.restore(TrackerDatabase.get(app), backup) }
            .onFailure { _backupMessage.value = BackupMessage.RestoreFailed }
            .onSuccess {
                _backupMessage.value = BackupMessage.Restored
                refresh()
                TrackerWidget().updateAll(app)
            }
    }

    fun clearBackupMessage() { _backupMessage.value = null }

    fun currentDate(): LocalDate = date.value
}
