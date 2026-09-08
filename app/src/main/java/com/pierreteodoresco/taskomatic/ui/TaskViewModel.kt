package com.pierreteodoresco.taskomatic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pierreteodoresco.taskomatic.TaskomaticApplication
import com.pierreteodoresco.taskomatic.core.TaskItem
import com.pierreteodoresco.taskomatic.core.Recurrence
import com.pierreteodoresco.taskomatic.core.RecurrenceUnit
import com.pierreteodoresco.taskomatic.data.CompletionUndo
import com.pierreteodoresco.taskomatic.data.Appearance
import com.pierreteodoresco.taskomatic.data.AppLanguage
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TaskViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val store = (application as TaskomaticApplication).store
    val tasks = store.tasks
    val preferences = store.preferences
    val preferencesLoaded = store.preferencesLoaded
    val quickTitle = savedState.getStateFlow("quick-title", "")
    val refreshFailed = store.refreshFailed
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()
    private val mutableEditing = MutableStateFlow(savedState.get<ArrayList<String>>("editor-original")?.toTaskSnapshot())
    val editing = mutableEditing.asStateFlow()
    private val mutableUndo = MutableStateFlow<CompletionUndo?>(null)
    val undo = mutableUndo.asStateFlow()
    private val mutableAdded = MutableStateFlow<UUID?>(null)
    val added = mutableAdded.asStateFlow()

    init { refresh() }

    fun refresh() { viewModelScope.launch {
        try { store.refresh() }
        catch (error: CancellationException) { throw error }
        catch (_: Exception) { mutableError.value = true }
    } }

    fun dismissError() { mutableError.value = false }
    fun setAppearance(value: Appearance) = perform { store.setAppearance(value) }
    fun setLanguage(value: AppLanguage) = perform { store.setLanguage(value) }
    fun acknowledgeAdded(id: UUID) { if (mutableAdded.value == id) mutableAdded.value = null }
    fun setQuickTitle(value: String) { if (!mutableBusy.value) savedState["quick-title"] = value }
    private fun setEditing(item: TaskItem?) {
        savedState["editor-original"] = item?.toSavedSnapshot()
        mutableEditing.value = item
    }
    fun openEditor(item: TaskItem) { setEditing(item) }
    fun closeEditor() { if (!mutableBusy.value) setEditing(null) }
    fun edit(item: TaskItem, title: String, note: String, recurrence: Recurrence?) = perform {
        store.edit(item, title, note, recurrence)
        setEditing(null)
    }
    fun delete(item: TaskItem) = perform {
        store.delete(item)
        setEditing(null)
    }

    fun complete(item: TaskItem) = perform { mutableUndo.value = store.complete(item) }
    fun restore(item: TaskItem) = perform { store.restore(item) }
    fun undo(completion: CompletionUndo) {
        viewModelScope.launch {
            try { store.undo(completion) }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableError.value = true }
        }
    }
    fun dismissUndo(completion: CompletionUndo) {
        if (mutableUndo.value == completion) mutableUndo.value = null
    }

    private fun perform(action: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableError.value = true }
            finally { mutableBusy.value = false }
        }
    }

    fun add() {
        val title = quickTitle.value
        if (mutableBusy.value || title.isBlank()) return
        mutableBusy.value = true
        viewModelScope.launch {
            try {
                val item = store.add(title)
                savedState["quick-title"] = ""
                mutableAdded.value = item.id
            }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableError.value = true }
            finally { mutableBusy.value = false }
        }
    }
}

private fun TaskItem.toSavedSnapshot() = arrayListOf(id.toString(), title, note, createdAt.toString(),
    completedAt?.toString().orEmpty(), recurrence?.interval?.toString().orEmpty(),
    recurrence?.unit?.name.orEmpty(), cycleId.toString())

private fun ArrayList<String>.toTaskSnapshot() = TaskItem(UUID.fromString(this[0]), this[1], this[2],
    Instant.parse(this[3]), this[4].takeIf { it.isNotEmpty() }?.let(Instant::parse),
    this[5].takeIf { it.isNotEmpty() }?.let { Recurrence(it.toInt(), RecurrenceUnit.valueOf(this[6])) }, UUID.fromString(this[7]))
