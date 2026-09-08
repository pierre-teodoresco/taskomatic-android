package com.pierreteodoresco.taskomatic

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pierreteodoresco.taskomatic.core.*
import com.pierreteodoresco.taskomatic.data.TaskRepository
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LargeTaskStorageTest {
    @Test fun aValidLargeBackupRemainsReadableEditableAndExportableAfterReopening() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "large-task-${UUID.randomUUID()}.db"
        val now = Instant.parse("2026-09-08T12:00:00Z")
        val original = TaskItem(title = "Large imported note", note = "é☕\u0000".repeat(550_000), createdAt = now)
        val backup = TaskBackup.decode(TaskBackup(listOf(original), now).encoded())
        try {
            TaskRepository(context, name).use { assertEquals(1, it.importBackup(backup)) }
            TaskRepository(context, name).use { repository ->
                val imported = repository.tasks().single()
                assertEquals(original.note, imported.note)
                repository.edit(imported, "Renamed", imported.note, null)
                val edited = repository.tasks().single()
                assertEquals("Renamed", edited.title)
                assertEquals(original.note, TaskBackup.decode(TaskBackup(listOf(edited), now).encoded()).items.single().note)
                assertNotNull(repository.complete(edited.id, edited.cycleId))
            }
        } finally { context.deleteDatabase(name) }
    }
}
