package com.idoelbak.tracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idoelbak.tracker.core.model.Schedule
import com.idoelbak.tracker.data.DefinedHabit
import com.idoelbak.tracker.data.TodayUi
import com.idoelbak.tracker.data.TrackerRepository
import com.idoelbak.tracker.data.db.TrackerDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * One view model for the whole app. It is three screens over one database -- splitting it per screen
 * would only mean building the same repository three times.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TrackerRepository(TrackerDatabase.get(app))

    /** Which day the app is showing. Re-read on every resume, so a phone left open rolls over. */
    private val date = MutableStateFlow(repo.today())

    val today: StateFlow<TodayUi> = date
        .flatMapLatest { repo.observeToday(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUi(date.value))

    val habits: StateFlow<List<DefinedHabit>> = date
        .flatMapLatest { repo.observeDefined(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Settle whatever finished while the app was away, then show the day we are actually in. */
    fun refresh() = viewModelScope.launch {
        repo.settle()
        date.value = repo.today()
    }

    fun toggle(habitId: Long) = viewModelScope.launch { repo.toggle(habitId, date.value) }

    fun rate(mood: Int?, motivation: Int?) = viewModelScope.launch {
        repo.rate(date.value, mood, motivation)
    }

    fun save(id: Long?, name: String, emoji: String?, schedule: Schedule) = viewModelScope.launch {
        repo.saveHabit(id, name, emoji, schedule)
    }

    fun archive(id: Long) = viewModelScope.launch { repo.archive(id) }

    fun unarchive(id: Long) = viewModelScope.launch { repo.unarchive(id) }

    fun currentDate(): LocalDate = date.value
}
