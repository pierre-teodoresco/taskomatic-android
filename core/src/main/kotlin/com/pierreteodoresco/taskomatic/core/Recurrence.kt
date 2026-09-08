package com.pierreteodoresco.taskomatic.core

import java.time.Instant
import java.time.ZoneId

enum class RecurrenceUnit { DAY, WEEK, MONTH }

data class Recurrence(val interval: Int, val unit: RecurrenceUnit) {
    init { require(interval in 1..99) }
    fun nextActivation(completed: Instant, zone: ZoneId): Instant {
        val day = completed.atZone(zone).toLocalDate()
        val next = when (unit) {
            RecurrenceUnit.DAY -> day.plusDays(interval.toLong())
            RecurrenceUnit.WEEK -> day.plusWeeks(interval.toLong())
            RecurrenceUnit.MONTH -> day.plusMonths(interval.toLong())
        }
        return next.atStartOfDay(zone).toInstant()
    }
}
