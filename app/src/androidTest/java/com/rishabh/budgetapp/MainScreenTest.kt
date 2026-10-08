package com.rishabh.budgetapp

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        limit = 100.0, spent = 40.0, left = 60.0, progress = 0.4f, weeklyLeft = 540.0, daysLeft = 6
    )

    // What the screen reported back, so tests can check it.
    private var confirmedBudget: Double? = null
    private var addedLabel: String? = null
    private var addedAmount: Double? = null
    private var deleted: Transaction? = null
    private var restored: Transaction? = null
    private var cleared = false

    private fun show(
        isBudgetSet: Boolean = true,
        summary: BudgetSummary = weeklySummary,
        transactions: List<Transaction> = emptyList(),
        period: BudgetPeriod = BudgetPeriod.WEEKLY
    ) {
        compose.setContent {
            BudgetTheme(darkTheme = false) {
                MainScreen(
                    budgetPeriod = period,
                    budgetAmount = 0.0,
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
                    onClearTransactions = { cleared = true }
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
        compose.onNodeWithText("$40.00 of $100.00 today").assertExists()
        compose.onNodeWithText("$540.00 left this week · 6 days left").assertExists()
    }

    @Test
    fun lastDayOfTheWeekSaysOneDayNotOneDays() {
        show(summary = weeklySummary.copy(daysLeft = 1))
        compose.onNodeWithText("$540.00 left this week · 1 day left").assertExists()
    }

    @Test
    fun dailyScreenHasNoWeeklyLine() {
        show(
            summary = BudgetSummary(limit = 50.0, spent = 20.0, left = 30.0, progress = 0.4f, weeklyLeft = null, daysLeft = null),
            period = BudgetPeriod.DAILY
        )
        compose.onNodeWithText("DAILY BUDGET").assertExists()
        compose.onNodeWithText("$30.00").assertExists()
        compose.onNodeWithText("$20.00 of $50.00 today").assertExists()
        compose.onNodeWithText("left this week", substring = true).assertDoesNotExist()
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
    fun saveIsDisabledUntilBothTheAmountAndTheDescriptionAreFilledIn() {
        show()
        compose.onNodeWithText("Add transaction").performClick()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithTag("amountField").performTextInput("12.5")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithTag("descriptionField").performTextInput("Lunch")
        compose.onNodeWithText("Save").assertIsEnabled()
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
        compose.onNodeWithText("Clear all transactions").performClick()
        compose.onNodeWithText("Clear all transactions?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        assertFalse(cleared)
        compose.onNodeWithText("Clear all transactions?").assertDoesNotExist()
    }

    @Test
    fun confirmingTheClearCardClearsTransactions() {
        show()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Clear all transactions").performClick()
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
}
