package com.pierreteodoresco.taskomatic.data

import android.content.Context
import com.pierreteodoresco.taskomatic.core.TaskItem
import com.pierreteodoresco.taskomatic.core.Recurrence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TaskStore(context: Context) {
    private val repository = TaskRepository(context)
    private val mutex = Mutex()
    private val mutableTasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks = mutableTasks.asStateFlow()
    private val mutableRefreshFailed = MutableStateFlow(false)
    val refreshFailed = mutableRefreshFailed.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock {
            mutableTasks.value = repository.tasks()
            mutableRefreshFailed.value = false
        }
    }

    suspend fun add(title: String) = mutate { repository.add(title) }
    suspend fun complete(item: TaskItem) = mutate { repository.complete(item.id, item.cycleId) }
    suspend fun restore(item: TaskItem) = mutate { repository.restore(item.id, item.cycleId) }
    suspend fun undo(undo: CompletionUndo) = mutate { repository.undo(undo) }
    suspend fun delete(item: TaskItem) = mutate { repository.delete(item.id) }
    suspend fun edit(item: TaskItem, title: String, note: String, recurrence: Recurrence?) = mutate {
        repository.edit(item, title, note, recurrence)
    }

    private suspend fun <T> mutate(action: () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            val result = action()
            // A refresh failure after a committed write must not offer to repeat that write.
            try { mutableTasks.value = repository.tasks(); mutableRefreshFailed.value = false }
            catch (_: Exception) { mutableRefreshFailed.value = true }
            result
        }
    }
}
