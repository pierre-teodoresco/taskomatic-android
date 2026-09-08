package com.pierreteodoresco.taskomatic.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pierreteodoresco.taskomatic.MainActivity
import com.pierreteodoresco.taskomatic.R
import com.pierreteodoresco.taskomatic.core.TaskItem
import com.pierreteodoresco.taskomatic.data.AppLanguage
import java.util.Locale
import java.util.UUID

class ReminderNotifications(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun canPost(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        return manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun show(tasks: List<TaskItem>, language: AppLanguage): Boolean {
        if (tasks.isEmpty() || !canPost()) return false
        val item = tasks.singleOrNull()
        val localized = context.withLanguage(language)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, localized.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), FLAGS)
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(if (item != null) R.string.notification_title else R.string.app_name))
            .setContentText(when (tasks.size) {
                1 -> tasks[0].title
                2 -> tasks.joinToString(" · ") { it.title }
                else -> localized.getString(R.string.notification_summary, tasks.size)
            })
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        if (item != null) {
            builder.addExtras(Bundle().apply { putString(TASK_ID, item.id.toString()); putString(CYCLE_ID, item.cycleId.toString()) })
            val action = Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = ACTION_COMPLETE
                data = Uri.Builder().scheme("taskomatic").authority("complete").appendPath(item.id.toString()).appendPath(item.cycleId.toString()).build()
            }
            builder.addAction(R.drawable.ic_notification, localized.getString(R.string.notification_complete),
                PendingIntent.getBroadcast(context, 0, action, FLAGS))
        }
        manager.notify(REMINDER_ID, builder.build())
        return true
    }

    fun cancelIfMatches(id: UUID, cycle: UUID) {
        val current = manager.activeNotifications.singleOrNull { it.id == REMINDER_ID }?.notification ?: return
        if (current.extras.getString(TASK_ID) == id.toString() && current.extras.getString(CYCLE_ID) == cycle.toString()) manager.cancel(REMINDER_ID)
    }

    fun showTest(language: AppLanguage): Boolean {
        if (!canPost()) return false
        val localized = context.withLanguage(language)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, localized.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT))
        manager.notify(TEST_ID, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(R.string.app_name))
            .setContentText(localized.getString(R.string.test_notification_body))
            .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), FLAGS))
            .setAutoCancel(true).build())
        return true
    }

    fun showActionFailure(language: AppLanguage) {
        val localized = context.withLanguage(language)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), FLAGS)
        manager.notify(FAILURE_ID, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(R.string.notification_failure))
            .setContentText(localized.getString(R.string.notification_failure_body))
            .setContentIntent(open).setAutoCancel(true).build())
    }

    companion object {
        const val ACTION_COMPLETE = "com.pierreteodoresco.taskomatic.COMPLETE"
        private const val CHANNEL = "task-reminders"
        private const val REMINDER_ID = 1
        private const val FAILURE_ID = 2
        private const val TEST_ID = 3
        private const val TASK_ID = "task-id"
        private const val CYCLE_ID = "cycle-id"
        private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}

internal fun Context.withLanguage(language: AppLanguage): Context {
    val locale = if (language == AppLanguage.SYSTEM) Resources.getSystem().configuration.locales[0]
        else Locale.forLanguageTag(language.tag)
    return createConfigurationContext(Configuration(resources.configuration).apply { setLocale(locale) })
}
