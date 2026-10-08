package com.rishabh.budgetapp.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.rishabh.budgetapp.data.BudgetPreferences
import com.rishabh.budgetapp.millisUntilNextDay
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime

/**
 * Schedules ONE alarm for just after the next rollover (midnight, or the time chosen in Settings).
 * When it fires, DayChangeReceiver refreshes the widget and deletes old transactions, then schedules
 * the next one. Nothing runs in between, so this costs almost no battery.
 */
object MidnightRefresh {
    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, DayChangeReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Safe to call many times: scheduling again just replaces the previous alarm.
    // Reads the rollover time from the saved settings, so it is always the current one.
    suspend fun schedule(context: Context) {
        val rollover = BudgetPreferences(context).rolloverTime.first()
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // "Inexact": Android may fire it a few minutes late to save battery, and needs no special permission.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + millisUntilNextDay(ZonedDateTime.now(), rollover) + 5_000,
            pendingIntent(context)
        )
    }

    /** Only keeps the alarm going while a widget is actually on the home screen. */
    suspend fun scheduleIfWidgetExists(context: Context) {
        val hasWidget = GlanceAppWidgetManager(context)
            .getGlanceIds(BudgetWidget::class.java).isNotEmpty()
        if (hasWidget) schedule(context)
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }
}
