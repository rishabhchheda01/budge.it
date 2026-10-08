package com.rishabh.budgetapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rishabh.budgetapp.data.BudgetPreferences
import com.rishabh.budgetapp.data.Transaction
import com.rishabh.budgetapp.data.TransactionRepository
import com.rishabh.budgetapp.widget.BudgetWidget
import com.rishabh.budgetapp.widget.MidnightRefresh
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

enum class BudgetPeriod { DAILY, WEEKLY }

// What the summary is worked out from, bundled together.
private data class SummaryInputs(
    val period: BudgetPeriod,
    val budget: Double,
    val rollover: LocalTime,
    val date: LocalDate
)

// How the app picks light or dark: follow the phone, or force one.
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = BudgetPreferences(application)
    private val transactionRepository = TransactionRepository(application)

    // When a new day begins (midnight unless changed in Settings). Read straight from the saved settings.
    private val savedRollover = preferences.rolloverTime
    val rollover = savedRollover
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalTime.MIDNIGHT)

    // Bumped to make "today" be worked out again (app resumed, rollover time passed).
    private val refreshTick = MutableStateFlow(0)

    // Today's budget date, together with the rollover it was worked out with. When it changes (the rollover
    // time passes, you reopen the app on a new day, or you change the rollover time), everything below
    // recalculates, which is how the budget "resets".
    private val today = combine(savedRollover, refreshTick) { rolloverTime, _ ->
        rolloverTime to budgetDate(ZonedDateTime.now(), rolloverTime)
    }.distinctUntilChanged()

    val budgetPeriod = preferences.budgetPeriod
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetPeriod.WEEKLY)

    val budgetAmount = preferences.budgetAmount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // These three start as null = "still loading". The screen waits for all of them, so you never
    // see a flash of the setup screen or empty numbers while your saved data is being read.
    val isBudgetSet = preferences.isBudgetSet
        .stateIn<Boolean?>(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeMode = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val transactions = transactionRepository.getAll()
        .stateIn<List<Transaction>?>(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Spent / left / progress for the current period. Re-queries whenever the period, amount or date changes.
    val summary = combine(budgetPeriod, budgetAmount, today) { period, amount, (rolloverTime, date) ->
        SummaryInputs(period, amount, rolloverTime, date)
    }.flatMapLatest { inputs ->
        transactionRepository.summaryFlow(inputs.period, inputs.budget, inputs.date, inputs.rollover)
    }.stateIn<BudgetSummary?>(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Runs on app open and whenever the date changes: delete transactions older than last week.
        viewModelScope.launch {
            today.collect { (rolloverTime, date) ->
                transactionRepository.deleteOlderThan(pruneCutoffMillis(date, rollover = rolloverTime))
            }
        }
        // While the app is open, move to the next day at the rollover time. collectLatest starts this
        // waiting over whenever the rollover time is changed.
        viewModelScope.launch {
            savedRollover.collectLatest { rolloverTime ->
                while (true) {
                    delay(millisUntilNextDay(ZonedDateTime.now(), rolloverTime) + 1000)
                    refreshDate()
                }
            }
        }
    }

    // Called when the app comes back to the screen, in case the day changed while it was away.
    fun refreshDate() {
        refreshTick.value++
    }

    // Saves the new rollover time, moves the midnight alarm to match, and redraws the widget.
    fun setRolloverTime(time: LocalTime) {
        viewModelScope.launch {
            preferences.setRolloverTime(time)
            MidnightRefresh.scheduleIfWidgetExists(getApplication())
            BudgetWidget.refreshAll(getApplication())
        }
    }

    fun setBudgetPeriod(period: BudgetPeriod) {
        viewModelScope.launch {
            preferences.setBudgetPeriod(period)
            BudgetWidget.refreshAll(getApplication())
        }
    }

    fun confirmBudget(amount: Double) {
        viewModelScope.launch {
            preferences.confirmBudget(amount)
            BudgetWidget.refreshAll(getApplication())
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun clearTransactions() {
        viewModelScope.launch {
            transactionRepository.clearAll()
        }
    }

    fun addTransaction(label: String, amount: Double) {
        viewModelScope.launch {
            transactionRepository.add(label, amount)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.update(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.delete(transaction)
        }
    }

    fun restoreTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.restore(transaction)
        }
    }
}
