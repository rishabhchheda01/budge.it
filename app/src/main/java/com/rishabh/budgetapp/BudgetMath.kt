package com.rishabh.budgetapp

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

// All the date math for budgets lives here as plain functions (no Android code),
// so it is easy to read and easy to test.

/** The first day of the current budget period: today for DAILY, the most recent Sunday for WEEKLY. */
fun periodStartDate(today: LocalDate, period: BudgetPeriod): LocalDate = when (period) {
    BudgetPeriod.DAILY -> today
    BudgetPeriod.WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
}

// ROLLOVER: the moment a new budget day begins. It is midnight (00:00) unless the user changes it in
// Settings, e.g. 05:00 means "your day runs from 5 am to 5 am". Every function below that needs it takes a
// [rollover] time, and all of them default to midnight.

/**
 * Which budget day does this moment belong to? Normally that is just the calendar date, but before the
 * [rollover] time you are still in the PREVIOUS day. Example with a 05:00 rollover:
 * Tuesday 04:59 is still Monday's budget day, Tuesday 05:00 is Tuesday's.
 *
 * Used for "today" everywhere (app, widget, alarm) and to put each transaction under the right day.
 */
fun budgetDate(now: ZonedDateTime, rollover: LocalTime = LocalTime.MIDNIGHT): LocalDate {
    return now.minusSeconds(rollover.toSecondOfDay().toLong()).toLocalDate()
}

/** Same as [periodStartDate], but as a timestamp in milliseconds (the rollover time on that day). */
fun periodStartMillis(
    today: LocalDate,
    period: BudgetPeriod,
    zone: ZoneId = ZoneId.systemDefault(),
    rollover: LocalTime = LocalTime.MIDNIGHT
): Long = periodStartDate(today, period).toMillis(zone, rollover)

/** When [today] began (midnight, or the rollover time), in milliseconds. Spending before this is "before today". */
fun dayStartMillis(
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    rollover: LocalTime = LocalTime.MIDNIGHT
): Long = today.toMillis(zone, rollover)

/** Anything older than this is deleted: we keep this week and last week (weeks start on Sunday). */
fun pruneCutoffMillis(
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    rollover: LocalTime = LocalTime.MIDNIGHT
): Long = periodStartDate(today, BudgetPeriod.WEEKLY).minusWeeks(1).toMillis(zone, rollover)

/** Days left in the week including today: Sunday = 7, Monday = 6, ... Saturday = 1. */
fun daysLeftInWeek(today: LocalDate): Int = 7 - (today.dayOfWeek.value % 7)

/** How long until the next rollover (midnight by default), so the app knows when to start a new day. */
fun millisUntilNextDay(now: ZonedDateTime, rollover: LocalTime = LocalTime.MIDNIGHT): Long {
    val date = now.toLocalDate()
    val rolloverToday = date.atTime(rollover).atZone(now.zone)
    // Already past today's rollover? Then the next one is tomorrow's.
    val next = if (rolloverToday.isAfter(now)) rolloverToday else date.plusDays(1).atTime(rollover).atZone(now.zone)
    return Duration.between(now, next).toMillis()
}

/**
 * The weekly budget's "how much can I spend per day" number:
 * what is left this week, divided by the days left (including today).
 * It goes negative when you are over the weekly budget.
 */
fun dailyAllowance(left: Double, daysLeft: Int): Double {
    return left/ daysLeft.toDouble();
}

private fun LocalDate.toMillis(zone: ZoneId, rollover: LocalTime): Long =
    atTime(rollover).atZone(zone).toInstant().toEpochMilli()
