package com.pierreteodoresco.taskomatic

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pierreteodoresco.taskomatic.core.TaskBackup
import com.pierreteodoresco.taskomatic.core.TaskItem
import com.pierreteodoresco.taskomatic.ui.TaskViewModel
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupAppTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = (context.applicationContext as TaskomaticApplication).store

    @Test fun invalidImportAndFailedExportLeaveTasksUntouchedAndAllowRetry() = runBlocking<Unit> {
        val item = store.add("Keep ${UUID.randomUUID()}")
        val invalid = File.createTempFile("invalid-backup-", ".json", context.cacheDir)
        val output = File.createTempFile("retry-backup-", ".json", context.cacheDir)
        val missing = File(context.cacheDir, "missing-${UUID.randomUUID()}/backup.json")
        try {
            invalid.writeText("{\"version\":999}")
            val before = store.tasks.value
            ActivityScenario.launch(MainActivity::class.java).use { activity ->
                compose.onNodeWithTag("settings-open").performClick()
                activity.onActivity { ViewModelProvider(it)[TaskViewModel::class.java].prepareImport(Uri.fromFile(invalid)) }
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("backup-notice-close").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(context.getString(R.string.backup_failed)).assertIsDisplayed()
                assertEquals(before, store.tasks.value)
                compose.onNodeWithTag("backup-notice-close").performClick()
                activity.onActivity { ViewModelProvider(it)[TaskViewModel::class.java].exportBackup(Uri.fromFile(missing)) }
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("backup-notice-close").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(context.getString(R.string.backup_failed)).assertIsDisplayed()
                assertEquals(before, store.tasks.value)
                compose.onNodeWithTag("backup-notice-close").performClick()
                activity.onActivity { ViewModelProvider(it)[TaskViewModel::class.java].exportBackup(Uri.fromFile(output)) }
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("backup-notice-close").fetchSemanticsNodes().isNotEmpty() }
                assertEquals(before, TaskBackup.decode(output.readBytes()).items)
            }
        } finally { invalid.delete(); output.delete(); store.delete(item) }
    }

    @Test fun cancellingAValidatedImportDoesNotWriteAnyTask() = runBlocking<Unit> {
        val incoming = TaskItem(title = "Cancelled import", createdAt = Instant.parse("2026-09-08T12:00:00Z"))
        val file = File.createTempFile("cancel-backup-", ".json", context.cacheDir)
        try {
            file.writeBytes(TaskBackup(listOf(incoming), Instant.parse("2026-09-08T13:00:00Z")).encoded())
            ActivityScenario.launch(MainActivity::class.java).use { activity ->
                compose.onNodeWithTag("settings-open").performClick()
                activity.onActivity { ViewModelProvider(it)[TaskViewModel::class.java].prepareImport(Uri.fromFile(file)) }
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("import-cancel").fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag("import-cancel").performClick()
                compose.onNodeWithTag("import-confirm").assertDoesNotExist()
                assertFalse(store.tasks.value.any { it.id == incoming.id })
            }
        } finally { file.delete() }
    }

    @Test fun exportWritesTheFreshPersistedSnapshotWithoutChangingTasks() = runBlocking<Unit> {
        val item = store.add("Export ${UUID.randomUUID()}")
        val file = File.createTempFile("taskomatic-export-", ".json", context.cacheDir)
        try {
            val before = store.tasks.value
            ActivityScenario.launch(MainActivity::class.java).use { activity ->
                compose.onNodeWithTag("settings-open").performClick()
                activity.onActivity { ViewModelProvider(it)[TaskViewModel::class.java].exportBackup(Uri.fromFile(file)) }
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("backup-notice-close").fetchSemanticsNodes().isNotEmpty() }
                assertEquals(before, TaskBackup.decode(file.readBytes()).items)
                assertEquals(before, store.tasks.value)
                compose.onNodeWithTag("backup-notice-close").performClick()
            }
        } finally { file.delete(); store.delete(item) }
    }

    @Test fun importRequiresConfirmationAndPreservesExistingTasksAcrossRecreation() = runBlocking<Unit> {
        val local = store.add("Local ${UUID.randomUUID()}")
        val incoming = TaskItem(title = "Imported ${UUID.randomUUID()}", createdAt = Instant.parse("2026-09-08T12:00:00Z"))
        val file = File.createTempFile("taskomatic-import-", ".json", context.cacheDir)
        try {
            file.writeBytes(TaskBackup(listOf(local.copy(title = "Must not overwrite"), incoming), Instant.parse("2026-09-08T13:00:00Z")).encoded())
            ActivityScenario.launch(MainActivity::class.java).use { activity ->
                compose.onNodeWithTag("settings-open").performClick()
                activity.onActivity { ViewModelProvider(it)[TaskViewModel::class.java].prepareImport(Uri.fromFile(file)) }
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("import-confirm").fetchSemanticsNodes().isNotEmpty() }
                assertFalse(store.tasks.value.any { it.id == incoming.id })
                activity.recreate()
                compose.onNodeWithTag("import-confirm").assertIsDisplayed().performClick()
                compose.waitUntil(5_000) { store.tasks.value.any { it.id == incoming.id } }
                assertEquals(local.title, store.tasks.value.single { it.id == local.id }.title)
                assertNotEquals(incoming.cycleId, store.tasks.value.single { it.id == incoming.id }.cycleId)
                compose.onNodeWithTag("backup-notice-close").performClick()
            }
        } finally {
            file.delete()
            store.tasks.value.filter { it.id == local.id || it.id == incoming.id }.forEach { store.delete(it) }
        }
    }
}
