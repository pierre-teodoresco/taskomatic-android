package com.pierreteodoresco.taskomatic.ui

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.pierreteodoresco.taskomatic.TaskomaticApplication
import com.pierreteodoresco.taskomatic.core.TaskItem
import com.pierreteodoresco.taskomatic.core.Recurrence
import com.pierreteodoresco.taskomatic.data.CompletionUndo
import com.pierreteodoresco.taskomatic.data.Appearance
import com.pierreteodoresco.taskomatic.data.AppLanguage
import com.pierreteodoresco.taskomatic.data.BackupDocuments
import com.pierreteodoresco.taskomatic.data.TextStateStorage
import com.pierreteodoresco.taskomatic.core.TaskBackup
import java.time.LocalTime
import java.time.DayOfWeek
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable

class TaskViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val store = (application as TaskomaticApplication).store
    private val documents = BackupDocuments(application.contentResolver)
    val textStateStorage = TextStateStorage(application)
    private val mutableDraftFailed = MutableStateFlow(false)
    val draftFailed = mutableDraftFailed.asStateFlow()
    private val mutablePendingImport = MutableStateFlow<TaskBackup?>(null)
    val pendingImport = mutablePendingImport.asStateFlow()
    private val mutableBackupNotice = MutableStateFlow<BackupNotice?>(null)
    val backupNotice = mutableBackupNotice.asStateFlow()
    val tasks = store.tasks
    val preferences = store.preferences
    val preferencesLoaded = store.preferencesLoaded
    val schedulingFailed = store.schedulingFailed
    val nextReminder = store.nextReminder
    private var quickStateKey = savedState.get<String>("quick-state-key") ?: UUID.randomUUID().toString().also { savedState["quick-state-key"] = it }
    private val mutableQuickTitle = MutableStateFlow(try {
        savedState.get<Bundle>("quick-title-snapshot")?.getString("text")?.let(textStateStorage::restore)
            ?: savedState.get<String>("quick-title").orEmpty()
    } catch (_: Exception) { mutableDraftFailed.value = true; "" })
    val quickTitle = mutableQuickTitle.asStateFlow()
    val refreshFailed = store.refreshFailed
    val backgroundFailed = store.backgroundFailed
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableError = MutableStateFlow(false)
    val error = mutableError.asStateFlow()
    private val mutableEditing = MutableStateFlow(try {
        savedState.get<ArrayList<String>>("editor-original")?.let(textStateStorage::restoreSnapshot)
    } catch (_: Exception) { mutableDraftFailed.value = true; null })
    val editing = mutableEditing.asStateFlow()
    private val mutableCreating = MutableStateFlow(savedState.get<Boolean>("editor-creating") ?: false)
    val creating = mutableCreating.asStateFlow()
    // Keep the published key stable until the editor leaves composition, even after clearing recovery state.
    private var publishedEditorKey = savedState.get<String>("editor-state-key")
    val editorStateKey: String get() = checkNotNull(publishedEditorKey)
    private val mutableUndo = MutableStateFlow<CompletionUndo?>(null)
    val undo = mutableUndo.asStateFlow()
    private val mutableAdded = MutableStateFlow<UUID?>(null)
    val added = mutableAdded.asStateFlow()

    init {
        savedState.remove<String>("quick-title")
        savedState.setSavedStateProvider("quick-title-snapshot") {
            Bundle().apply { putString("text", try { textStateStorage.save(quickTitle.value, "$quickStateKey-draft-title") }
                catch (_: Exception) { reportDraftFailure(); "E" }) }
        }
        refresh()
    }

    fun refresh() { viewModelScope.launch {
        try { store.refresh() }
        catch (error: CancellationException) { throw error }
        catch (_: Exception) { mutableError.value = true }
    } }

    fun dismissError() { mutableError.value = false; mutableDraftFailed.value = false; store.dismissBackgroundFailure() }
    fun reportDraftFailure() { mutableDraftFailed.value = true }
    fun setAppearance(value: Appearance) = perform { store.setAppearance(value) }
    fun setLanguage(value: AppLanguage) = perform { store.setLanguage(value) }
    fun setRemindersEnabled(value: Boolean) = perform { store.setRemindersEnabled(value) }
    fun setReminderTime(value: LocalTime) = perform { store.setReminderTime(value) }
    fun toggleReminderDay(value: DayOfWeek) = perform { store.toggleReminderDay(value) }
    fun prepareImport(uri: Uri) = backupAction {
        mutablePendingImport.value = null
        mutablePendingImport.value = withContext(Dispatchers.IO) { documents.read(uri) }
    }
    fun exportBackup(uri: Uri) = backupAction {
        val backup = store.backup()
        withContext(Dispatchers.IO) { documents.write(uri, backup) }
        mutableBackupNotice.value = BackupNotice.Exported
    }
    fun cancelImport() { if (!mutableBusy.value) mutablePendingImport.value = null }
    fun dismissBackupNotice() { mutableBackupNotice.value = null }
    fun confirmImport() {
        val backup = mutablePendingImport.value ?: return
        backupAction {
            val inserted = store.importBackup(backup)
            mutablePendingImport.value = null
            mutableBackupNotice.value = BackupNotice.Imported(inserted, backup.items.size - inserted)
        }
    }

    private fun backupAction(action: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableBackupNotice.value = BackupNotice.Failed }
            finally { mutableBusy.value = false }
        }
    }
    fun acknowledgeAdded(id: UUID) { if (mutableAdded.value == id) mutableAdded.value = null }
    fun setQuickTitle(value: String) {
        if (mutableBusy.value) return
        if (value.isEmpty() && mutableQuickTitle.value.isNotEmpty()) clearQuickDraft()
        else mutableQuickTitle.value = value
    }
    private fun clearQuickDraft() {
        val oldKey = quickStateKey
        mutableQuickTitle.value = ""
        quickStateKey = UUID.randomUUID().toString()
        savedState["quick-state-key"] = quickStateKey
        (getApplication<Application>() as TaskomaticApplication).backgroundScope.launch {
            runCatching { textStateStorage.removeEditor(oldKey) }
        }
    }
    private fun clearEditor() {
        val key = savedState.get<String>("editor-state-key")
        savedState["editor-original"] = null
        savedState["editor-state-key"] = null
        mutableEditing.value = null
        savedState["editor-creating"] = false
        mutableCreating.value = false
        if (key != null) (getApplication<Application>() as TaskomaticApplication).backgroundScope.launch {
            runCatching { textStateStorage.removeEditor(key) }
        }
    }
    fun openNewTask() = openEditor(TaskItem(title = quickTitle.value, createdAt = java.time.Instant.now()), isNew = true)

    fun openEditor(item: TaskItem, isNew: Boolean = false) {
        if (mutableBusy.value) return
        val key = UUID.randomUUID().toString()
        fun publish(snapshot: ArrayList<String>) {
            clearEditor()
            savedState["editor-state-key"] = key
            publishedEditorKey = key
            savedState["editor-original"] = snapshot
            savedState["editor-creating"] = isNew
            mutableCreating.value = isNew
            mutableEditing.value = item
        }
        if (item.title.length <= TextStateStorage.INLINE_LIMIT && item.note.length <= TextStateStorage.INLINE_LIMIT) {
            publish(textStateStorage.saveSnapshot(item, key))
        } else perform {
            try { publish(withContext(Dispatchers.IO) { textStateStorage.saveSnapshot(item, key) }) }
            catch (error: Exception) {
                // Also roll back when cancellation occurs after IO finishes but before publication.
                withContext(NonCancellable + Dispatchers.IO) { runCatching { textStateStorage.removeEditor(key) } }
                throw error
            }
        }
    }
    fun closeEditor() { if (!mutableBusy.value) clearEditor() }
    fun edit(item: TaskItem, title: String, note: String, recurrence: Recurrence?) = perform {
        if (mutableCreating.value) {
            val created = store.add(title, note, recurrence)
            clearQuickDraft()
            mutableAdded.value = created.id
        } else store.edit(item, title, note, recurrence)
        clearEditor()
    }
    fun delete(item: TaskItem) = perform {
        store.delete(item)
        clearEditor()
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
                clearQuickDraft()
                mutableAdded.value = item.id
            }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableError.value = true }
            finally { mutableBusy.value = false }
        }
    }
}

sealed interface BackupNotice {
    data class Imported(val inserted: Int, val skipped: Int) : BackupNotice
    data object Failed : BackupNotice
    data object Exported : BackupNotice
}
