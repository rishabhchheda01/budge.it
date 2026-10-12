package com.rishabh.budgetapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rishabh.budgetapp.ui.BatterySaverDialog
import com.rishabh.budgetapp.ui.ColorPickerActivity
import com.rishabh.budgetapp.ui.MainScreen
import com.rishabh.budgetapp.ui.needsBatteryExemption
import com.rishabh.budgetapp.ui.requestBatteryExemption
import com.rishabh.budgetapp.ui.theme.BudgetTheme
import com.rishabh.budgetapp.ui.theme.resolveDarkTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BudgetViewModel by viewModels()

    // Re-checked every time the app comes to the front (see onResume).
    private var batteryPromptNeeded by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = themeMode.resolveDarkTheme()

            // Draw behind the status/navigation bars, and keep their icons readable
            // when the app's theme (light/dark) differs from the phone's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                        Color.argb(0x80, 0x1b, 0x1b, 0x1b)
                    ) { darkTheme }
                )
                onDispose {}
            }

            BudgetTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BudgetApp(viewModel)

                    // "Not now" hides the popup until the app is closed and opened again.
                    var notNow by rememberSaveable { mutableStateOf(false) }
                    if (batteryPromptNeeded && !notNow) {
                        BatterySaverDialog(
                            onAllow = { requestBatteryExemption() },
                            onNotNow = { notNow = true }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The day may have changed while the app was in the background.
        viewModel.refreshDate()
        // Also runs when you come back from the system question, so the popup goes away once allowed.
        batteryPromptNeeded = needsBatteryExemption()
    }
}

@Composable
private fun BudgetApp(viewModel: BudgetViewModel) {
    val budgetPeriod by viewModel.budgetPeriod.collectAsState()
    val budgetAmount by viewModel.budgetAmount.collectAsState()
    val isBudgetSet by viewModel.isBudgetSet.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val rollover by viewModel.rollover.collectAsState()
    val widgetColor by viewModel.widgetColor.collectAsState()
    val context = LocalContext.current

    // Wait until everything has loaded (all three are null until then): draw nothing, not the wrong screen.
    val loadedIsBudgetSet = isBudgetSet
    val loadedSummary = summary
    val loadedTransactions = transactions
    if (loadedIsBudgetSet == null || loadedSummary == null || loadedTransactions == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    MainScreen(
        budgetPeriod = budgetPeriod,
        budgetAmount = budgetAmount,
        isBudgetSet = loadedIsBudgetSet,
        summary = loadedSummary,
        transactions = loadedTransactions,
        onBudgetPeriodChange = { viewModel.setBudgetPeriod(it) },
        onConfirmBudget = { viewModel.confirmBudget(it) },
        onAddTransaction = { label, amount -> viewModel.addTransaction(label, amount) },
        onUpdateTransaction = { viewModel.updateTransaction(it) },
        onDeleteTransaction = { viewModel.deleteTransaction(it) },
        onRestoreTransaction = { viewModel.restoreTransaction(it) },
        themeMode = themeMode,
        onThemeModeChange = { viewModel.setThemeMode(it) },
        onClearTransactions = { viewModel.clearTransactions() },
        rollover = rollover,
        onRolloverChange = { viewModel.setRolloverTime(it) },
        onSwitchBudgetPeriod = { period, amount -> viewModel.switchBudgetPeriod(period, amount) },
        widgetColor = widgetColor,
        onEditWidgetColor = { context.startActivity(Intent(context, ColorPickerActivity::class.java)) }
    )
}
