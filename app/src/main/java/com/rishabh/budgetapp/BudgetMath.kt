package com.rishabh.budgetapp

import com.rishabh.budgetapp.data.Transaction
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
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

/**
 * Anything older than this is deleted: we keep ONE MONTH, from the same date last month (at the rollover
 * time) until now. Today = Oct 12 keeps everything from Sep 12. Where last month has no such date
 * (Mar 31) it uses the month's last day (Feb 28).
 */
fun pruneCutoffMillis(
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    rollover: LocalTime = LocalTime.MIDNIGHT
): Long = today.minusMonths(1).toMillis(zone, rollover)

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

/**
 * Keeps a budget meaning the same thing when you switch between DAILY and WEEKLY (a week is 7 days):
 * daily -> weekly multiplies by 7, weekly -> daily divides by 7. The result is NOT rounded here,
 * so switching back and forth gives exactly the starting number again.
 */
fun convertBudget(amount: Double, from: BudgetPeriod, to: BudgetPeriod): Double = when {
    from == to -> amount
    to == BudgetPeriod.WEEKLY -> amount * 7
    else -> amount / 7
}

/**
 * The budget for each day of the week, Sunday first (7 numbers). Used by the Insights graph.
 *  - DAILY:  every day has the same limit, [budget].
 *  - WEEKLY: each day's limit is worked out the way the app does it, from what was left at the START of
 *    that day: dailyAllowance(budget - spent on the earlier days, days left that day including itself).
 *
 * [spentPerDay] is what was spent on each day of the week, Sunday first (7 numbers).
 * [todayIndex] is today's place in the week (Sunday = 0 ... Saturday = 6). Days AFTER today have not
 * happened yet, so they simply keep today's limit (that is what it would be if you spend exactly the
 * limit each day). Without this, future days would climb every day as if you spent nothing.
 */
fun weekLimits(
    period: BudgetPeriod,
    budget: Double,
    spentPerDay: List<Double>,
    todayIndex: Int = 6
): List<Double> {
    if (period == BudgetPeriod.DAILY) {
        return List(7) { budget }
    }
    // WEEKLY: [left] is what remains of the week at the start of each day. Sunday has 7 days left, Monday 6, ...
    var left = budget
    var latest = 0.0
    return List(7) { day ->
        if (day <= todayIndex) {
            latest = dailyAllowance(left, 7 - day)
            left -= spentPerDay.getOrElse(day) { 0.0 }
        }
        latest // future days repeat the last known (today's) limit
    }
}

/**
 * What was spent on each day of the week starting at [weekStart] (a Sunday), Sunday first (7 numbers).
 * Each transaction goes under its budget day (so the rollover time is respected). Anything outside the
 * week is ignored.
 */
fun spentPerDay(
    transactions: List<Transaction>,
    weekStart: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    rollover: LocalTime = LocalTime.MIDNIGHT
): List<Double> {
    val totals = DoubleArray(7)
    for (transaction in transactions) {
        val date = budgetDate(Instant.ofEpochMilli(transaction.timestamp).atZone(zone), rollover)
        val day = ChronoUnit.DAYS.between(weekStart, date).toInt()
        if (day in 0..6) totals[day] += transaction.amount
    }
    return totals.toList()
}

private fun LocalDate.toMillis(zone: ZoneId, rollover: LocalTime): Long =
    atTime(rollover).atZone(zone).toInstant().toEpochMilli()
