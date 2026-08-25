package com.idoelbak.tracker.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Where an alarm lands, and where reminders come back to life after a reboot.
 *
 * Without the boot half of this, every reminder stops silently the first time the phone restarts --
 * the single most common way habit apps quietly stop working.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        // The receiver is exported so the system can deliver BOOT_COMPLETED; anything else it is
        // handed is ignored rather than acted on.
        if (action !in HANDLED) return

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                Reminders.ensureChannels(context)
                if (action == Reminders.ACTION_FIRE) {
                    Reminders.fire(context, intent.getIntExtra(Reminders.EXTRA_SLOT, 0))
                }
                // Whatever brought us here, the next slot has to be booked before we go.
                Reminders.scheduleNext(context)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED = setOf(
            Reminders.ACTION_FIRE,
            Reminders.ACTION_RESCHEDULE,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON"
        )
    }
}
