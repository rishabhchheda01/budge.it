package com.rishabh.budgetapp

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// Tests for the second (weekly) progress bar. Both budget types have a weekly budget:
// WEEKLY = what you set, DAILY = your daily budget x 7.
class WeeklyBarTest {
    private val wednesday = LocalDate.of(2026, 10, 7)

    @Test
    fun aDailyBudgetHasAWeeklyBudgetOfSevenDays() {
        // 20 a day = 140 a week. 35 spent this week, 5 of it today.
        val summary = BudgetSummary.of(BudgetPeriod.DAILY, 20.0, spent = 35.0, spentToday = 5.0, today = wednesday)
        assertEquals(105.0, summary.weeklyLeft, 0.001)      // 140 - 35
        assertEquals(35.0 / 140.0, summary.weeklyProgress.toDouble(), 0.001)
        assertEquals(4, summary.daysLeft)
    }

    @Test
    fun theDailyBarOfADailyBudgetIsStillAboutToday() {
        val summary = BudgetSummary.of(BudgetPeriod.DAILY, 20.0, spent = 35.0, spentToday = 5.0, today = wednesday)
        assertEquals(15.0, summary.left, 0.001)
        assertEquals(0.25f, summary.progress, 0.001f)
    }

    @Test
    fun aWeeklyBudgetsWeeklyBarIsSpentOverTheBudget() {
        val summary = BudgetSummary.of(BudgetPeriod.WEEKLY, 100.0, spent = 40.0, spentToday = 10.0, today = wednesday)
        assertEquals(0.4f, summary.weeklyProgress, 0.001f)
        assertEquals(60.0, summary.weeklyLeft, 0.001)
    }

    @Test
    fun theWeeklyBarIsFullWhenTheWeekIsOverBudget() {
        val daily = BudgetSummary.of(BudgetPeriod.DAILY, 10.0, spent = 100.0, spentToday = 0.0, today = wednesday)
        assertEquals(1f, daily.weeklyProgress, 0f)
        assertEquals(-30.0, daily.weeklyLeft, 0.001) // 70 - 100
        val weekly = BudgetSummary.of(BudgetPeriod.WEEKLY, 100.0, spent = 140.0, spentToday = 0.0, today = wednesday)
        assertEquals(1f, weekly.weeklyProgress, 0f)
    }

    @Test
    fun theWeeklyBarIsEmptyWhenNothingIsSpent() {
        val summary = BudgetSummary.of(BudgetPeriod.DAILY, 20.0, spent = 0.0, spentToday = 0.0, today = wednesday)
        assertEquals(0f, summary.weeklyProgress, 0f)
        assertEquals(140.0, summary.weeklyLeft, 0.001)
    }
}
