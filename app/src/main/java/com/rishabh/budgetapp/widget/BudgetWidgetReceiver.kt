package com.rishabh.budgetapp.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()

    // Last widget removed: stop the midnight alarm, so nothing wakes up for no reason.
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        MidnightRefresh.cancel(context)
    }

    // NOTE: do not override onUpdate / onEnabled here to start the midnight alarm. Glance's own receiver already
    // uses the one "keep this broadcast alive" handle (goAsync) that Android gives it while it draws the widget.
    // Asking for a second handle returns null, and using it crashed the app before the widget was drawn, which
    // left the widget empty until something else refreshed it. The alarm is started in BudgetWidget.provideGlance
    // instead, which runs when the widget is added and on every update.
}
