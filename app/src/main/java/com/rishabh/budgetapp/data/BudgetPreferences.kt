package com.rishabh.budgetapp.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rishabh.budgetapp.BudgetPeriod
import com.rishabh.budgetapp.ThemeMode
import com.rishabh.budgetapp.ui.theme.WidgetBackground
import kotlinx.coroutines.flow.map
import java.time.LocalTime

private val Context.budgetDataStore by preferencesDataStore(name = "budget_prefs")

class BudgetPreferences(private val context: Context) {
    private val periodKey = stringPreferencesKey("budget_period")
    // New key name: the old "budget_amount" key stored text, and this one stores a number.
    private val amountKey = doublePreferencesKey("budget_amount_value")
    private val isSetKey = booleanPreferencesKey("budget_is_set")
    private val themeKey = stringPreferencesKey("theme_mode")
    // When a new day begins, saved as minutes after midnight (0 = midnight, 300 = 05:00).
    private val rolloverKey = intPreferencesKey("rollover_minutes")
    // The widget's background color, saved as one number (ARGB). Not saved yet = the default sand.
    private val widgetColorKey = intPreferencesKey("widget_color")

    val budgetPeriod = context.budgetDataStore.data.map { prefs ->
        prefs[periodKey]?.let { BudgetPeriod.valueOf(it) } ?: BudgetPeriod.WEEKLY
    }

    val budgetAmount = context.budgetDataStore.data.map { prefs ->
        prefs[amountKey] ?: 0.0
    }

    val isBudgetSet = context.budgetDataStore.data.map { prefs ->
        prefs[isSetKey] ?: false
    }

    val themeMode = context.budgetDataStore.data.map { prefs ->
        prefs[themeKey]?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } } ?: ThemeMode.SYSTEM
    }

    val rolloverTime = context.budgetDataStore.data.map { prefs ->
        val minutes = (prefs[rolloverKey] ?: 0).coerceIn(0, 24 * 60 - 1)
        LocalTime.of(minutes / 60, minutes % 60)
    }

    val widgetColor = context.budgetDataStore.data.map { prefs ->
        prefs[widgetColorKey]?.let { Color(it) } ?: WidgetBackground
    }

    suspend fun setWidgetColor(color: Color) {
        context.budgetDataStore.edit { prefs -> prefs[widgetColorKey] = color.toArgb() }
    }

    // Switching Daily <-> Weekly changes the period AND the amount together, in one step.
    suspend fun setPeriodAndAmount(period: BudgetPeriod, amount: Double) {
        context.budgetDataStore.edit { prefs ->
            prefs[periodKey] = period.name
            prefs[amountKey] = amount
        }
    }

    suspend fun setRolloverTime(time: LocalTime) {
        context.budgetDataStore.edit { prefs -> prefs[rolloverKey] = time.hour * 60 + time.minute }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.budgetDataStore.edit { prefs -> prefs[themeKey] = mode.name }
    }

    suspend fun setBudgetPeriod(period: BudgetPeriod) {
        context.budgetDataStore.edit { prefs -> prefs[periodKey] = period.name }
    }

    // Saves the amount and marks the budget as set in one step.
    suspend fun confirmBudget(amount: Double) {
        context.budgetDataStore.edit { prefs ->
            prefs[amountKey] = amount
            prefs[isSetKey] = true
        }
    }
}
