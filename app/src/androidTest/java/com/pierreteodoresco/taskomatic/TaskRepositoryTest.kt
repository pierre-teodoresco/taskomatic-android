package com.pierreteodoresco.taskomatic

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pierreteodoresco.taskomatic.data.TaskRepository
import com.pierreteodoresco.taskomatic.core.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "repository-test-${UUID.randomUUID()}.db"
    private val clock = Clock.fixed(Instant.parse("2026-09-08T08:00:00Z"), ZoneId.of("Europe/Paris"))

    @After fun cleanUp() { context.deleteDatabase(name) }

    @Test fun failedImportRollsBackEveryInsertionAndKeepsExistingTasks() {
        TaskRepository(context, name, clock).use { repo ->
            repo.add("Existing")
            context.openOrCreateDatabase(name, 0, null).use { database ->
                database.execSQL("""CREATE TRIGGER fail_import BEFORE INSERT ON tasks
                    WHEN NEW.title = 'Reject' BEGIN SELECT RAISE(ABORT, 'Simulated storage failure'); END""")
            }
            val backup = TaskBackup(listOf(
                TaskItem(title = "First", createdAt = clock.instant()),
                TaskItem(title = "Reject", createdAt = clock.instant()),
            ), clock.instant())
            assertThrows(android.database.sqlite.SQLiteException::class.java) { repo.importBackup(backup) }
        }
        TaskRepository(context, name, clock).use { repo ->
            assertEquals(listOf("Existing"), repo.tasks().map { it.title })
            repo.add("Still writable")
            assertEquals(2, repo.tasks().size)
        }
    }

    @Test fun aStaleRestoreCannotUndoANewerCompletion() {
        TaskRepository(context, name, clock).use { repo ->
            val original = repo.add("Café")
            repo.complete(original.id)
            val archived = repo.tasks().single()
            assertTrue(repo.restore(original.id, archived.cycleId))
            repo.complete(original.id)
            assertFalse(repo.restore(original.id, archived.cycleId))
            assertNotNull(repo.tasks().single().completedAt)
        }
    }

    @Test fun removingRecurrenceFromAStaleEditorPreservesTheFreshActiveCycle() {
        val original = TaskRepository(context, name, clock).use { repo ->
            val item = repo.add("Lessive", recurrence = Recurrence(1, RecurrenceUnit.DAY))
            repo.complete(item.id)
            item
        }
        val later = Clock.fixed(Instant.parse("2026-09-10T10:00:00Z"), clock.zone)
        TaskRepository(context, name, later) { clock.zone }.use { repo ->
            val edited = repo.edit(original, original.title, original.note, null)
            assertNull(edited.completedAt)
            assertTrue(edited.isActive(later.instant(), later.zone))
        }
    }

    @Test fun deletionPersistsAndAnOpenEditorCannotResurrectTheTask() {
        val original = TaskRepository(context, name, clock).use { repo ->
            val item = repo.add("À supprimer")
            assertTrue(repo.delete(item.id))
            assertFalse(repo.delete(item.id))
            assertThrows(IllegalStateException::class.java) { repo.edit(item, "Revenir", "", null) }
            item
        }
        TaskRepository(context, name, clock).use { repo ->
            assertTrue(repo.tasks().isEmpty())
            assertNull(repo.complete(original.id, original.cycleId))
        }
    }

    @Test fun backupRestoreIsAddOnlyIdempotentAndRotatesImportedCycleTokens() {
        TaskRepository(context, name, clock).use { repo ->
            val existing = repo.add("Local", "Garder")
            val missing = TaskItem(title = "Importer", createdAt = clock.instant(),
                recurrence = Recurrence(2, RecurrenceUnit.WEEK)).complete(clock.instant())
            val backup = TaskBackup(listOf(existing.copy(title = "Ancien"), missing), clock.instant())
            assertEquals(1, repo.importBackup(backup))
            assertEquals(0, repo.importBackup(backup))
            val all = repo.tasks()
            assertEquals("Local", all.single { it.id == existing.id }.title)
            val restored = all.single { it.id == missing.id }
            assertEquals(missing.completedAt, restored.completedAt)
            assertNotEquals(missing.cycleId, restored.cycleId)
        }
        TaskRepository(context, name, clock).use { assertEquals(2, it.tasks().size) }
    }

    @Test fun undoPreservesLaterEditsAndRejectsASupersededCompletion() {
        TaskRepository(context, name, clock).use { repo ->
            val original = repo.add("Café")
            val undo = repo.complete(original.id)!!
            repo.edit(original, "Thé", "En feuilles", null)
            assertTrue(repo.undo(undo))
            assertEquals("Thé", repo.tasks().single().title)
            assertTrue(repo.tasks().single().isActive(clock.instant(), clock.zone))
            repo.complete(original.id)
            assertFalse(repo.undo(undo))
            assertFalse(repo.tasks().single().isActive(clock.instant(), clock.zone))
        }
    }

    @Test fun removingRecurrenceFromAnActiveReturnedTaskDoesNotArchiveIt() {
        TaskRepository(context, name, clock).use { repo ->
            val task = repo.add("Lessive", recurrence = Recurrence(1, RecurrenceUnit.DAY))
            repo.complete(task.id)
        }
        val later = Clock.fixed(Instant.parse("2026-09-15T10:00:00Z"), clock.zone)
        TaskRepository(context, name, later) { clock.zone }.use { repo ->
            val original = repo.tasks().single()
            assertTrue(original.isActive(later.instant(), later.zone))
            val edited = repo.edit(original, original.title, original.note, null)
            assertTrue(edited.isActive(later.instant(), later.zone))
            assertNull(edited.completedAt)
            assertNotEquals(original.cycleId, edited.cycleId)
        }
    }

    @Test fun editorPreservesACompletionAndUnrelatedNoteSavedAfterItOpened() {
        TaskRepository(context, name, clock).use { editor ->
            val original = editor.add("Titre", "Ancienne note")
            TaskRepository(context, name, clock).use { other ->
                other.complete(original.id)
                other.edit(original, original.title, "Nouvelle note", null)
            }
            editor.edit(original, "Titre modifié", original.note, null)
            val saved = editor.tasks().single()
            assertEquals("Titre modifié", saved.title)
            assertEquals("Nouvelle note", saved.note)
            assertEquals(clock.instant(), saved.completedAt)
        }
    }

    @Test fun completionAndRestorationPersistAndAnOldNotificationCannotCompleteTheNewCycle() {
        val original = TaskRepository(context, name, clock).use { repo ->
            val task = repo.add("Café")
            assertNotNull(repo.complete(task.id, task.cycleId))
            assertFalse(repo.tasks().single().isActive(clock.instant(), clock.zone))
            assertTrue(repo.restore(task.id, repo.tasks().single().cycleId))
            task
        }
        TaskRepository(context, name, clock).use { repo ->
            assertNull(repo.complete(original.id, original.cycleId))
            assertTrue(repo.tasks().single().isActive(clock.instant(), clock.zone))
            assertNotEquals(original.cycleId, repo.tasks().single().cycleId)
        }
    }

    @Test fun creationPersistsTitleAndNoteAcrossRepositoryReopen() {
        val created = TaskRepository(context, name, clock).use { it.add("  Café  ", "En grains", null) }
        TaskRepository(context, name, clock).use { reopened ->
            val saved = reopened.tasks().single()
            assertEquals(created.id, saved.id)
            assertEquals("Café", saved.title)
            assertEquals("En grains", saved.note)
            assertEquals(Instant.parse("2026-09-08T08:00:00Z"), saved.createdAt)
        }
    }
}
