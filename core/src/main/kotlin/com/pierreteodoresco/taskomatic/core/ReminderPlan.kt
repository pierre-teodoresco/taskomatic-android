package com.pierreteodoresco.taskomatic.core

import java.time.*

data class ReminderSettings(
    val enabled: Boolean = false,
    val time: LocalTime = LocalTime.of(9, 0),
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
)

data class ReminderPlan(val at: Instant, val tasks: List<TaskItem>)

object ReminderPlanner {
    fun due(tasks: List<TaskItem>, settings: ReminderSettings, expectedAt: Instant, now: Instant, zone: ZoneId): List<TaskItem> {
        val today = now.atZone(zone).toLocalDate()
        if (expectedAt > now || expectedAt.atZone(zone).toLocalDate() != today) return emptyList()
        val plan = next(tasks, settings, today.atStartOfDay(zone).toInstant().minusNanos(1), zone)
        return if (plan?.at == expectedAt) tasks.filter { it.isActive(now, zone) } else emptyList()
    }

    fun next(tasks: List<TaskItem>, settings: ReminderSettings, after: Instant, zone: ZoneId): ReminderPlan? {
        if (!settings.enabled) return null
        val earliest = tasks.mapNotNull {
            if (it.isActive(after, zone)) after else it.nextActivation(zone)
        }.minOrNull() ?: return null
        val today = earliest.atZone(zone).toLocalDate()
        // A date-line change can skip an entire selected weekday; include its next occurrence.
        for (offset in 0L..14L) {
            val day = today.plusDays(offset)
            if (day.dayOfWeek !in settings.weekdays) continue
            val local = day.atTime(settings.time)
            val transition = zone.rules.getTransition(local)
            val at = (if (transition?.isGap == true) transition.dateTimeAfter else local)
                .atZone(zone).toInstant()
            if (at.atZone(zone).toLocalDate() != day) continue
            if (!at.isAfter(after)) continue
            val active = tasks.filter { it.isActive(at, zone) }
            if (active.isNotEmpty()) return ReminderPlan(at, active)
        }
        return null
    }
}
