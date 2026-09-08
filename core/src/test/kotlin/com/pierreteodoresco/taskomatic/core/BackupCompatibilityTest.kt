package com.pierreteodoresco.taskomatic.core

import java.io.File
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class BackupCompatibilityTest {
    @Test fun readsAnActualSwiftEncodedBackupAndProducesTheReverseCompatibilityArtifact() {
        // Generated with Taskomatic iOS TaskBackup.encoded(), checkout 0e8a3b0, 2026-09-08.
        val bytes = javaClass.getResourceAsStream("/ios-v1.json")!!.use { it.readBytes() }
        val backup = TaskBackup.decode(bytes)
        assertEquals(Instant.parse("2026-09-08T12:00:00Z"), backup.createdAt)
        assertEquals(3, backup.items.size)
        assertEquals("Café ☕", backup.items[0].title)
        assertEquals("日本語 aussi", backup.items[0].note)
        assertNull(backup.items[0].completedAt)
        assertEquals(Instant.parse("2024-01-31T16:30:00Z"), backup.items[1].completedAt)
        assertEquals(Recurrence(1, RecurrenceUnit.MONTH), backup.items[1].recurrence)
        assertEquals(Instant.parse("2024-02-01T09:15:00Z"), backup.items[2].completedAt)
        assertNull(backup.items[2].recurrence)
        assertEquals(UUID.fromString("20000000-0000-0000-0000-000000000003"), backup.items[2].cycleId)
        val encoded = backup.encoded()
        assertEquals(backup, TaskBackup.decode(encoded))
        // Test output only, ignored with build/. Also decoded by Swift during delivery verification.
        File("build/verification/android-v1.json").apply { parentFile.mkdirs(); writeBytes(encoded) }
    }
}
