package com.rishabh.budgetapp

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

// Tests for the "day starts at" setting. A 05:00 rollover means the budget day runs 5 am to 5 am.
class RolloverTest {
    private val utc: ZoneId = ZoneOffset.UTC
    private val fiveAm = LocalTime.of(5, 0)
    private val tuesday = LocalDate.of(2026, 10, 6)
    private val monday = LocalDate.of(2026, 10, 5)

    private fun at(date: LocalDate, hour: Int, minute: Int) = ZonedDateTime.of(date, LocalTime.of(hour, minute), utc)

    // ---------- budgetDate (the part you write) ----------

    @Test
    fun beforeTheRolloverStillCountsAsThePreviousDay() {
        assertEquals(monday, budgetDate(at(tuesday, 4, 59), fiveAm))
        assertEquals(monday, budgetDate(at(tuesday, 0, 0), fiveAm))
    }

    @Test
    fun fromTheRolloverOnItIsTheNewDay() {
        assertEquals(tuesday, budgetDate(at(tuesday, 5, 0), fiveAm))
        assertEquals(tuesday, budgetDate(at(tuesday, 23, 59), fiveAm))
    }

    @Test
    fun midnightRolloverIsJustTheCalendarDate() {
        assertEquals(tuesday, budgetDate(at(tuesday, 0, 0)))
        assertEquals(tuesday, budgetDate(at(tuesday, 23, 59)))
    }

    @Test
    fun aLateRolloverWorksToo() {
        // Day starts at 23:30: Tuesday 23:29 still belongs to the day that began Monday 23:30.
        val late = LocalTime.of(23, 30)
        assertEquals(monday, budgetDate(at(tuesday, 23, 29), late))
        assertEquals(tuesday, budgetDate(at(tuesday, 23, 30), late))
    }

    // ---------- the rest of the math follows the rollover time ----------

    @Test
    fun dayStartIsTheRolloverTime() {
        val expected = ZonedDateTime.of(tuesday, fiveAm, utc).toInstant().toEpochMilli()
        assertEquals(expected, dayStartMillis(tuesday, utc, fiveAm))
    }

    @Test
    fun weekStartIsSundayAtTheRolloverTime() {
        val sunday = LocalDate.of(2026, 10, 4)
        val expected = ZonedDateTime.of(sunday, fiveAm, utc).toInstant().toEpochMilli()
        assertEquals(expected, periodStartMillis(tuesday, BudgetPeriod.WEEKLY, utc, fiveAm))
    }

    @Test
    fun pruneCutoffIsOneMonthAgoAtTheRolloverTime() {
        val oneMonthAgo = LocalDate.of(2026, 9, 6) // tuesday is Oct 6
        val expected = ZonedDateTime.of(oneMonthAgo, fiveAm, utc).toInstant().toEpochMilli()
        assertEquals(expected, pruneCutoffMillis(tuesday, utc, fiveAm))
    }

    @Test
    fun nextRolloverIsLaterTodayWhenItHasNotHappenedYet() {
        assertEquals(2 * 3_600_000L, millisUntilNextDay(at(tuesday, 3, 0), fiveAm))
    }

    @Test
    fun nextRolloverIsTomorrowWhenTodaysHasPassed() {
        assertEquals(23 * 3_600_000L, millisUntilNextDay(at(tuesday, 6, 0), fiveAm))
        assertEquals(24 * 3_600_000L, millisUntilNextDay(at(tuesday, 5, 0), fiveAm)) // exactly on it
    }
}
