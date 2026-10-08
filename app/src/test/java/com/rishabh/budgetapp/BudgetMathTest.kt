package com.rishabh.budgetapp

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetMathTest {
    // 2026-10-04 is a Sunday, 2026-10-07 a Wednesday, 2026-10-10 a Saturday.
    private val sunday = LocalDate.of(2026, 10, 4)
    private val wednesday = LocalDate.of(2026, 10, 7)
    private val saturday = LocalDate.of(2026, 10, 10)
    private val utc = ZoneId.of("UTC")

    @Test
    fun dailyPeriodStartsToday() {
        assertEquals(wednesday, periodStartDate(wednesday, BudgetPeriod.DAILY))
    }

    @Test
    fun weeklyPeriodStartsOnTheMostRecentSunday() {
        assertEquals(sunday, periodStartDate(sunday, BudgetPeriod.WEEKLY))
        assertEquals(sunday, periodStartDate(wednesday, BudgetPeriod.WEEKLY))
        assertEquals(sunday, periodStartDate(saturday, BudgetPeriod.WEEKLY))
        // The day after Saturday starts a new week.
        assertEquals(LocalDate.of(2026, 10, 11), periodStartDate(saturday.plusDays(1), BudgetPeriod.WEEKLY))
    }

    @Test
    fun daysLeftIncludesToday() {
        assertEquals(7, daysLeftInWeek(sunday))
        assertEquals(4, daysLeftInWeek(wednesday))
        assertEquals(1, daysLeftInWeek(saturday))
    }

    @Test
    fun pruneCutoffKeepsThisWeekAndLastWeek() {
        // This week starts Sunday Oct 4, so we keep everything from Sunday Sep 27 on.
        val expected = LocalDate.of(2026, 9, 27).atStartOfDay(utc).toInstant().toEpochMilli()
        assertEquals(expected, pruneCutoffMillis(wednesday, utc))
    }

    @Test
    fun periodStartMillisIsMidnightInTheGivenZone() {
        val expected = sunday.atStartOfDay(utc).toInstant().toEpochMilli()
        assertEquals(expected, periodStartMillis(wednesday, BudgetPeriod.WEEKLY, utc))
    }

    // These two tests check the function you write.
    @Test
    fun dailyAllowanceSplitsWhatIsLeftAcrossTheDaysLeft() {
        assertEquals(10.0, dailyAllowance(70.0, 7), 0.001)
        assertEquals(25.0, dailyAllowance(25.0, 1), 0.001)
    }

    @Test
    fun dailyAllowanceIsNegativeWhenOverBudget() {
        assertEquals(-7.0, dailyAllowance(-14.0, 2), 0.001)
    }

    @Test
    fun weeklySummaryUsesTheDailyAllowanceAsTheLimit() {
        // Wednesday: 4 days left. 100 budget, 40 spent this week, 10 of that today.
        // Left at the start of today = 100 - 30 = 70, so today's limit is 70 / 4 = 17.5.
        val summary = BudgetSummary.of(BudgetPeriod.WEEKLY, budget = 100.0, spent = 40.0, spentToday = 10.0, today = wednesday)
        assertEquals(17.5, summary.limit, 0.001)
        assertEquals(10.0, summary.spent, 0.001)
        assertEquals(7.5, summary.left, 0.001)
        assertEquals(10f / 17.5f, summary.progress, 0.001f)
        assertEquals(60.0, summary.weeklyLeft!!, 0.001)
        assertEquals(4, summary.daysLeft)
    }

    @Test
    fun dailySummaryUsesTheBudgetAsTheLimitAndCapsProgress() {
        val summary = BudgetSummary.of(BudgetPeriod.DAILY, budget = 20.0, spent = 30.0, spentToday = 30.0, today = wednesday)
        assertEquals(20.0, summary.limit, 0.001)
        assertEquals(-10.0, summary.left, 0.001)
        assertEquals(1f, summary.progress, 0.001f)
        assertNull(summary.weeklyLeft)
        assertNull(summary.daysLeft)
    }

    // The example: a 700 weekly budget.
    @Test
    fun dailyLimitStaysTheSameAllDayEvenAfterSpending() {
        val before = BudgetSummary.of(BudgetPeriod.WEEKLY, 700.0, spent = 0.0, spentToday = 0.0, today = sunday)
        val after = BudgetSummary.of(BudgetPeriod.WEEKLY, 700.0, spent = 100.0, spentToday = 100.0, today = sunday)
        assertEquals(100.0, before.limit, 0.001)
        assertEquals(100.0, after.limit, 0.001) // still 100 after spending 100
        assertEquals(100.0, before.left, 0.001)
        assertEquals(0.0, after.left, 0.001)    // but nothing is left today
        assertEquals(600.0, after.weeklyLeft!!, 0.001)
    }

    @Test
    fun dailyLimitIsRecalculatedAtTheStartOfEachDay() {
        val tuesday = LocalDate.of(2026, 10, 6) // 5 days left including Tuesday
        // Spent 100 on Sunday and 0 on Monday: 600 left / 5 days = 120.
        val summary = BudgetSummary.of(BudgetPeriod.WEEKLY, 700.0, spent = 100.0, spentToday = 0.0, today = tuesday)
        assertEquals(120.0, summary.limit, 0.001)
        assertEquals(120.0, summary.left, 0.001)
    }

    @Test
    fun overspendingTodayShowsNegativeAndFullBar() {
        val summary = BudgetSummary.of(BudgetPeriod.WEEKLY, 700.0, spent = 120.0, spentToday = 120.0, today = sunday)
        assertEquals(100.0, summary.limit, 0.001)
        assertEquals(-20.0, summary.left, 0.001)
        assertEquals(1f, summary.progress, 0.001f)
    }
}
