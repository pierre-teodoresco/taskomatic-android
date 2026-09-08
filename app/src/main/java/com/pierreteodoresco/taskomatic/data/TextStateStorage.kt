package com.pierreteodoresco.taskomatic.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID
import com.pierreteodoresco.taskomatic.core.TaskItem
import com.pierreteodoresco.taskomatic.core.Recurrence
import com.pierreteodoresco.taskomatic.core.RecurrenceUnit

/** Large UI state stays private on disk, not in Android's size-limited Binder Bundle. */
class TextStateStorage(context: Context) {
    private val directory = File(context.filesDir, "editor-state")

    fun saveSnapshot(item: TaskItem, key: String): ArrayList<String> = try {
        arrayListOf(item.id.toString(),
            save(item.title, "$key-original-title"), save(item.note, "$key-original-note"), item.createdAt.toString(),
            item.completedAt?.toString().orEmpty(), item.recurrence?.interval?.toString().orEmpty(),
            item.recurrence?.unit?.name.orEmpty(), item.cycleId.toString())
    } catch (error: Exception) {
        removeEditor(key)
        throw error
    }

    fun restoreSnapshot(snapshot: ArrayList<String>) = with(snapshot) {
        TaskItem(UUID.fromString(this[0]), restore(this[1]), restore(this[2]), Instant.parse(this[3]),
            this[4].takeIf { it.isNotEmpty() }?.let(Instant::parse),
            this[5].takeIf { it.isNotEmpty() }?.let { Recurrence(it.toInt(), RecurrenceUnit.valueOf(this[6])) }, UUID.fromString(this[7]))
    }

    fun save(text: String, key: String): String {
        if (text.length <= INLINE_LIMIT) return "T$text"
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create draft storage" }
        val file = file(key)
        val stream = file.startWrite()
        try { stream.write(text.encodeToByteArray()); file.finishWrite(stream) }
        catch (error: Exception) { file.failWrite(stream); throw error }
        return "F$key"
    }

    fun restore(value: String): String = when (value.firstOrNull()) {
        'T' -> value.drop(1)
        'F' -> file(value.drop(1)).openRead().use { it.readBytes().decodeToString(throwOnInvalidSequence = true) }
        else -> throw IOException("Draft state could not be recovered")
    }

    fun removeEditor(key: String) {
        for (field in listOf("original-title", "original-note", "draft-title", "draft-note", "draft-interval")) file("$key-$field").delete()
    }

    private fun file(key: String): AtomicFile {
        require(key.matches(Regex("[a-z0-9-]{1,100}")))
        return AtomicFile(File(directory, key))
    }

    companion object { const val INLINE_LIMIT = 8192 }
}
