package com.pierreteodoresco.taskomatic.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pierreteodoresco.taskomatic.TaskomaticApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(SystemReminderScheduler.ACTION_REMIND, Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending = goAsync()
        val app = context.applicationContext as TaskomaticApplication
        app.backgroundScope.launch {
            try {
                if (intent.action == SystemReminderScheduler.ACTION_REMIND && intent.hasExtra(SystemReminderScheduler.EXPECTED_AT)) {
                    app.store.deliverReminder(Instant.ofEpochMilli(intent.getLongExtra(SystemReminderScheduler.EXPECTED_AT, 0)))
                } else app.store.refresh()
            }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { app.store.reportBackgroundFailure() }
            finally { pending.finish() }
        }
    }
}
