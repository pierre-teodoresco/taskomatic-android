package com.pierreteodoresco.taskomatic.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pierreteodoresco.taskomatic.TaskomaticApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderNotifications.ACTION_COMPLETE || intent.data?.authority != "complete") return
        val parts = intent.data?.pathSegments ?: return
        if (parts.size != 2) return
        val id = runCatching { UUID.fromString(parts[0]) }.getOrNull() ?: return
        val cycle = runCatching { UUID.fromString(parts[1]) }.getOrNull() ?: return
        val pending = goAsync()
        val app = context.applicationContext as TaskomaticApplication
        app.backgroundScope.launch {
            try {
                app.store.refresh()
                app.store.completeFromNotification(id, cycle)
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                app.store.reportBackgroundFailure()
                // The saved task is unchanged. Notification failures must not crash this receiver.
                runCatching { ReminderNotifications(context).showActionFailure(app.store.preferences.value.language) }
            }
            finally { pending.finish() }
        }
    }
}
