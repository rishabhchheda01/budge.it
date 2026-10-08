package com.rishabh.budgetapp.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.rishabh.budgetapp.ThemeMode
import com.rishabh.budgetapp.data.BudgetPreferences
import com.rishabh.budgetapp.data.TransactionRepository
import com.rishabh.budgetapp.ui.TransactionDialog
import com.rishabh.budgetapp.ui.theme.BudgetTheme
import com.rishabh.budgetapp.ui.theme.resolveDarkTheme
import kotlinx.coroutines.launch

/**
 * Opened by the "+" button on the widget. It is see-through, so you just see the add card
 * floating over your home screen. Save adds the transaction, refreshes the widget, and closes.
 */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = BudgetPreferences(applicationContext)
        setContent {
            val themeMode by preferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            BudgetTheme(darkTheme = themeMode.resolveDarkTheme()) {
                TransactionDialog(
                    title = "Add transaction",
                    onDismiss = { finish() },
                    onConfirm = { label, amount ->
                        lifecycleScope.launch {
                            TransactionRepository(applicationContext).add(label, amount)
                            finish()
                        }
                    }
                )
            }
        }
    }
}
