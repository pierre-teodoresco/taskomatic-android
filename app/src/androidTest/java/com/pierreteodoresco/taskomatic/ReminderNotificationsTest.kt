package com.pierreteodoresco.taskomatic

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.pierreteodoresco.taskomatic.data.AppLanguage
import com.pierreteodoresco.taskomatic.data.AppPreferences
import com.pierreteodoresco.taskomatic.data.PreferencesRepository
import com.pierreteodoresco.taskomatic.core.ReminderSettings
import com.pierreteodoresco.taskomatic.notifications.ReminderReceiver
import com.pierreteodoresco.taskomatic.notifications.SystemReminderScheduler
import java.time.ZonedDateTime
import com.pierreteodoresco.taskomatic.notifications.ReminderNotifications
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderNotificationsTest {
    @get:Rule val permission: GrantPermissionRule = GrantPermissionRule.grant(
        *if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray())
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = (context.applicationContext as TaskomaticApplication).store
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Before fun reset() = runBlocking {
        assumeTrue("Use an isolated emulator", Build.MODEL.contains("sdk", ignoreCase = true))
        store.refresh()
        store.tasks.value.forEach { store.delete(it) }
        manager.cancelAll()
    }
    @After fun cleanup() = runBlocking {
        manager.cancelAll()
        store.dismissBackgroundFailure()
        PreferencesRepository(context).use { it.save(AppPreferences()) }
        store.refresh()
    }

    @Test fun theAlarmReceiverLoadsFreshDataAndPostsALocalReminder() = runBlocking {
        store.add("Fresh alarm task")
        val expected = ZonedDateTime.now().withSecond(0).withNano(0)
        PreferencesRepository(context).use { it.save(AppPreferences(language = AppLanguage.ENGLISH,
            reminders = ReminderSettings(true, expected.toLocalTime()))) }
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(SystemReminderScheduler.ACTION_REMIND)
            .putExtra(SystemReminderScheduler.EXPECTED_AT, expected.toInstant().toEpochMilli())
        PendingIntent.getBroadcast(context, 190, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE).send()
        withTimeout(5_000) { while (manager.activeNotifications.isEmpty()) delay(20) }
        assertEquals("Fresh alarm task", manager.activeNotifications.single().notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test fun anOldNotificationActionCannotCompleteARestoredTask() = runBlocking {
        val original = store.add("Protected cycle")
        ReminderNotifications(context).show(listOf(original), AppLanguage.ENGLISH)
        withTimeout(5_000) { while (manager.activeNotifications.isEmpty()) delay(20) }
        val action = manager.activeNotifications.single().notification.actions.single().actionIntent
        store.complete(original)
        store.restore(store.tasks.value.single())
        val restored = store.tasks.value.single()
        action.send()
        delay(500)
        store.refresh()
        assertEquals(restored, store.tasks.value.single())
    }

    @Test fun anOldSingleTaskActionDoesNotEraseANewerSummary() = runBlocking {
        val first = store.add("Coffee")
        ReminderNotifications(context).show(listOf(first), AppLanguage.ENGLISH)
        withTimeout(5_000) { while (manager.activeNotifications.isEmpty()) delay(20) }
        val action = manager.activeNotifications.single().notification.actions.single().actionIntent
        val second = store.add("Flowers")
        ReminderNotifications(context).show(listOf(first, second), AppLanguage.ENGLISH)
        withTimeout(5_000) { while (manager.activeNotifications.single().notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString() != "Coffee · Flowers") delay(20) }
        action.send()
        withTimeout(5_000) { while (store.tasks.value.single { it.id == first.id }.completedAt == null) delay(20) }
        delay(200)
        assertEquals("Coffee · Flowers", manager.activeNotifications.single().notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertNull(store.tasks.value.single { it.id == second.id }.completedAt)
    }

    @Test fun singleTaskNotificationCompletesTheCurrentCycleThroughItsRealAction() = runBlocking {
        val item = store.add("Acheter du café")
        assertTrue(ReminderNotifications(context).show(listOf(item), AppLanguage.FRENCH))
        withTimeout(5_000) { while (manager.activeNotifications.isEmpty()) delay(20) }
        val notification = manager.activeNotifications.single().notification
        assertEquals("Acheter du café", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals("Terminer", notification.actions.single().title.toString())
        notification.actions.single().actionIntent.send()
        withTimeout(5_000) { while (store.tasks.value.single().completedAt == null) delay(20) }
        assertNotEquals(item.cycleId, store.tasks.value.single().cycleId)
    }

    @Test fun multipleTasksUseASummaryWithoutACompletionAction() = runBlocking {
        val items = listOf(store.add("Coffee"), store.add("Flowers"), store.add("Laundry"))
        assertTrue(ReminderNotifications(context).show(items, AppLanguage.ENGLISH))
        withTimeout(5_000) { while (manager.activeNotifications.isEmpty()) delay(20) }
        val notification = manager.activeNotifications.single().notification
        assertEquals("3 tasks are waiting for you", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertTrue(notification.actions.isNullOrEmpty())
    }

    @Test fun failedNotificationActionKeepsTheTaskAndReportsTheFailure() = runBlocking {
        val item = store.add("Keep me")
        val database = SQLiteDatabase.openDatabase(context.getDatabasePath("taskomatic.db").path, null, SQLiteDatabase.OPEN_READWRITE)
        database.execSQL("CREATE TRIGGER fail_notification BEFORE UPDATE ON tasks BEGIN SELECT RAISE(ABORT, 'test write failure'); END")
        try {
            ReminderNotifications(context).show(listOf(item), AppLanguage.ENGLISH)
            withTimeout(5_000) { while (manager.activeNotifications.isEmpty()) delay(20) }
            manager.activeNotifications.single().notification.actions.single().actionIntent.send()
            withTimeout(5_000) {
                while (manager.activeNotifications.none { it.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString() == "Unable to complete task" }) delay(20)
            }
            store.refresh()
            assertEquals(item, store.tasks.value.single())
        } finally { database.execSQL("DROP TRIGGER fail_notification"); database.close() }
    }
}
