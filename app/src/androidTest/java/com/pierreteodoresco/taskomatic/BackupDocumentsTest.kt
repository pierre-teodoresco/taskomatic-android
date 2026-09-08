package com.pierreteodoresco.taskomatic

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pierreteodoresco.taskomatic.core.TaskBackup
import com.pierreteodoresco.taskomatic.core.TaskItem
import com.pierreteodoresco.taskomatic.data.BackupDocuments
import java.io.File
import java.io.InputStream
import java.io.IOException
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupDocumentsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun anOversizedProviderStreamStopsAtTheLimitWithoutReadingTheWholeFile() {
        var count = 0
        val stream = object : InputStream() {
            override fun read(): Int {
                count++
                if (count > TaskBackup.MAX_BYTES + 1) throw AssertionError("Read beyond the size guard")
                return 32
            }
        }
        try { BackupDocuments(context.contentResolver).read(stream); fail("Must reject an oversized document") }
        catch (_: IOException) { assertEquals(TaskBackup.MAX_BYTES + 1, count) }
    }

    @Test fun aUserChosenDocumentRoundTripsEveryTaskWithoutChangingItsLanguage() {
        val file = File.createTempFile("taskomatic-backup-", ".json", context.cacheDir)
        try {
            val snapshot = TaskBackup(listOf(TaskItem(title = "Acheter du café ☕", note = "日本語 aussi",
                createdAt = Instant.parse("2026-09-08T12:00:00Z"))), Instant.parse("2026-09-08T13:00:00Z"))
            val documents = BackupDocuments(context.contentResolver)
            documents.write(Uri.fromFile(file), snapshot)
            assertEquals(snapshot, documents.read(Uri.fromFile(file)))
        } finally { file.delete() }
    }
}
