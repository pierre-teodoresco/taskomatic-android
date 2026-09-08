package com.pierreteodoresco.taskomatic.core

import java.time.Instant
import java.util.UUID
import java.util.Collections
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TaskBackup(items: List<TaskItem>, val createdAt: Instant) {
    val items: List<TaskItem> = Collections.unmodifiableList(items.toList())
    override fun equals(other: Any?): Boolean = other is TaskBackup && items == other.items && createdAt == other.createdAt
    override fun hashCode(): Int = 31 * items.hashCode() + createdAt.hashCode()
    init {
        require(items.size <= 10_000 && items.map { it.id }.toSet().size == items.size)
        require(items.all { it.title.isNotBlank() })
        require(validDate(createdAt) && items.all {
            validDate(it.createdAt) && (it.completedAt?.let(::validDate) ?: true)
        })
    }
    fun encoded(): ByteArray = json.encodeToString(Envelope(
        "com.pierreteodoresco.taskomatic.backup", 1, createdAt.toEpochMilli().toDouble(),
        items.map { item ->
            BackupItem(item.id.toString(), item.title, item.note, item.createdAt.toEpochMilli().toDouble(),
                item.completedAt?.toEpochMilli()?.toDouble(),
                item.recurrence?.let { BackupRecurrence(it.interval, it.unit.name.lowercase()) },
                item.cycleId.toString())
        },
    )).encodeToByteArray().also { require(it.size <= MAX_BYTES) }

    companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
        private fun validDate(value: Instant) = value >= Instant.ofEpochMilli(-62135769600000L) &&
            value <= Instant.ofEpochMilli(64092211200000L)

        private fun date(value: Double): Instant {
            require(value.isFinite() && value in -62135769600000.0..64092211200000.0)
            return Instant.ofEpochMilli(value.toLong())
        }

        private fun uuid(value: String): UUID {
            val id = UUID.fromString(value)
            require(id.toString().equals(value, ignoreCase = true))
            return id
        }

        fun decode(bytes: ByteArray): TaskBackup {
            require(bytes.size <= MAX_BYTES)
            val document = json.decodeFromString<Envelope>(bytes.decodeToString(throwOnInvalidSequence = true))
            require(document.format == "com.pierreteodoresco.taskomatic.backup" && document.version == 1)
            return TaskBackup(document.items.map { item ->
                TaskItem(uuid(item.id), item.title, item.note, date(item.createdAt),
                    item.completedAt?.let(::date),
                    item.recurrence?.let { recurrence ->
                        Recurrence(recurrence.interval, RecurrenceUnit.entries.single { it.name.lowercase() == recurrence.unit })
                    }, uuid(item.cycleID))
            }, date(document.createdAt))
        }
    }
}

@Serializable
private data class Envelope(
    val format: String,
    val version: Int,
    val createdAt: Double,
    val items: List<BackupItem>,
)

@Serializable
private data class BackupItem(
    val id: String,
    val title: String,
    val note: String,
    val createdAt: Double,
    val completedAt: Double? = null,
    val recurrence: BackupRecurrence? = null,
    val cycleID: String,
)

@Serializable
private data class BackupRecurrence(val interval: Int, val unit: String)
