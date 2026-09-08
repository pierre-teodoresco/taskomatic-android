package com.pierreteodoresco.taskomatic.core

import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class TaskItem(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val note: String = "",
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val recurrence: Recurrence? = null,
    val cycleId: UUID = UUID.randomUUID(),
) {
    fun nextActivation(zone: ZoneId): Instant? = completedAt?.let { recurrence?.nextActivation(it, zone) }
    fun isActive(at: Instant, zone: ZoneId): Boolean =
        completedAt == null || nextActivation(zone)?.let { !at.isBefore(it) } == true
    fun complete(at: Instant): TaskItem = copy(completedAt = at, cycleId = UUID.randomUUID())
    fun restore(): TaskItem = copy(completedAt = null, cycleId = UUID.randomUUID())
}
