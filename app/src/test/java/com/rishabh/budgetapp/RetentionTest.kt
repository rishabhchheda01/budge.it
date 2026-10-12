package com.rishabh.budgetapp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

// The Insights page can go back 3 weeks, so the stored month must always reach back far enough.
class RetentionTest {
    private val utc = ZoneOffset.UTC

    @Test
    fun oneMonthOfDataAlwaysCoversThisWeekAndThe3WeeksBefore() {
        var day = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2030, 12, 31) // includes leap years and every month length
        while (!day.isAfter(end)) {
            val oldestWeekStart = periodStartDate(day, BudgetPeriod.WEEKLY).minusWeeks(3)
            val oldestNeeded = oldestWeekStart.atStartOfDay(utc).toInstant().toEpochMilli()
            assertTrue("on $day", pruneCutoffMillis(day, utc) <= oldestNeeded)
            day = day.plusDays(1)
        }
    }

    @Test
    fun theSameHoldsWithALateDayStartTime() {
        val rollover = LocalTime.of(5, 0)
        var day = LocalDate.of(2026, 1, 1)
        while (day.year == 2026) {
            val oldestWeekStart = periodStartDate(day, BudgetPeriod.WEEKLY).minusWeeks(3)
            val oldestNeeded = oldestWeekStart.atTime(rollover).atZone(utc).toInstant().toEpochMilli()
            assertTrue("on $day", pruneCutoffMillis(day, utc, rollover) <= oldestNeeded)
            day = day.plusDays(1)
        }
    }
}
