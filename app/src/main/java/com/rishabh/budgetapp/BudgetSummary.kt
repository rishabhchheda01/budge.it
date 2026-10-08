package com.rishabh.budgetapp

import com.rishabh.budgetapp.data.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalTime

/**
 * Everything the screen and widget need to show. It is always about TODAY and TODAY'S LIMIT:
 *  - DAILY budget:  the limit is your daily budget.
 *  - WEEKLY budget: the limit is your daily allowance (see [of]).
 */
data class BudgetSummary(
    /** Today's limit. */
    val limit: Double,
    /** Spent today. */
    val spent: Double,
    /** Limit minus spent today. Negative when you are over today's limit. */
    val left: Double,
    /** 0f to 1f, for the progress bar. Full (1f) when you are over the limit. */
    val progress: Float,
    /** WEEKLY only (null for DAILY): what is left of the whole weekly budget. */
    val weeklyLeft: Double?,
    /** WEEKLY only (null for DAILY): days left in the week including today. */
    val daysLeft: Int?
) {
    val isOver: Boolean get() = left < 0

    companion object {
        /**
         * [spent] is everything spent in the current period (today for DAILY, this week for WEEKLY);
         * [spentToday] is the part of that spent since midnight today.
         */
        fun of(
            period: BudgetPeriod,
            budget: Double,
            spent: Double,
            spentToday: Double,
            today: LocalDate
        ): BudgetSummary {
            val weeklyLeft = if (period == BudgetPeriod.WEEKLY) budget - spent else null
            val daysLeft = if (period == BudgetPeriod.WEEKLY) daysLeftInWeek(today) else null

            // WEEKLY: today's limit is what was left at the START of today, split over the days left.
            // Spending today doesn't change it; it is worked out again tomorrow.
            val limit = if (weeklyLeft != null && daysLeft != null) {
                dailyAllowance(weeklyLeft + spentToday, daysLeft)
            } else {
                budget
            }
            val spentAgainstLimit = spentToday
            val left = limit - spentAgainstLimit

            return BudgetSummary(
                limit = limit,
                spent = spentAgainstLimit,
                left = left,
                progress = when {
                    limit > 0 -> (spentAgainstLimit / limit).toFloat().coerceIn(0f, 1f)
                    spentAgainstLimit > 0 || left < 0 -> 1f // no allowance left but something spent: full
                    else -> 0f
                },
                weeklyLeft = weeklyLeft,
                daysLeft = daysLeft
            )
        }
    }
}

/** A live summary: re-emits whenever a transaction is added, edited or deleted. Used by the app and the widget. */
fun TransactionRepository.summaryFlow(
    period: BudgetPeriod,
    budget: Double,
    today: LocalDate,
    rollover: LocalTime = LocalTime.MIDNIGHT
): Flow<BudgetSummary> = combine(
    getSpentSince(periodStartMillis(today, period, rollover = rollover)),
    getSpentSince(dayStartMillis(today, rollover = rollover))
) { spent, spentToday ->
    BudgetSummary.of(period, budget, spent, spentToday, today)
}
