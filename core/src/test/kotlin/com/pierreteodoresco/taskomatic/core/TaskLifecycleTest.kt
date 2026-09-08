package com.pierreteodoresco.taskomatic.core

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class TaskLifecycleTest {
    private val now = Instant.parse("2026-09-08T08:00:00Z")
    private val zone = ZoneId.of("Europe/Paris")

    @Test fun midnightDstDoesNotCarryOneAmIntoFollowingDays() {
        val santiago = ZoneId.of("America/Santiago")
        assertEquals(Instant.parse("2026-09-07T03:00:00Z"), Recurrence(2, RecurrenceUnit.DAY)
            .nextActivation(Instant.parse("2026-09-05T20:00:00Z"), santiago))
    }

    @Test fun recurrenceRejectsIntervalsOutsideThePortableRange() {
        assertThrows(IllegalArgumentException::class.java) { Recurrence(0, RecurrenceUnit.DAY) }
        assertThrows(IllegalArgumentException::class.java) { Recurrence(100, RecurrenceUnit.MONTH) }
    }

    @Test fun weeklyTaskReturnsNextTuesdayAndMissedCyclesDoNotAccumulate() {
        val task = TaskItem(title = "Lessive", createdAt = now,
            recurrence = Recurrence(1, RecurrenceUnit.WEEK)).complete(now)
        assertEquals(Instant.parse("2026-09-14T22:00:00Z"), task.nextActivation(zone))
        val late = task.complete(Instant.parse("2026-10-01T14:00:00Z"))
        assertEquals(Instant.parse("2026-10-07T22:00:00Z"), late.nextActivation(zone))
    }

    @Test fun calendarMonthClampsJanuary31InOrdinaryAndLeapYears() {
        val monthly = Recurrence(1, RecurrenceUnit.MONTH)
        assertEquals(Instant.parse("2026-02-27T23:00:00Z"),
            monthly.nextActivation(Instant.parse("2026-01-31T17:30:00Z"), zone))
        assertEquals(Instant.parse("2028-02-28T23:00:00Z"),
            monthly.nextActivation(Instant.parse("2028-01-31T17:30:00Z"), zone))
    }

    @Test fun dailyTaskReturnsAtLocalMidnightAcrossSpringClockChange() {
        val task = TaskItem(title = "Arroser", createdAt = now,
            recurrence = Recurrence(2, RecurrenceUnit.DAY))
            .complete(Instant.parse("2026-03-28T15:00:00Z"))
        assertEquals(Instant.parse("2026-03-29T22:00:00Z"), task.nextActivation(zone))
        assertFalse(task.isActive(Instant.parse("2026-03-29T21:59:59Z"), zone))
        assertTrue(task.isActive(Instant.parse("2026-03-29T22:00:00Z"), zone))
    }

    @Test fun simpleTaskCanBeCompletedAndRestoredWithoutLosingItsContent() {
        val task = TaskItem(title = "Acheter du café", note = "En grains", createdAt = now)
        assertTrue(task.isActive(now, zone))
        val done = task.complete(now)
        assertFalse(done.isActive(Instant.parse("2028-01-01T00:00:00Z"), zone))
        assertNotEquals(task.cycleId, done.cycleId)
        val restored = done.restore()
        assertTrue(restored.isActive(now, zone))
        assertEquals("En grains", restored.note)
        assertNotEquals(done.cycleId, restored.cycleId)
    }
}
