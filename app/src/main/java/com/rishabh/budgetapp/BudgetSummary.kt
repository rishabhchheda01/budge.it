package com.rishabh.budgetapp

import com.rishabh.budgetapp.data.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalTime

/**
 * Everything the screen and widget need to show. It has TWO parts, for both budget types:
 *  - TODAY: today's limit, what was spent today, what is left, and the daily progress bar.
 *      DAILY budget:  the limit is your daily budget.
 *      WEEKLY budget: the limit is your daily allowance (see [of]).
 *  - THIS WEEK: the weekly budget, what was spent this week, what is left, and the weekly progress bar.
 *      WEEKLY budget: the weekly budget is what you set.
 *      DAILY budget:  the weekly budget is worked out for you: your daily budget x 7.
 */
data class BudgetSummary(
    /** Today's limit. */
    val limit: Double,
    /** Spent today. */
    val spent: Double,
    /** Limit minus spent today. Negative when you are over today's limit. */
    val left: Double,
    /** 0f to 1f, for the DAILY progress bar. Full (1f) when you are over the limit. */
    val progress: Float,
    /** What is left of the whole week's budget. Negative when you are over it. */
    val weeklyLeft: Double,
    /** Days left in the week including today. */
    val daysLeft: Int,
    /** 0f to 1f, for the WEEKLY progress bar. Full (1f) when you are over the week's budget. */
    val weeklyProgress: Float
) {
    val isOver: Boolean get() = left < 0

    companion object {
        /**
         * [spent] is everything spent THIS WEEK (since the week began, whatever the budget type);
         * [spentToday] is the part of that spent since today began.
         */
        fun of(
            period: BudgetPeriod,
            budget: Double,
            spent: Double,
            spentToday: Double,
            today: LocalDate
        ): BudgetSummary {
            // The week's budget: what you set (WEEKLY), or the daily budget for all 7 days (DAILY).
            val weeklyBudget = if (period == BudgetPeriod.WEEKLY) budget else budget * 7
            val weeklyLeft = weeklyBudget - spent
            val daysLeft = daysLeftInWeek(today)

            // WEEKLY: today's limit is what was left at the START of today, split over the days left.
            // Spending today doesn't change it; it is worked out again tomorrow.
            val limit = if (period == BudgetPeriod.WEEKLY) {
                dailyAllowance(weeklyLeft + spentToday, daysLeft)
            } else {
                budget
            }
            val left = limit - spentToday

            return BudgetSummary(
                limit = limit,
                spent = spentToday,
                left = left,
                progress = progressOf(spentToday, limit),
                weeklyLeft = weeklyLeft,
                daysLeft = daysLeft,
                weeklyProgress = progressOf(spent, weeklyBudget)
            )
        }

        // How full a bar is: spent as a share of the limit, from 0f to 1f. A negative limit (you are already
        // over the week's budget) or nothing left but something spent: the bar is full.
        private fun progressOf(spent: Double, limit: Double): Float = when {
            limit > 0 -> (spent / limit).toFloat().coerceIn(0f, 1f)
            limit < 0 || spent > 0 -> 1f
            else -> 0f
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
    // Always the whole week so far (both bars need it, whatever the budget type), and today.
    getSpentSince(periodStartMillis(today, BudgetPeriod.WEEKLY, rollover = rollover)),
    getSpentSince(dayStartMillis(today, rollover = rollover))
) { spent, spentToday ->
    BudgetSummary.of(period, budget, spent, spentToday, today)
}
