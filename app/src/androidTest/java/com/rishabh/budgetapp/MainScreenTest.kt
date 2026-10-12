package com.rishabh.budgetapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rishabh.budgetapp.data.Transaction
import com.rishabh.budgetapp.ui.MainScreen
import com.rishabh.budgetapp.ui.theme.BudgetTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Taps through the real screen: setup, adding, deleting with undo, settings, and the numbers shown. */
@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val weeklySummary = BudgetSummary(
        limit = 100.0, spent = 40.0, left = 60.0, progress = 0.4f,
        weeklyLeft = 540.0, daysLeft = 6, weeklyProgress = 0.23f
    )

    // What the screen reported back, so tests can check it.
    private var confirmedBudget: Double? = null
    private var addedLabel: String? = null
    private var addedAmount: Double? = null
    private var deleted: Transaction? = null
    private var restored: Transaction? = null
    private var cleared = false
    private var switched: Pair<BudgetPeriod, Double>? = null
    private var editColorOpened = false

    private fun show(
        isBudgetSet: Boolean = true,
        summary: BudgetSummary = weeklySummary,
        transactions: List<Transaction> = emptyList(),
        period: BudgetPeriod = BudgetPeriod.WEEKLY,
        budgetAmount: Double = 0.0
    ) {
        compose.setContent {
            BudgetTheme(darkTheme = false) {
                MainScreen(
                    budgetPeriod = period,
                    budgetAmount = budgetAmount,
                    isBudgetSet = isBudgetSet,
                    summary = summary,
                    transactions = transactions,
                    onBudgetPeriodChange = {},
                    onConfirmBudget = { confirmedBudget = it },
                    onAddTransaction = { label, amount -> addedLabel = label; addedAmount = amount },
                    onUpdateTransaction = {},
                    onDeleteTransaction = { deleted = it },
                    onRestoreTransaction = { restored = it },
                    themeMode = ThemeMode.SYSTEM,
                    onThemeModeChange = {},
                    onClearTransactions = { cleared = true },
                    onSwitchBudgetPeriod = { newPeriod, amount -> switched = newPeriod to amount },
                    onEditWidgetColor = { editColorOpened = true }
                )
            }
        }
    }

    // ---------- first run ----------

    @Test
    fun firstRunShowsTheSetupScreenAndSetBudgetStaysDisabledUntilAnAmountIsTyped() {
        show(isBudgetSet = false)
        compose.onNodeWithText("Set your budget").assertExists()
        compose.onNodeWithText("Set budget").assertIsNotEnabled()
        compose.onNodeWithTag("amountField").performTextInput("150")
        compose.onNodeWithText("Set budget").assertIsEnabled().performClick()
        assertEquals(150.0, confirmedBudget!!, 0.001)
    }

    @Test
    fun theAmountFieldIgnoresLettersAndASecondDecimalPoint() {
        show(isBudgetSet = false)
        compose.onNodeWithTag("amountField").performTextInput("1a2.5.7")
        compose.onNodeWithText("Set budget").performClick()
        assertEquals(12.57, confirmedBudget!!, 0.001)
    }

    // ---------- numbers on screen ----------

    @Test
    fun weeklyScreenShowsLeftTodayAndTheWeeklyRemaining() {
        show()
        compose.onNodeWithText("TODAY'S BUDGET").assertExists()
        compose.onNodeWithText("$60.00").assertExists()
        compose.onNodeWithText("left today").assertExists()
        compose.onNodeWithText("$40.00 of $100.00").assertExists()
        compose.onNodeWithText("left this week").assertExists()
        compose.onNodeWithText("$540.00 · 6 days left").assertExists()
    }

    @Test
    fun lastDayOfTheWeekSaysOneDayNotOneDays() {
        show(summary = weeklySummary.copy(daysLeft = 1))
        compose.onNodeWithText("$540.00 · 1 day left").assertExists()
    }

    @Test
    fun dailyScreenShowsTheWeeklyBarToo() {
        // A daily budget of 50 has a weekly budget of 350, here with 20 spent so far.
        show(
            summary = BudgetSummary(
                limit = 50.0, spent = 20.0, left = 30.0, progress = 0.4f,
                weeklyLeft = 330.0, daysLeft = 4, weeklyProgress = 0.06f
            ),
            period = BudgetPeriod.DAILY
        )
        compose.onNodeWithText("DAILY BUDGET").assertExists()
        compose.onNodeWithText("$30.00").assertExists()
        compose.onNodeWithText("$20.00 of $50.00").assertExists()
        compose.onNodeWithText("left this week").assertExists()
        compose.onNodeWithText("$330.00 · 4 days left").assertExists()
    }

    @Test
    fun overTheLimitShowsANegativeNumber() {
        show(summary = weeklySummary.copy(spent = 120.0, left = -20.0, progress = 1f))
        compose.onNodeWithText("-$20.00").assertExists()
    }

    // ---------- the transaction list ----------

    @Test
    fun emptyListSaysThereIsNoSpendingYet() {
        show()
        compose.onNodeWithText("No spending yet").assertExists()
    }

    @Test
    fun todaysTransactionsAreGroupedUnderTodayAndShownAsNegative() {
        show(transactions = listOf(Transaction(id = 1, label = "Coffee", amount = 4.5)))
        compose.onNodeWithText("TODAY").assertExists()
        compose.onNodeWithText("Coffee").assertExists()
        compose.onNodeWithText("-$4.50").assertExists()
    }

    @Test
    fun theDateStaysPinnedAtTheTopWhileYouScrollThroughItsTransactions() {
        show(transactions = List(30) { Transaction(id = it + 1L, label = "Item $it", amount = 1.0) })
        compose.onNodeWithText("TODAY").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToIndex(30)
        compose.onNodeWithText("TODAY").assertIsDisplayed() // still there, although its first row is far above
    }

    @Test
    fun deletingShowsUndoAndUndoRestoresTheSameTransaction() {
        val coffee = Transaction(id = 1, label = "Coffee", amount = 4.5)
        show(transactions = listOf(coffee))
        compose.onNodeWithContentDescription("Delete transaction").performClick()
        assertEquals(coffee, deleted)
        compose.onNodeWithText("Transaction deleted").assertExists()
        compose.onNodeWithText("Undo").performClick()
        compose.waitForIdle()
        assertEquals(coffee, restored)
    }

    // ---------- adding ----------

    @Test
    fun saveIsDisabledUntilTheAmountIsFilledIn() {
        show()
        compose.onNodeWithText("Add transaction").performClick()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithTag("amountField").performTextInput("12.5")
        compose.onNodeWithText("Save").assertIsEnabled() // the name is optional
    }

    @Test
    fun aTransactionCanBeAddedWithoutAName() {
        show()
        compose.onNodeWithText("Add transaction").performClick()
        compose.onNodeWithTag("amountField").performTextInput("7")
        compose.onNodeWithText("Save").performClick()
        assertEquals("", addedLabel)
        assertEquals(7.0, addedAmount!!, 0.001)
    }

    @Test
    fun aTransactionWithoutANameShowsJustTheAmount() {
        show(transactions = listOf(Transaction(id = 1, label = "", amount = 3.0)))
        compose.onNodeWithText("Untitled").assertDoesNotExist()
        compose.onNodeWithText("-$3.00").assertExists()
    }

    @Test
    fun addingATransactionReportsTheLabelAndAmountAndClosesTheCard() {
        show()
        compose.onNodeWithText("Add transaction").performClick()
        compose.onNodeWithTag("amountField").performTextInput("12.5")
        compose.onNodeWithTag("descriptionField").performTextInput("Lunch")
        compose.onNodeWithText("Save").performClick()
        assertEquals("Lunch", addedLabel)
        assertEquals(12.5, addedAmount!!, 0.001)
        compose.onNodeWithText("ADD TRANSACTION").assertDoesNotExist()
    }

    @Test
    fun cancellingTheAddCardAddsNothing() {
        show()
        compose.onNodeWithText("Add transaction").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(null, addedLabel)
        compose.onNodeWithText("ADD TRANSACTION").assertDoesNotExist()
    }

    // ---------- settings ----------

    @Test
    fun settingsOpensFromTheGearAndShowsTheBudgetThemeAndDataSections() {
        show()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("BUDGET").assertExists()
        compose.onNodeWithText("THEME").assertExists()
        compose.onNodeWithText("DATA").assertExists()
        compose.onNodeWithText("Add transaction").assertDoesNotExist() // the Add button hides in settings
    }

    @Test
    fun backArrowReturnsToTheMainScreen() {
        show()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Add transaction").assertExists()
    }

    @Test
    fun clearingTransactionsNeedsConfirmationAndCancelDoesNothing() {
        show()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Clear all transactions").performScrollTo().performClick() // it is below the fold now
        compose.onNodeWithText("Clear all transactions?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        assertFalse(cleared)
        compose.onNodeWithText("Clear all transactions?").assertDoesNotExist()
    }

    @Test
    fun confirmingTheClearCardClearsTransactions() {
        show()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Clear all transactions").performScrollTo().performClick() // it is below the fold now
        compose.onNodeWithText("Clear").performClick()
        assertTrue(cleared)
    }

    @Test
    fun saveBudgetIsOnlyEnabledAfterTheAmountChanges() {
        show()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Save budget").assertIsNotEnabled()
        compose.onNodeWithTag("amountField").performTextInput("250")
        compose.onNodeWithText("Save budget").assertIsEnabled().performClick()
        assertEquals(250.0, confirmedBudget!!, 0.001)
    }

    // ---------- switching Daily <-> Weekly in Settings (nothing is saved until "Save budget") ----------

    @Test
    fun switchingToDailyConvertsTheAmountButSavesNothingYet() {
        show(period = BudgetPeriod.WEEKLY, budgetAmount = 100.0)
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Save budget").assertIsNotEnabled()
        compose.onNodeWithText("Daily").performClick()
        compose.onNodeWithTag("amountField").assertTextContains("14.29")
        assertEquals(null, switched) // nothing saved by the switch alone
        compose.onNodeWithText("Save budget").assertIsEnabled()
    }

    @Test
    fun saveBudgetSavesThePeriodAndTheExactConvertedAmountTogether() {
        show(period = BudgetPeriod.WEEKLY, budgetAmount = 100.0)
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Daily").performClick()
        compose.onNodeWithText("Save budget").performClick()
        assertEquals(BudgetPeriod.DAILY, switched!!.first)
        assertEquals(100.0 / 7, switched!!.second, 0.0000001) // exact, not the rounded 14.29
    }

    @Test
    fun switchingThereAndBackPutsTheOriginalAmountBackAndDisablesSave() {
        show(period = BudgetPeriod.WEEKLY, budgetAmount = 100.0)
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Daily").performClick()
        compose.onNodeWithText("Weekly").performClick()
        compose.onNodeWithTag("amountField").assertTextContains("100")
        compose.onNodeWithText("Save budget").assertIsNotEnabled()
    }

    @Test
    fun anAmountYouTypedIsConvertedWhenYouSwitch() {
        show(period = BudgetPeriod.WEEKLY, budgetAmount = 100.0)
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithTag("amountField").performTextClearance()
        compose.onNodeWithTag("amountField").performTextInput("70")
        compose.onNodeWithText("Daily").performClick()
        compose.onNodeWithTag("amountField").assertTextContains("10")
        compose.onNodeWithText("Save budget").performClick()
        assertEquals(10.0, switched!!.second, 0.0001)
    }

    @Test
    fun firstRunSetupAlsoConvertsTheTypedAmountWhenYouSwitch() {
        show(isBudgetSet = false, period = BudgetPeriod.WEEKLY)
        compose.onNodeWithTag("amountField").performTextInput("70")
        compose.onNodeWithText("Daily").performClick()
        compose.onNodeWithTag("amountField").assertTextContains("10")
    }

    // ---------- the Insights page ----------

    @Test
    fun insightsOpensFromTheMagnifyingGlassAndBackReturnsToTheMainScreen() {
        show()
        compose.onNodeWithContentDescription("Insights").performClick()
        compose.onNodeWithText("THIS WEEK", substring = true).assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("TODAY'S BUDGET").assertExists()
    }

    @Test
    fun insightsOpensOnThisWeekWithNoWayForwardButAWayBack() {
        show()
        compose.onNodeWithContentDescription("Insights").performClick()
        compose.onNodeWithText("THIS WEEK").assertExists()
        compose.onNodeWithContentDescription("Next week").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Previous week").assertIsEnabled()
    }

    @Test
    fun insightsGoesBackUpToThreeWeeksAndThenStops() {
        show()
        compose.onNodeWithContentDescription("Insights").performClick()
        compose.onNodeWithContentDescription("Previous week").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("LAST WEEK").assertExists()
        compose.onNodeWithContentDescription("Previous week").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("2 WEEKS AGO").assertExists()
        compose.onNodeWithContentDescription("Previous week").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("3 WEEKS AGO").assertExists()
        compose.onNodeWithContentDescription("Previous week").assertIsNotEnabled()
    }

    @Test
    fun theNextArrowComesBackToThisWeek() {
        show()
        compose.onNodeWithContentDescription("Insights").performClick()
        compose.onNodeWithContentDescription("Previous week").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Next week").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("THIS WEEK").assertExists()
        compose.onNodeWithContentDescription("Next week").assertIsNotEnabled()
    }

    @Test
    fun insightsShowsTodaysSpendingAgainstTodaysBudget() {
        show(
            period = BudgetPeriod.DAILY,
            budgetAmount = 20.0,
            transactions = listOf(Transaction(id = 1, label = "Coffee", amount = 4.5))
        )
        compose.onNodeWithContentDescription("Insights").performClick()
        compose.onNodeWithText("$4.50 / $20.00").assertExists()
    }

    // ---------- the widget color button, and the pinned Settings header ----------

    @Test
    fun editWidgetColorButtonOpensTheColorScreen() {
        show()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Edit widget color").performScrollTo().performClick()
        assertTrue(editColorOpened)
    }

    @Test
    fun theSettingsTitleStaysOnScreenWhileTheOptionsScroll() {
        show()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Clear all transactions").performScrollTo()
        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed()
    }
}
