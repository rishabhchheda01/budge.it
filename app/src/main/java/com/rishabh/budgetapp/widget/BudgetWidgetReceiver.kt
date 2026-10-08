package com.rishabh.budgetapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()

    // First widget added to the home screen: start the midnight refresh.
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleInBackground(context)
    }

    // Last widget removed: stop it, so nothing wakes up for no reason.
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        MidnightRefresh.cancel(context)
    }

    // Also re-arm on every widget update, in case the alarm was lost.
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleInBackground(context)
    }

    // The alarm needs the saved rollover time, which is read off the main thread.
    // goAsync keeps Android from stopping this receiver until that is done.
    private fun scheduleInBackground(context: Context) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MidnightRefresh.schedule(app)
            } finally {
                pending.finish()
            }
        }
    }
}
