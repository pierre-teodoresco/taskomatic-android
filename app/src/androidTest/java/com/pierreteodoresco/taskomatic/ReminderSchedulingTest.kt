package com.pierreteodoresco.taskomatic

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pierreteodoresco.taskomatic.core.*
import com.pierreteodoresco.taskomatic.data.*
import com.pierreteodoresco.taskomatic.notifications.ReminderScheduler
import java.time.*
import java.util.UUID
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderSchedulingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun schedulingFailureDoesNotTurnACommittedSaveIntoAFailedSave() = runBlocking {
        val name = "schedule-${UUID.randomUUID()}.db"
        val prefsName = "prefs-$name"
        var calls = 0
        val scheduler = ReminderScheduler { calls++; error("System scheduling unavailable") }
        try {
            TaskStore(context, TaskRepository(context, name), PreferencesRepository(context, prefsName), scheduler = scheduler).use { store ->
                val item = store.add("Persist first")
                assertEquals(listOf(item), store.tasks.value)
                assertTrue(store.schedulingFailed.value)
                TaskRepository(context, name).use { assertEquals(listOf(item), it.tasks()) }
                assertEquals(1, calls)
                SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                    db.execSQL("CREATE TRIGGER fail_save BEFORE INSERT ON tasks BEGIN SELECT RAISE(ABORT, 'test failure'); END")
                    try { store.add("Not saved"); fail("Must report the storage failure") } catch (_: SQLiteException) { }
                    assertEquals(1, calls)
                    assertEquals(listOf(item), store.tasks.value)
                }
            }
        } finally { context.deleteDatabase(name); context.deleteDatabase(prefsName) }
    }

    @Test fun aPersistedTaskSchedulesTheNextLocalReminder() = runBlocking {
        val name = "schedule-${UUID.randomUUID()}.db"
        val prefsName = "prefs-$name"
        val clock = Clock.fixed(Instant.parse("2026-09-08T06:00:00Z"), ZoneOffset.UTC)
        val paris = ZoneId.of("Europe/Paris")
        val tasks = TaskRepository(context, name, clock, { paris })
        val preferences = PreferencesRepository(context, prefsName)
        preferences.save(AppPreferences(reminders = ReminderSettings(enabled = true)))
        val plans = mutableListOf<ReminderPlan?>()
        val scheduler = ReminderScheduler { plan ->
            // The external scheduling boundary must see already-persisted tasks.
            TaskRepository(context, name).use { assertEquals(plan?.tasks.orEmpty(), it.tasks()) }
            plans += plan
        }
        try {
            TaskStore(context, tasks, preferences, clock, { paris }, scheduler).use { store ->
                val item = store.add("Coffee")
                assertEquals(listOf(ReminderPlan(Instant.parse("2026-09-08T07:00:00Z"), listOf(item))), plans)
            }
        } finally { context.deleteDatabase(name); context.deleteDatabase(prefsName) }
    }
}
