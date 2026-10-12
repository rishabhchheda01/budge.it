package com.rishabh.budgetapp

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The tricky cases: year and month boundaries, daylight-saving days, and unusual budgets. */
class BudgetEdgeCaseTest {
    private val newYork = ZoneId.of("America/New_York")

    // ---------- weeks ----------

    @Test
    fun weekStartsOnSundayAcrossTheNewYear() {
        val sunday = LocalDate.of(2026, 12, 27)
        assertEquals(sunday, periodStartDate(LocalDate.of(2026, 12, 31), BudgetPeriod.WEEKLY)) // Thursday
        assertEquals(sunday, periodStartDate(LocalDate.of(2027, 1, 2), BudgetPeriod.WEEKLY))   // Saturday
        assertEquals(LocalDate.of(2027, 1, 3), periodStartDate(LocalDate.of(2027, 1, 3), BudgetPeriod.WEEKLY)) // next Sunday
    }

    @Test
    fun weekStartsOnSundayAroundALeapDay() {
        assertEquals(LocalDate.of(2028, 2, 27), periodStartDate(LocalDate.of(2028, 2, 29), BudgetPeriod.WEEKLY))
        assertEquals(LocalDate.of(2028, 2, 27), periodStartDate(LocalDate.of(2028, 3, 4), BudgetPeriod.WEEKLY))
    }

    @Test
    fun daysLeftCountsDownFromSevenToOneAcrossTheWeek() {
        val sunday = LocalDate.of(2026, 10, 4)
        val expected = listOf(7, 6, 5, 4, 3, 2, 1)
        expected.forEachIndexed { offset, days ->
            assertEquals("day +$offset", days, daysLeftInWeek(sunday.plusDays(offset.toLong())))
        }
        assertEquals(7, daysLeftInWeek(sunday.plusDays(7))) // the next Sunday starts a new week
    }

    // ---------- daylight saving ----------

    @Test
    fun springForwardDayIsTwentyThreeHoursLong() {
        // 2026-03-08: clocks jump from 2:00 to 3:00. From 00:30 there are 22.5 hours until the next midnight.
        val now = ZonedDateTime.of(2026, 3, 8, 0, 30, 0, 0, newYork)
        assertEquals((22.5 * 3_600_000).toLong(), millisUntilNextDay(now))
    }

    @Test
    fun fallBackDayIsTwentyFiveHoursLong() {
        // 2026-11-01: clocks go back from 2:00 to 1:00. From 00:30 there are 24.5 hours until the next midnight.
        val now = ZonedDateTime.of(2026, 11, 1, 0, 30, 0, 0, newYork)
        assertEquals((24.5 * 3_600_000).toLong(), millisUntilNextDay(now))
    }

    @Test
    fun periodStartIsMidnightEvenWhenTheWeekContainsADaylightSavingChange() {
        // Wednesday after the spring-forward Sunday: the week still starts at midnight on Sunday Mar 8.
        val start = Instant.ofEpochMilli(
            periodStartMillis(LocalDate.of(2026, 3, 11), BudgetPeriod.WEEKLY, newYork)
        ).atZone(newYork)
        assertEquals(LocalDate.of(2026, 3, 8).atStartOfDay(newYork), start)
        assertEquals(0, start.hour)
    }

    @Test
    fun dayStartIsMidnightOnADaylightSavingDay() {
        val start = Instant.ofEpochMilli(dayStartMillis(LocalDate.of(2026, 11, 1), newYork)).atZone(newYork)
        assertEquals(LocalDate.of(2026, 11, 1).atStartOfDay(newYork), start)
    }

    @Test
    fun pruneCutoffIsMidnightOneMonthAgoEvenWhenTheClocksChangeInBetween() {
        // Today is Mar 9; the clocks changed on Mar 8. One month back is Feb 9, at midnight New York time.
        val cutoff = Instant.ofEpochMilli(pruneCutoffMillis(LocalDate.of(2026, 3, 9), newYork)).atZone(newYork)
        assertEquals(LocalDate.of(2026, 2, 9).atStartOfDay(newYork), cutoff)
    }

    // ---------- summary edge cases ----------

    @Test
    fun saturdayHasOneDayLeftSoTheWholeRemainderIsToday() {
        val saturday = LocalDate.of(2026, 10, 10)
        val summary = BudgetSummary.of(BudgetPeriod.WEEKLY, 700.0, spent = 400.0, spentToday = 0.0, today = saturday)
        assertEquals(300.0, summary.limit, 0.001)
        assertEquals(1, summary.daysLeft)
    }

    @Test
    fun alreadyOverTheWeeklyBudgetGivesANegativeLimitAndAFullBar() {
        val wednesday = LocalDate.of(2026, 10, 7)
        // 100 budget, 140 spent before today, 4 days left: (100 - 140) / 4 = -10.
        val summary = BudgetSummary.of(BudgetPeriod.WEEKLY, 100.0, spent = 140.0, spentToday = 0.0, today = wednesday)
        assertEquals(-10.0, summary.limit, 0.001)
        assertEquals(-10.0, summary.left, 0.001)
        assertEquals(1f, summary.progress, 0.001f)
    }

    @Test
    fun aZeroLimitWithNothingSpentShowsAnEmptyBar() {
        val saturday = LocalDate.of(2026, 10, 10)
        // Everything was spent before today, so today's limit is exactly 0.
        val summary = BudgetSummary.of(BudgetPeriod.WEEKLY, 100.0, spent = 100.0, spentToday = 0.0, today = saturday)
        assertEquals(0.0, summary.limit, 0.001)
        assertEquals(0f, summary.progress, 0.001f)
    }

    @Test
    fun spendingAgainstAZeroLimitFillsTheBar() {
        val saturday = LocalDate.of(2026, 10, 10)
        val summary = BudgetSummary.of(BudgetPeriod.WEEKLY, 100.0, spent = 105.0, spentToday = 5.0, today = saturday)
        assertEquals(0.0, summary.limit, 0.001)
        assertEquals(-5.0, summary.left, 0.001)
        assertEquals(1f, summary.progress, 0.001f)
    }

    @Test
    fun dailyBudgetIgnoresEarlierSpendingBecauseOnlyTodayCounts() {
        val wednesday = LocalDate.of(2026, 10, 7)
        val summary = BudgetSummary.of(BudgetPeriod.DAILY, 50.0, spent = 20.0, spentToday = 20.0, today = wednesday)
        assertEquals(50.0, summary.limit, 0.001)
        assertEquals(30.0, summary.left, 0.001)
        assertEquals(0.4f, summary.progress, 0.001f)
    }

    @Test
    fun progressNeverLeavesZeroToOne() {
        val wednesday = LocalDate.of(2026, 10, 7)
        for (spentToday in listOf(0.0, 0.01, 10.0, 17.5, 1000.0)) {
            val progress = BudgetSummary.of(BudgetPeriod.WEEKLY, 100.0, spentToday, spentToday, wednesday).progress
            assertTrue("progress $progress for $spentToday", progress in 0f..1f)
        }
    }

    // ---------- money formatting ----------

    @Test
    fun moneyIsFormattedAsUsDollars() {
        assertEquals("$1,234.50", currencyFormat().format(1234.5))
        assertEquals("$0.00", currencyFormat().format(0.0))
        assertEquals("-$12.00", currencyFormat().format(-12.0))
    }
}
