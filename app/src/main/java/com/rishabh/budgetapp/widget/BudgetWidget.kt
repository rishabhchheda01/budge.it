package com.rishabh.budgetapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.rishabh.budgetapp.BudgetSummary
import com.rishabh.budgetapp.MainActivity
import com.rishabh.budgetapp.budgetDate
import com.rishabh.budgetapp.BudgetPeriod
import com.rishabh.budgetapp.currencyFormat
import com.rishabh.budgetapp.data.BudgetPreferences
import com.rishabh.budgetapp.data.TransactionRepository
import com.rishabh.budgetapp.summaryFlow
import com.rishabh.budgetapp.ui.theme.WidgetColors
import com.rishabh.budgetapp.ui.theme.fillColorOn
import com.rishabh.budgetapp.ui.theme.progressColor
import com.rishabh.budgetapp.ui.theme.widgetColorsFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.ZonedDateTime

// The widget is one cell tall and any width. The layout is the same at every width:
// "left today" and the number on the left, the "+" button on the right, and a pill-shaped
// progress bar along the bottom (with some space around it).

// Same color in light and dark mode, so the widget always looks the same.
// The background is picked in Settings; the other colors are worked out from it (ui/theme/Theme.kt).
private fun fixed(color: Color) = ColorProvider(day = color, night = color)

// Sizes are worked out from the widget's real size in widget/WidgetLayout.kt (change the rules there).

class BudgetWidget : GlanceAppWidget() {
    // Exact = the layout can ask for the widget's real size (LocalSize) and scale to it.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Make sure the midnight refresh is set up. This runs when the widget is first added and on every update.
        // If it fails, the widget must still be drawn, so the error is ignored.
        runCatching { MidnightRefresh.schedule(context) }

        // The widget WATCHES the data (budget settings and transactions). Whenever anything changes,
        // it redraws by itself. Reading the data only once here would leave the widget showing old
        // numbers whenever Android reuses it instead of starting it fresh.
        val states = widgetStates(context)
        val firstState = states.first()

        provideContent {
            val state by states.collectAsState(initial = firstState)
            WidgetContent(
                isBudgetSet = state.isBudgetSet,
                summary = state.summary,
                colors = widgetColorsFor(state.background)
            )
        }
    }

    companion object {
        /**
         * The size (in dp) the first widget on the home screen is really drawn at on this phone, or null if
         * there is no widget. The color screen uses it so its look-alike is exactly the same size.
         * Portrait uses the minimum width and the maximum height, which is what the widget itself uses.
         */
        suspend fun currentSize(context: Context): DpSize? {
            val manager = GlanceAppWidgetManager(context)
            val glanceId = manager.getGlanceIds(BudgetWidget::class.java).firstOrNull() ?: return null
            val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(manager.getAppWidgetId(glanceId))
            val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val width = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val height = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else null
        }

        suspend fun refreshAll(context: Context) {
            val widget = BudgetWidget()
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(BudgetWidget::class.java)
            ids.forEach { id -> widget.update(context, id) }
        }
    }
}

internal data class WidgetState(val isBudgetSet: Boolean, val summary: BudgetSummary, val background: Color)

// The saved settings the widget depends on, bundled so they can travel together.
private data class Settings(
    val period: BudgetPeriod,
    val amount: Double,
    val isSet: Boolean,
    val rollover: java.time.LocalTime,
    val background: Color
)

/** Emits a new state every time the budget settings or the transactions change. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun widgetStates(context: Context): Flow<WidgetState> {
    val preferences = BudgetPreferences(context)
    val repository = TransactionRepository(context)

    return combine(
        preferences.budgetPeriod,
        preferences.budgetAmount,
        preferences.isBudgetSet,
        preferences.rolloverTime,
        preferences.widgetColor
    ) { period, amount, isSet, rollover, background -> Settings(period, amount, isSet, rollover, background) }
        .flatMapLatest { settings ->
            val today = budgetDate(ZonedDateTime.now(), settings.rollover)
            repository.summaryFlow(settings.period, settings.amount, today, settings.rollover)
                .map { summary -> WidgetState(settings.isSet, summary, settings.background) }
        }
}

@Composable
private fun WidgetContent(isBudgetSet: Boolean, summary: BudgetSummary, colors: WidgetColors) {
    val backgroundColor = fixed(colors.background)
    val textColor = fixed(colors.text)
    val quietColor = fixed(colors.quietText)
    val left = currencyFormat().format(summary.left)
    val weeklyLeft = currencyFormat().format(summary.weeklyLeft)

    // Work out every size from the widget's real size, so nothing overlaps (see widget/WidgetLayout.kt).
    val size = LocalSize.current
    val sizes = widgetSizes(size.width.value, size.height.value, left.length)
    // Text sizes are in sp, which grows with the phone's "font size" setting, but the layout is in dp.
    // Dividing by the font scale keeps the text the size we worked out, so big-text users get no overlap.
    val fontScale = LocalContext.current.resources.configuration.fontScale
    fun textSize(dp: Float) = (dp / fontScale).sp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>()) // tap anywhere to open the app
    ) {
        // Top: the number ("left today") on the left, the "+" button on the right.
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                // The bottom padding here IS the clear gap above the progress bar.
                .padding(
                    start = SidePadding,
                    end = EndPadding,
                    top = sizes.topPadding.dp,
                    bottom = if (isBudgetSet) CONTENT_BAR_GAP.dp else 0.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                if (isBudgetSet) {
                    Text(
                        text = left,
                        style = TextStyle(
                            color = textColor,
                            fontSize = textSize(sizes.number),
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Set your budget",
                        style = TextStyle(color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }
            if (isBudgetSet) AddButton(sizes.button.dp, colors)
        }

        // Bottom: two pill-shaped progress bars with space around them. The daily bar is on top, the weekly one
        // underneath, each with its label above it when the widget is tall enough (see WidgetLayout.kt).
        if (isBudgetSet) {
            val barWidth = size.width - SidePadding * 2
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(start = SidePadding, end = SidePadding, bottom = sizes.bottomPadding.dp)
            ) {
                sizes.label?.let { labelSize ->
                    Text(
                        text = "left today",
                        style = TextStyle(color = quietColor, fontSize = textSize(labelSize))
                    )
                }
                ProgressPill(progress = summary.progress, width = barWidth, height = sizes.bar.dp, colors = colors)
                Spacer(modifier = GlanceModifier.height(BAR_GAP.dp))
                sizes.label?.let { labelSize ->
                    // "left this week" on the left, and what is left of the week on the right.
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = "left this week",
                            style = TextStyle(color = quietColor, fontSize = textSize(labelSize)),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = weeklyLeft,
                            style = TextStyle(
                                color = textColor,
                                fontSize = textSize(labelSize),
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
                ProgressPill(progress = summary.weeklyProgress, width = barWidth, height = sizes.bar.dp, colors = colors)
            }
        }
    }
}

/** A long pill: a track with a colored fill (green -> amber -> red) on top. */
@Composable
private fun ProgressPill(progress: Float, width: Dp, height: Dp, colors: WidgetColors) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height)
            .background(fixed(colors.track))
            .cornerRadius(height / 2)
    ) {
        if (progress > 0f) {
            // Glance can't do "fill 40% of the width", so work out the fill width in dp ourselves.
            // It is never narrower than the bar is tall, so a tiny fill still looks like a pill.
            val fillWidth = maxOf(width * progress, height)
            // The fill color is adjusted when it would blend into the widget's background.
            val fillColor = fillColorOn(progressColor(progress), colors.background)
            Box(
                modifier = GlanceModifier
                    .width(fillWidth)
                    .height(height)
                    .background(fixed(fillColor))
                    .cornerRadius(height / 2)
            ) {}
        }
    }
}

/** The round "+" button: black, like the Add button in the app. It opens a floating add card. */
@Composable
private fun AddButton(size: Dp, colors: WidgetColors) {
    val plusLength = size * PLUS_SHARE

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(size)
            .background(fixed(colors.text))
            .cornerRadius(size / 2)
            .clickable(actionStartActivity<QuickAddActivity>())
    ) {
        // The "+" is drawn as two thin bars, so its thickness is exactly what we choose.
        Box(
            modifier = GlanceModifier
                .width(plusLength)
                .height(PlusThickness)
                .background(fixed(colors.background))
                .cornerRadius(PlusThickness / 2)
        ) {}
        Box(
            modifier = GlanceModifier
                .width(PlusThickness)
                .height(plusLength)
                .background(fixed(colors.background))
                .cornerRadius(PlusThickness / 2)
        ) {}
    }
}
