package com.pierreteodoresco.taskomatic.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pierreteodoresco.taskomatic.core.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

class TaskRepository(
    context: Context,
    databaseName: String = "taskomatic.db",
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) : AutoCloseable {
    private val helper = object : SQLiteOpenHelper(context.applicationContext, databaseName, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""CREATE TABLE tasks (
                id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, note TEXT NOT NULL,
                created_at INTEGER NOT NULL, completed_at INTEGER,
                recurrence_interval INTEGER, recurrence_unit TEXT, cycle_id TEXT NOT NULL
            )""")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Missing database migration from $oldVersion to $newVersion")
        }
    }

    fun tasks(): List<TaskItem> = helper.readableDatabase.query(
        "tasks", null, null, null, null, null, "created_at DESC, id ASC"
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.task()) } }

    fun add(title: String, note: String = "", recurrence: Recurrence? = null): TaskItem {
        require(title.isNotBlank())
        val item = TaskItem(title = title.trim(), note = note,
            createdAt = clock.instant().truncatedTo(ChronoUnit.MILLIS), recurrence = recurrence)
        helper.writableDatabase.insertOrThrow("tasks", null, item.values())
        return item
    }

    fun edit(original: TaskItem, title: String, note: String, recurrence: Recurrence?): TaskItem = transaction { db ->
        require(title.isNotBlank())
        val fresh = find(db, original.id) ?: throw IllegalStateException("Task no longer exists")
        var edited = fresh.copy(
            title = if (title.trim() != original.title) title.trim() else fresh.title,
            note = if (note != original.note) note else fresh.note,
            recurrence = if (recurrence != original.recurrence) recurrence else fresh.recurrence,
        )
        if (recurrence != original.recurrence && fresh.isActive(clock.instant(), zone())) {
            edited = edited.restore()
        }
        db.update("tasks", edited.values(), "id = ?", arrayOf(original.id.toString()))
        edited
    }

    fun complete(id: UUID, expectedCycle: UUID? = null): CompletionUndo? = transaction { db ->
        val item = find(db, id) ?: return@transaction null
        if (expectedCycle != null && expectedCycle != item.cycleId) return@transaction null
        val now = clock.instant().truncatedTo(ChronoUnit.MILLIS)
        if (!item.isActive(now, zone())) return@transaction null
        val completed = item.complete(now)
        db.update("tasks", completed.values(), "id = ?", arrayOf(id.toString()))
        CompletionUndo(id, item.completedAt, completed.cycleId)
    }

    fun restore(id: UUID, expectedCycle: UUID): Boolean = transaction { db ->
        val item = find(db, id) ?: return@transaction false
        if (item.cycleId != expectedCycle || item.completedAt == null) return@transaction false
        db.update("tasks", item.restore().values(), "id = ?", arrayOf(id.toString()))
        true
    }

    fun undo(undo: CompletionUndo): Boolean = transaction { db ->
        val item = find(db, undo.id) ?: return@transaction false
        if (item.cycleId != undo.completedCycle) return@transaction false
        val restored = item.copy(completedAt = undo.previousCompletion, cycleId = UUID.randomUUID())
        db.update("tasks", restored.values(), "id = ?", arrayOf(item.id.toString()))
        true
    }

    fun importBackup(backup: TaskBackup): Int = transaction { db ->
        var inserted = 0
        for (item in backup.items) {
            if (find(db, item.id) != null) continue
            db.insertOrThrow("tasks", null, item.copy(cycleId = UUID.randomUUID()).values())
            inserted++
        }
        inserted
    }

    fun delete(id: UUID): Boolean =
        helper.writableDatabase.delete("tasks", "id = ?", arrayOf(id.toString())) > 0

    private fun find(db: SQLiteDatabase, id: UUID): TaskItem? = db.query(
        "tasks", null, "id = ?", arrayOf(id.toString()), null, null, null
    ).use { if (it.moveToFirst()) it.task() else null }

    private fun <T> transaction(block: (SQLiteDatabase) -> T): T {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val result = block(db)
            db.setTransactionSuccessful()
            return result
        } finally { db.endTransaction() }
    }

    override fun close() = helper.close()
}

data class CompletionUndo(val id: UUID, val previousCompletion: Instant?, val completedCycle: UUID)

private fun TaskItem.values() = ContentValues().apply {
    put("id", id.toString()); put("title", title); put("note", note)
    put("created_at", createdAt.toEpochMilli()); put("completed_at", completedAt?.toEpochMilli())
    put("recurrence_interval", recurrence?.interval); put("recurrence_unit", recurrence?.unit?.name)
    put("cycle_id", cycleId.toString())
}

private fun Cursor.task(): TaskItem {
    fun string(name: String) = getString(getColumnIndexOrThrow(name))
    fun instant(name: String): Instant? = getColumnIndexOrThrow(name).let {
        if (isNull(it)) null else Instant.ofEpochMilli(getLong(it))
    }
    val intervalIndex = getColumnIndexOrThrow("recurrence_interval")
    return TaskItem(UUID.fromString(string("id")), string("title"), string("note"),
        instant("created_at")!!, instant("completed_at"),
        if (isNull(intervalIndex)) null else Recurrence(getInt(intervalIndex), RecurrenceUnit.valueOf(string("recurrence_unit"))),
        UUID.fromString(string("cycle_id")))
}
