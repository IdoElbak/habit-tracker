package com.idoelbak.tracker.notify

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.idoelbak.tracker.MainActivity
import com.idoelbak.tracker.R
import com.idoelbak.tracker.core.engine.DueCalculator
import com.idoelbak.tracker.core.engine.StreakEngine
import com.idoelbak.tracker.core.engine.StreakState
import com.idoelbak.tracker.data.Prefs
import com.idoelbak.tracker.data.TodayUi
import com.idoelbak.tracker.data.TrackerRepository
import com.idoelbak.tracker.data.db.TrackerDatabase
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The nudges.
 *
 * Six moments in a day, scheduled one at a time: each alarm, when it fires, books the next. That is
 * cheaper than holding six pending intents and it survives the app being killed, because the alarm
 * lives in the system rather than in the process.
 *
 * The alarms are deliberately **inexact** ([AlarmManager.setWindow], ten-minute window). A habit
 * reminder does not need second accuracy, and asking for exact alarms costs a permission that
 * Samsung's One UI has a well-known bug around -- deny it once and the setting disappears entirely.
 */
object Reminders {

    /** Quiet, everyday nudges. */
    const val CHANNEL_NUDGE = "nudges"

    /** The end-of-day countdown and the streak-save alert. Loud on purpose. */
    const val CHANNEL_URGENT = "urgent"

    const val ACTION_FIRE = "com.idoelbak.tracker.FIRE"
    const val ACTION_RESCHEDULE = "com.idoelbak.tracker.RESCHEDULE"
    const val EXTRA_SLOT = "slot"

    private const val ID_PLAN = 1
    private const val ID_PROGRESS = 2
    private const val ID_COUNTDOWN = 3
    private const val ID_STREAK = 4

    private const val WINDOW_MINUTES = 10L

    /** When the day's urgency begins, and when the last warning goes out. */
    private val PLAN = LocalTime.of(8, 0)
    private val COUNTDOWN_FROM = LocalTime.of(22, 0)
    private val STREAK_SAVE = LocalTime.of(22, 45)

    /** In order. The index is what an alarm carries as its slot. */
    val SLOTS: List<LocalTime> = listOf(
        PLAN,
        LocalTime.of(12, 30),
        LocalTime.of(15, 30),
        LocalTime.of(18, 30),
        COUNTDOWN_FROM,
        STREAK_SAVE
    )

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_NUDGE, "Daily nudges", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "What is left to do today."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_URGENT, "End of day", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "The countdown, and the warning when a streak is about to break."
            }
        )
    }

    /** Books the next slot after [from]. Safe to call as often as you like -- it replaces itself. */
    fun scheduleNext(context: Context, from: LocalDateTime = LocalDateTime.now()) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        val slot = SLOTS.indexOfFirst { it.isAfter(from.toLocalTime()) }
        val at = if (slot >= 0) {
            from.toLocalDate().atTime(SLOTS[slot])
        } else {
            from.toLocalDate().plusDays(1).atTime(SLOTS.first())
        }
        val index = if (slot >= 0) slot else 0

        val trigger = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setWindow(
            AlarmManager.RTC_WAKEUP,
            trigger - Duration.ofMinutes(WINDOW_MINUTES / 2).toMillis(),
            Duration.ofMinutes(WINDOW_MINUTES).toMillis(),
            fireIntent(context, index)
        )
    }

    fun cancel(context: Context) {
        val alarms = context.getSystemService<AlarmManager>() ?: return
        SLOTS.indices.forEach { alarms.cancel(fireIntent(context, it)) }
        NotificationManagerCompat.from(context).apply {
            cancel(ID_PLAN); cancel(ID_PROGRESS); cancel(ID_COUNTDOWN); cancel(ID_STREAK)
        }
    }

    /** What one slot actually does when it arrives. */
    suspend fun fire(context: Context, slot: Int) {
        val prefs = Prefs(context).flow.first()
        if (!prefs.remindersEnabled) return cancel(context)

        val repo = TrackerRepository(TrackerDatabase.get(context))
        repo.settle(prefs.weekStart)
        val date = repo.today()
        val ui = repo.observeToday(date, prefs.weekStart).first()
        val habits = TrackerDatabase.get(context).habits().activeHabits()

        // Once the day is done the app says one thing and then shuts up. Nagging someone who has
        // already finished is the fastest way to get an app uninstalled.
        if (ui.dueCount == 0 || ui.left == 0) {
            NotificationManagerCompat.from(context).cancel(ID_COUNTDOWN)
            return
        }

        when (SLOTS.getOrNull(slot)) {
            PLAN -> {
                val escalated = habits.count { habit ->
                    DueCalculator.justEscalated(habit.schedule, date, prefs.weekStart, 0)
                }
                post(
                    context, ID_PLAN, CHANNEL_NUDGE,
                    "${ui.dueCount} today",
                    buildString {
                        append(ui.due.joinToString(", ") { it.name })
                        if (escalated > 0) append(" — and a weekly one runs out of days")
                    }
                )
            }

            COUNTDOWN_FROM -> countdown(context, ui)

            STREAK_SAVE -> {
                val state = StreakState(currentStreak = ui.streak, freezes = ui.freezes)
                if (StreakEngine.streakAtRisk(state, ui.dueCount, ui.doneCount)) {
                    post(
                        context, ID_STREAK, CHANNEL_URGENT,
                        "Your ${ui.streak}-day streak is about to break",
                        "${ui.left} left. Finishing now keeps it.",
                        urgent = true
                    )
                }
            }

            else -> post(
                context, ID_PROGRESS, CHANNEL_NUDGE,
                "${ui.doneCount} of ${ui.dueCount} done",
                ui.due.filterNot { it.done }.joinToString(", ") { it.name }
            )
        }
    }

    /**
     * Called after a tick: the ongoing countdown has to reflect what is actually left, and vanish
     * the moment the day is finished.
     */
    suspend fun refresh(context: Context) {
        val prefs = Prefs(context).flow.first()
        val manager = NotificationManagerCompat.from(context)
        if (!prefs.remindersEnabled) return

        val repo = TrackerRepository(TrackerDatabase.get(context))
        val date = repo.today()
        val now = LocalDateTime.now()
        val ui = repo.observeToday(date, prefs.weekStart).first()

        val inCountdownWindow = now.toLocalTime().isAfter(COUNTDOWN_FROM)
        if (!inCountdownWindow || ui.left == 0 || ui.dueCount == 0) {
            manager.cancel(ID_COUNTDOWN)
            if (ui.dueCount > 0 && ui.left == 0) manager.cancel(ID_STREAK)
            return
        }
        countdown(context, ui)
    }

    /**
     * The one red thing in the whole app. Ongoing, colorised, and counting down live -- the
     * chronometer ticks by itself, so this costs one notification rather than an alarm a minute.
     */
    private fun countdown(context: Context, ui: TodayUi) {
        val midnight = ui.date.plusDays(1).atStartOfDay()
        post(
            context = context,
            id = ID_COUNTDOWN,
            channel = CHANNEL_URGENT,
            title = "${ui.left} left today",
            text = ui.due.filterNot { it.done }.joinToString(", ") { it.name },
            urgent = true
        ) {
            setOngoing(true)
            setColorized(true)
            setColor(ContextCompat.getColor(context, R.color.notification_urgent))
            setUsesChronometer(true)
            setChronometerCountDown(true)
            setWhen(midnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        }
    }

    // canPost() below is the permission check; lint cannot follow it across the helper.
    @SuppressLint("MissingPermission")
    private fun post(
        context: Context,
        id: Int,
        channel: String,
        title: String,
        text: String,
        urgent: Boolean = false,
        extra: NotificationCompat.Builder.() -> Unit = {}
    ) {
        if (!canPost(context)) return
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_tracker)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context))
            .setAutoCancel(!urgent)
            .setPriority(if (urgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .apply(extra)
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    /** On Android 13+ the user can simply have said no; posting anyway would throw. */
    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun fireIntent(context: Context, slot: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        slot,
        Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_SLOT, slot),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
