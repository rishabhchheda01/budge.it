package com.rishabh.budgetapp

import org.junit.Assert.assertEquals
import org.junit.Test

// Tests for switching a budget between Daily and Weekly (a week is 7 days).
class ConvertBudgetTest {
    @Test
    fun dailyToWeeklyMultipliesBySeven() {
        assertEquals(700.0, convertBudget(100.0, BudgetPeriod.DAILY, BudgetPeriod.WEEKLY), 0.0001)
    }

    @Test
    fun weeklyToDailyDividesBySeven() {
        assertEquals(10.0, convertBudget(70.0, BudgetPeriod.WEEKLY, BudgetPeriod.DAILY), 0.0001)
    }

    @Test
    fun samePeriodChangesNothing() {
        assertEquals(55.5, convertBudget(55.5, BudgetPeriod.DAILY, BudgetPeriod.DAILY), 0.0)
    }

    @Test
    fun switchingThereAndBackGivesTheStartingNumberAgain() {
        // 100 / 7 = 14.2857...: not rounded, so there is no drift.
        val daily = convertBudget(100.0, BudgetPeriod.WEEKLY, BudgetPeriod.DAILY)
        assertEquals(100.0, convertBudget(daily, BudgetPeriod.DAILY, BudgetPeriod.WEEKLY), 0.0000001)
    }
}
