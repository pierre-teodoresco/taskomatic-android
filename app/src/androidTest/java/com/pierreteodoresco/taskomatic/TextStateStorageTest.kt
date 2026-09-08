package com.pierreteodoresco.taskomatic

import android.content.ContextWrapper
import androidx.compose.runtime.saveable.SaverScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pierreteodoresco.taskomatic.data.TextStateStorage
import com.pierreteodoresco.taskomatic.ui.textStateSaver
import java.io.File
import java.util.UUID
import java.time.Instant
import java.io.IOException
import com.pierreteodoresco.taskomatic.core.TaskItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextStateStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun aFailedSecondSnapshotFieldRollsBackTheNewRecoveryFiles() {
        val key = UUID.randomUUID().toString()
        val directory = File(context.filesDir, "editor-state").apply { mkdirs() }
        val blocked = File(directory, "$key-original-note.new").apply { createNewFile() }
        assertTrue(blocked.setWritable(false, false))
        val storage = TextStateStorage(context)
        try {
            val item = TaskItem(title = "Title ".repeat(2_000), note = "Note ".repeat(2_000), createdAt = Instant.parse("2026-09-08T12:00:00Z"))
            try { storage.saveSnapshot(item, key); fail("Second field should fail to write") } catch (_: IOException) { }
            assertFalse(File(directory, "$key-original-title").exists())
        } finally { blocked.setWritable(true, false); storage.removeEditor(key) }
    }

    @Test fun largeStateRoundTripsUsingASmallReferenceAndMissingFilesAreReported() {
        val key = UUID.randomUUID().toString()
        val text = "Fé☕\u0000".repeat(25_000)
        val storage = TextStateStorage(context)
        val reference = storage.save(text, "$key-draft-note")
        assertTrue(reference.length < 100)
        assertEquals(text, TextStateStorage(context).restore(reference))
        assertEquals("Fuser text", storage.restore(storage.save("Fuser text", "$key-draft-title")))
        storage.removeEditor(key)
        var reported = false
        val saver = textStateSaver(storage, "$key-draft-note") { reported = true }
        assertNull(saver.restore(reference))
        assertTrue(reported)
    }

    @Test fun aDraftWriteFailureProducesARecoveryWarningInsteadOfAnOversizedBundle() {
        val blocker = File.createTempFile("blocked-draft-", ".tmp", context.cacheDir)
        val unavailable = object : ContextWrapper(context) { override fun getFilesDir(): File = blocker }
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
        var reports = 0
        val saver = textStateSaver(TextStateStorage(unavailable), "unavailable-draft-note") { reports++ }
        try {
            val reference = with(saver) { scope.save("Draft ".repeat(50_000)) }
            assertEquals("E", reference)
            assertEquals(1, reports)
            assertNull(saver.restore(reference!!))
            assertEquals(2, reports)
        } finally { blocker.delete() }
    }
}
