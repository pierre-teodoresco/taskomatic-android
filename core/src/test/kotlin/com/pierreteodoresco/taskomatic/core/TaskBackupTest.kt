package com.pierreteodoresco.taskomatic.core

import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class TaskBackupTest {
    @Test fun validatedBackupIsUnaffectedByLaterChangesToTheSourceList() {
        val decoded = TaskBackup.decode(fixture.toByteArray())
        val source = decoded.items.toMutableList()
        val snapshot = TaskBackup(source, decoded.createdAt)
        source.add(source.single().copy(title = ""))
        assertEquals(1, snapshot.items.size)
        assertEquals(decoded, TaskBackup.decode(snapshot.encoded()))
    }
    @Test fun rejectsDuplicateIdsAndOversizedBackupDocuments() {
        val decoded = TaskBackup.decode(fixture.toByteArray())
        assertThrows(IllegalArgumentException::class.java) {
            TaskBackup(listOf(decoded.items.single(), decoded.items.single()), decoded.createdAt)
        }
        assertThrows(IllegalArgumentException::class.java) { TaskBackup.decode(ByteArray(10 * 1024 * 1024 + 1)) }
        val many = (0..10_000).map { decoded.items.single().copy(id = UUID.randomUUID()) }
        assertThrows(IllegalArgumentException::class.java) { TaskBackup(many, decoded.createdAt) }
    }
    @Test fun rejectsAnEntireBackupWithAnInvalidTaskBeforeReturningAnyItems() {
        val invalidTitle = fixture.replace("Café ☕", "   ")
        assertThrows(IllegalArgumentException::class.java) { TaskBackup.decode(invalidTitle.toByteArray()) }
        val invalidDate = fixture.replace("1788854400000", "999999999999999")
        assertThrows(IllegalArgumentException::class.java) { TaskBackup.decode(invalidDate.toByteArray()) }
        val invalidId = fixture.replace("10000000-0000-0000-0000-000000000001", "1-0-0-0-1")
        assertThrows(IllegalArgumentException::class.java) { TaskBackup.decode(invalidId.toByteArray()) }
    }
    @Test fun exportRoundTripPreservesActiveArchivedAndRecurringTasks() {
        val now = Instant.parse("2026-09-08T08:00:00Z")
        val active = TaskItem(title = "Café ☕", note = "日本語", createdAt = now)
        val archived = TaskItem(title = "Done", createdAt = now).complete(now)
        val recurring = TaskItem(title = "Walk", createdAt = now,
            recurrence = Recurrence(3, RecurrenceUnit.DAY)).complete(now)
        val original = TaskBackup(listOf(active, archived, recurring), now)
        val encoded = original.encoded()
        assertTrue(encoded.decodeToString().contains("com.pierreteodoresco.taskomatic.backup"))
        assertEquals(original, TaskBackup.decode(encoded))
    }
    private val fixture = """
        {"format":"com.pierreteodoresco.taskomatic.backup","version":1,
         "createdAt":1788854400000,"items":[
          {"id":"10000000-0000-0000-0000-000000000001","title":"Café ☕","note":"En grains",
           "createdAt":1788854400000,"completedAt":1788854400000,
           "cycleID":"20000000-0000-0000-0000-000000000001",
           "recurrence":{"interval":2,"unit":"week"}}]}
    """.trimIndent()

    @Test fun decodesTheIosVersionOneFormatWithoutTranslatingTaskContent() {
        val backup = TaskBackup.decode(fixture.toByteArray())
        val item = backup.items.single()
        assertEquals("Café ☕", item.title)
        assertEquals("En grains", item.note)
        assertEquals(Instant.parse("2026-09-08T08:00:00Z"), item.completedAt)
        assertEquals(Recurrence(2, RecurrenceUnit.WEEK), item.recurrence)
        assertEquals(UUID.fromString("10000000-0000-0000-0000-000000000001"), item.id)
    }
}
