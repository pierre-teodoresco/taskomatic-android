package com.pierreteodoresco.taskomatic.data

import android.content.Context
import com.pierreteodoresco.taskomatic.core.TaskItem
import com.pierreteodoresco.taskomatic.core.Recurrence
import com.pierreteodoresco.taskomatic.notifications.ReminderScheduler
import com.pierreteodoresco.taskomatic.notifications.SystemReminderScheduler
import com.pierreteodoresco.taskomatic.notifications.ReminderNotifications
import com.pierreteodoresco.taskomatic.core.ReminderPlanner
import com.pierreteodoresco.taskomatic.core.ReminderSettings
import com.pierreteodoresco.taskomatic.core.TaskBackup
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TaskStore(
    context: Context,
    private val repository: TaskRepository = TaskRepository(context),
    private val preferencesRepository: PreferencesRepository = PreferencesRepository(context),
    private val clock: Clock = Clock.systemUTC(),
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val scheduler: ReminderScheduler = SystemReminderScheduler(context),
) : AutoCloseable {
    private val notifications = ReminderNotifications(context)
    private val mutex = Mutex()
    private val mutableTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks = mutableTasks.asStateFlow()
    private val mutableRefreshFailed = MutableStateFlow(false)
    val refreshFailed = mutableRefreshFailed.asStateFlow()
    private val mutablePreferences = MutableStateFlow(AppPreferences())
    val preferences = mutablePreferences.asStateFlow()
    private val mutablePreferencesLoaded = MutableStateFlow(false)
    val preferencesLoaded = mutablePreferencesLoaded.asStateFlow()
    private val mutableBackgroundFailed = MutableStateFlow(false)
    val backgroundFailed = mutableBackgroundFailed.asStateFlow()
    fun reportBackgroundFailure() { mutableBackgroundFailed.value = true }
    fun dismissBackgroundFailure() { mutableBackgroundFailed.value = false }
    private val mutableSchedulingFailed = MutableStateFlow(false)
    val schedulingFailed = mutableSchedulingFailed.asStateFlow()
    private val mutableNextReminder = MutableStateFlow<Instant?>(null)
    val nextReminder = mutableNextReminder.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock {
            mutablePreferences.value = preferencesRepository.read()
            mutablePreferencesLoaded.value = true
            mutableTasks.value = repository.tasks()
            mutableRefreshFailed.value = false
            synchronizeReminders()
        }
    }

    suspend fun deliverReminder(expectedAt: Instant) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val freshPreferences = preferencesRepository.read()
            val freshTasks = repository.tasks()
            mutablePreferences.value = freshPreferences
            mutablePreferencesLoaded.value = true
            mutableTasks.value = freshTasks
            try {
                val due = ReminderPlanner.due(freshTasks, freshPreferences.reminders, expectedAt, clock.instant(), zone())
                notifications.show(due, freshPreferences.language)
            } finally { synchronizeReminders() }
        }
    }

    suspend fun add(title: String, note: String = "", recurrence: Recurrence? = null) = mutate { repository.add(title, note, recurrence) }
    suspend fun importBackup(backup: TaskBackup) = mutate { repository.importBackup(backup) }
    suspend fun backup() = withContext(Dispatchers.IO) {
        mutex.withLock { TaskBackup(repository.tasks(), clock.instant()) }
    }
    suspend fun complete(item: TaskItem) = mutate { repository.complete(item.id, item.cycleId) }
    suspend fun completeFromNotification(id: UUID, cycle: UUID) = mutate {
        val completion = repository.complete(id, cycle)
        if (completion != null) {
            // Share the delivery mutex: a newer notification cannot replace this one between check and cancel.
            // Dismissing a system notification is best-effort; it must not turn a committed completion into a failed save.
            runCatching { notifications.cancelIfMatches(id, cycle) }
        }
        completion
    }
    suspend fun restore(item: TaskItem) = mutate { repository.restore(item.id, item.cycleId) }
    suspend fun undo(undo: CompletionUndo) = mutate { repository.undo(undo) }
    suspend fun delete(item: TaskItem) = mutate { repository.delete(item.id) }
    suspend fun edit(item: TaskItem, title: String, note: String, recurrence: Recurrence?) = mutate {
        repository.edit(item, title, note, recurrence)
    }

    suspend fun setAppearance(value: Appearance) = updatePreferences { it.copy(appearance = value) }
    suspend fun setLanguage(value: AppLanguage) = updatePreferences { it.copy(language = value) }
    suspend fun setRemindersEnabled(value: Boolean) = updatePreferences { it.copy(reminders = it.reminders.copy(enabled = value)) }
    suspend fun setReminderTime(value: LocalTime) = updatePreferences { it.copy(reminders = it.reminders.copy(time = value.withSecond(0).withNano(0))) }
    suspend fun toggleReminderDay(value: DayOfWeek) = updatePreferences {
        val days = it.reminders.weekdays
        it.copy(reminders = it.reminders.copy(weekdays = if (value in days) days - value else days + value))
    }

    private suspend fun updatePreferences(transform: (AppPreferences) -> AppPreferences) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = transform(preferencesRepository.read())
            preferencesRepository.save(updated)
            mutablePreferences.value = updated
            synchronizeReminders()
        }
    }

    private suspend fun <T> mutate(action: () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            val result = action()
            // A refresh failure after a committed write must not offer to repeat that write.
            try { mutableTasks.value = repository.tasks(); mutableRefreshFailed.value = false }
            catch (_: Exception) { mutableRefreshFailed.value = true }
            synchronizeReminders()
            result
        }
    }

    private fun synchronizeReminders() {
        try {
            val plan = ReminderPlanner.next(repository.tasks(), preferencesRepository.read().reminders, clock.instant(), zone())
            scheduler.replace(plan)
            mutableNextReminder.value = plan?.at
            mutableSchedulingFailed.value = false
        } catch (_: Exception) { mutableSchedulingFailed.value = true; mutableNextReminder.value = null }
    }

    override fun close() { repository.close(); preferencesRepository.close() }
}
