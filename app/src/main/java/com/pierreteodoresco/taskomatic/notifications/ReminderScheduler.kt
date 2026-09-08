package com.pierreteodoresco.taskomatic.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pierreteodoresco.taskomatic.core.ReminderPlan

fun interface ReminderScheduler {
    fun replace(plan: ReminderPlan?)
}

class SystemReminderScheduler(private val context: Context) : ReminderScheduler {
    override fun replace(plan: ReminderPlan?) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND)
        if (plan == null) {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                manager.cancel(it)
                it.cancel()
            }
        } else {
            intent.putExtra(EXPECTED_AT, plan.at.toEpochMilli())
            val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.at.toEpochMilli(), pending)
        }
    }

    companion object {
        const val ACTION_REMIND = "com.pierreteodoresco.taskomatic.REMIND"
        const val EXPECTED_AT = "expected-at"
    }
}
