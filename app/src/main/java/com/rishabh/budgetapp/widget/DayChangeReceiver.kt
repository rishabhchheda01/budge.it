package com.rishabh.budgetapp.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rishabh.budgetapp.budgetDate
import com.rishabh.budgetapp.data.BudgetPreferences
import com.rishabh.budgetapp.data.TransactionRepository
import com.rishabh.budgetapp.pruneCutoffMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * Wakes up (even when the app is closed) at the rollover time (midnight unless changed in Settings), and also after a reboot or a
 * clock/timezone change. It refreshes the widget, deletes old transactions, and sets the next alarm.
 */
class DayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        // goAsync keeps Android from stopping this receiver while the work below finishes.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rollover = BudgetPreferences(app).rolloverTime.first()
                val today = budgetDate(ZonedDateTime.now(), rollover)
                TransactionRepository(app).deleteOlderThan(pruneCutoffMillis(today, rollover = rollover))
                BudgetWidget.refreshAll(app)
                MidnightRefresh.scheduleIfWidgetExists(app)
            } finally {
                pending.finish()
            }
        }
    }
}
