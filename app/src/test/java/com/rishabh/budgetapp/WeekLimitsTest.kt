package com.rishabh.budgetapp

import com.rishabh.budgetapp.data.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

// Tests for the Insights graph numbers: each day's limit and each day's spending.
class WeekLimitsTest {
    private val utc: ZoneId = ZoneOffset.UTC
    private val sunday = LocalDate.of(2026, 10, 4)

    private fun spend(date: LocalDate, hour: Int, amount: Double) = Transaction(
        label = "x",
        amount = amount,
        timestamp = date.atTime(hour, 0).atZone(utc).toInstant().toEpochMilli()
    )

    // ---------- weekLimits ----------

    @Test
    fun dailyBudgetIsTheSameEveryDay() {
        val limits = weekLimits(BudgetPeriod.DAILY, 40.0, List(7) { 100.0 })
        assertEquals(List(7) { 40.0 }, limits)
    }

    @Test
    fun weeklyLimitStaysEvenWhenYouSpendExactlyEachDaysLimit() {
        val limits = weekLimits(BudgetPeriod.WEEKLY, 700.0, List(7) { 100.0 })
        assertEquals(List(7) { 100.0 }, limits)
    }

    @Test
    fun weeklyLimitGrowsWhenYouSpendNothing() {
        // Nothing spent: Sun 700 / 7 = 100, Mon 700 / 6, ... Sat 700 / 1
        val limits = weekLimits(BudgetPeriod.WEEKLY, 700.0, List(7) { 0.0 })
        assertEquals(100.0, limits[0], 0.001)
        assertEquals(700.0 / 6, limits[1], 0.001)
        assertEquals(700.0, limits[6], 0.001)
    }

    @Test
    fun futureDaysKeepTodaysLimit() {
        // 100 weekly, it is Tuesday (index 2), 10 spent on Sunday and Monday, nothing yet today:
        // Sun 100/7, Mon 90/6 = 15, Tue 80/5 = 16, and Wed-Sat stay at 16.
        val spent = listOf(10.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val limits = weekLimits(BudgetPeriod.WEEKLY, 100.0, spent, todayIndex = 2)
        assertEquals(100.0 / 7, limits[0], 0.001)
        assertEquals(15.0, limits[1], 0.001)
        assertEquals(16.0, limits[2], 0.001)
        for (day in 3..6) assertEquals(16.0, limits[day], 0.001)
    }

    @Test
    fun weeklyLimitIsWorkedOutFromWhatWasLeftAtTheStartOfEachDay() {
        // 700 weekly: Sun 100, spend 100 -> Mon 100, spend 0 -> Tue 600 / 5 = 120
        val limits = weekLimits(BudgetPeriod.WEEKLY, 700.0, listOf(100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        assertEquals(100.0, limits[0], 0.001)
        assertEquals(100.0, limits[1], 0.001)
        assertEquals(120.0, limits[2], 0.001)
    }

    @Test
    fun overspendingLowersTheNextDaysAndCanGoNegative() {
        // 70 weekly: Sun limit 10, spend 80 -> -10 left, Mon = -10 / 6
        val limits = weekLimits(BudgetPeriod.WEEKLY, 70.0, listOf(80.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        assertEquals(10.0, limits[0], 0.001)
        assertEquals(-10.0 / 6, limits[1], 0.001)
    }

    // ---------- spentPerDay ----------

    @Test
    fun spendingIsAddedUpPerDaySundayFirst() {
        val transactions = listOf(
            spend(sunday, 9, 5.0),
            spend(sunday, 20, 7.0),
            spend(sunday.plusDays(2), 12, 3.0),
            spend(sunday.plusDays(6), 23, 1.0)
        )
        val result = spentPerDay(transactions, sunday, utc)
        assertEquals(listOf(12.0, 0.0, 3.0, 0.0, 0.0, 0.0, 1.0), result)
    }

    @Test
    fun spendingOutsideTheWeekIsIgnored() {
        val transactions = listOf(
            spend(sunday.minusDays(1), 12, 50.0),
            spend(sunday.plusDays(7), 12, 60.0)
        )
        assertEquals(List(7) { 0.0 }, spentPerDay(transactions, sunday, utc))
    }

    @Test
    fun spendingBeforeTheRolloverCountsTowardTheDayBefore() {
        // Monday 02:00 with a 05:00 rollover still belongs to Sunday.
        val transactions = listOf(spend(sunday.plusDays(1), 2, 9.0))
        val result = spentPerDay(transactions, sunday, utc, LocalTime.of(5, 0))
        assertEquals(9.0, result[0], 0.001)
        assertEquals(0.0, result[1], 0.001)
    }
}
