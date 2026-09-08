package com.pierreteodoresco.taskomatic.core

import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ReminderPlanTest {
    private val zone = ZoneId.of("Europe/Paris")
    private val now = Instant.parse("2026-09-08T08:00:00Z")

    @Test fun aDelayedAlarmOnlyDeliversItsStillValidLocalDayAndTime() {
        val task = TaskItem(title = "Coffee", createdAt = Instant.parse("2026-09-07T10:00:00Z"))
        val settings = ReminderSettings(true, LocalTime.of(9, 0), setOf(DayOfWeek.TUESDAY))
        val expected = Instant.parse("2026-09-08T07:00:00Z")
        assertEquals(listOf(task), ReminderPlanner.due(listOf(task), settings, expected, now, zone))
        assertTrue(ReminderPlanner.due(listOf(task), settings, expected, Instant.parse("2026-09-08T06:59:00Z"), zone).isEmpty())
        assertTrue(ReminderPlanner.due(listOf(task), settings, expected, Instant.parse("2026-09-08T22:30:00Z"), zone).isEmpty())
        assertTrue(ReminderPlanner.due(listOf(task), settings.copy(time = LocalTime.of(10, 0)), expected, now, zone).isEmpty())
        assertTrue(ReminderPlanner.due(listOf(task.complete(now)), settings, expected, now, zone).isEmpty())
        assertTrue(ReminderPlanner.due(listOf(task), settings.copy(enabled = false), expected, now, zone).isEmpty())
    }

    @Test fun aSkippedCalendarDayDoesNotSendOnAnUnselectedWeekday() {
        val plan = ReminderPlanner.next(listOf(TaskItem(title = "Friday", createdAt = now)),
            ReminderSettings(true, LocalTime.of(9, 0), setOf(DayOfWeek.FRIDAY)),
            Instant.parse("2011-12-29T10:00:00Z"), ZoneId.of("Pacific/Apia"))!!
        assertEquals(Instant.parse("2012-01-05T19:00:00Z"), plan.at)
    }

    @Test fun emptyAndArchivedListsOrNoWeekdaysProduceNoReminder() {
        assertNull(ReminderPlanner.next(emptyList(), ReminderSettings(true), now, zone))
        val task = TaskItem(title = "Café", createdAt = now)
        assertNull(ReminderPlanner.next(listOf(task.complete(now)), ReminderSettings(true), now, zone))
        assertNull(ReminderPlanner.next(listOf(task), ReminderSettings(true, weekdays = emptySet()), now, zone))
    }

    @Test fun reminderInMissingSpringTimeMovesToFirstValidTime() {
        val task = TaskItem(title = "Café", createdAt = now)
        val plan = ReminderPlanner.next(listOf(task),
            ReminderSettings(true, LocalTime.of(2, 30), setOf(DayOfWeek.SUNDAY)),
            Instant.parse("2026-03-28T12:00:00Z"), zone)!!
        assertEquals(Instant.parse("2026-03-29T01:00:00Z"), plan.at)
    }

    @Test fun aSleepingTaskIsScheduledOnReturnEvenYearsAway() {
        val task = TaskItem(title = "Archives", createdAt = now,
            recurrence = Recurrence(99, RecurrenceUnit.MONTH)).complete(now)
        val plan = ReminderPlanner.next(listOf(task), ReminderSettings(enabled = true), now, zone)!!
        assertEquals(Instant.parse("2034-12-08T08:00:00Z"), plan.at)
        assertEquals(listOf(task), plan.tasks)
    }

    @Test fun disabledRemindersNeverScheduleEvenWithActiveTasks() {
        assertNull(ReminderPlanner.next(listOf(TaskItem(title = "Café", createdAt = now)),
            ReminderSettings(enabled = false), now, zone))
    }

    @Test fun nextReminderHonorsSelectedWeekdayAndIncludesOnlyActiveTasks() {
        val active = TaskItem(title = "Café", createdAt = now)
        val archived = TaskItem(title = "Fait", createdAt = now).complete(now)
        val settings = ReminderSettings(true, LocalTime.of(9, 0), setOf(DayOfWeek.WEDNESDAY))
        val plan = ReminderPlanner.next(listOf(active, archived), settings, now, zone)!!
        assertEquals(Instant.parse("2026-09-09T07:00:00Z"), plan.at)
        assertEquals(listOf(active), plan.tasks)
    }
}
