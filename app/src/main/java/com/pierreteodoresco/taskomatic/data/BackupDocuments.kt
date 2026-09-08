package com.pierreteodoresco.taskomatic.data

import android.content.ContentResolver
import android.net.Uri
import com.pierreteodoresco.taskomatic.core.TaskBackup
import java.io.IOException
import java.io.InputStream
import java.io.ByteArrayOutputStream

class BackupDocuments(private val resolver: ContentResolver) {
    fun read(uri: Uri): TaskBackup = resolver.openInputStream(uri)?.use(::read)
        ?: throw IOException("Document provider returned no input stream")

    fun read(stream: InputStream): TaskBackup {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = stream.read(buffer, 0, minOf(buffer.size, TaskBackup.MAX_BYTES + 1 - bytes.size()))
            if (count < 0) break
            if (count == 0) {
                val next = stream.read()
                if (next < 0) break
                bytes.write(next)
            } else bytes.write(buffer, 0, count)
            if (bytes.size() > TaskBackup.MAX_BYTES) throw IOException("Backup exceeds the 10 MiB limit")
        }
        return TaskBackup.decode(bytes.toByteArray())
    }

    fun write(uri: Uri, backup: TaskBackup) {
        val bytes = backup.encoded()
        val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Document provider returned no output stream")
        stream.use { it.write(bytes) }
    }
}
