package com.rishabh.budgetapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import com.rishabh.budgetapp.ui.theme.WidgetBackground
import com.rishabh.budgetapp.ui.theme.WidgetQuietText
import com.rishabh.budgetapp.ui.theme.WidgetText
import com.rishabh.budgetapp.ui.theme.WidgetTrack
import com.rishabh.budgetapp.ui.theme.progressColor
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
// Colors come from ui/theme/Theme.kt.
private fun fixed(color: Color) = ColorProvider(day = color, night = color)

private val BackgroundColor = fixed(WidgetBackground)
private val TextColor = fixed(WidgetText)
private val QuietColor = fixed(WidgetQuietText)
private val TrackColor = fixed(WidgetTrack)

// Every launcher draws a "one cell" widget at a different height, so sizes scale with the
// widget's real height. These are the rules (change the numbers to taste):
private const val NUMBER_SHARE = 0.34f    // the number's size, as a share of the widget height (in sp)
private const val NumberMin = 20f         // smallest and biggest number size, in sp
private const val NumberMax = 40f
private const val BUTTON_SHARE = 0.34f    // the "+" circle's size, as a share of the widget height
private val ButtonMin = 30.dp
private val ButtonMax = 44.dp
private const val PLUS_SHARE = 0.55f      // the "+" sign's length, as a share of the circle
private val PlusThickness = 2.dp          // how thin the "+" lines are
private const val BAR_SHARE = 0.10f       // the progress bar's thickness, as a share of the height
private val BarMin = 6.dp
private val BarMax = 12.dp
private val SidePadding = 16.dp           // space left and right of the content and the bar
private val BarBottomPadding = 6.dp       // space between the bar and the bottom edge (was 12)
private val LabelSize = 10.sp             // the small "left today" text (was 12)

class BudgetWidget : GlanceAppWidget() {
    // Exact = the layout can ask for the widget's real size (LocalSize) and scale to it.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // The widget WATCHES the data (budget settings and transactions). Whenever anything changes,
        // it redraws by itself. Reading the data only once here would leave the widget showing old
        // numbers whenever Android reuses it instead of starting it fresh.
        val states = widgetStates(context)
        val firstState = states.first()

        provideContent {
            val state by states.collectAsState(initial = firstState)
            WidgetContent(isBudgetSet = state.isBudgetSet, summary = state.summary)
        }
    }

    companion object {
        suspend fun refreshAll(context: Context) {
            val widget = BudgetWidget()
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(BudgetWidget::class.java)
            ids.forEach { id -> widget.update(context, id) }
        }
    }
}

private data class WidgetState(val isBudgetSet: Boolean, val summary: BudgetSummary)

// The saved settings the widget depends on, bundled so they can travel together.
private data class Settings(
    val period: BudgetPeriod,
    val amount: Double,
    val isSet: Boolean,
    val rollover: java.time.LocalTime
)

/** Emits a new state every time the budget settings or the transactions change. */
@OptIn(ExperimentalCoroutinesApi::class)
private fun widgetStates(context: Context): Flow<WidgetState> {
    val preferences = BudgetPreferences(context)
    val repository = TransactionRepository(context)

    return combine(
        preferences.budgetPeriod,
        preferences.budgetAmount,
        preferences.isBudgetSet,
        preferences.rolloverTime
    ) { period, amount, isSet, rollover -> Settings(period, amount, isSet, rollover) }
        .flatMapLatest { settings ->
            val today = budgetDate(ZonedDateTime.now(), settings.rollover)
            repository.summaryFlow(settings.period, settings.amount, today, settings.rollover)
                .map { summary -> WidgetState(settings.isSet, summary) }
        }
}

@Composable
private fun WidgetContent(isBudgetSet: Boolean, summary: BudgetSummary) {
    val left = currencyFormat().format(summary.left)

    // Work out sizes from the widget's real size.
    val size = LocalSize.current
    val buttonSize = (size.height * BUTTON_SHARE).coerceIn(ButtonMin, ButtonMax)
    val numberSize = (size.height.value * NUMBER_SHARE).coerceIn(NumberMin, NumberMax).sp
    val barHeight = (size.height * BAR_SHARE).coerceIn(BarMin, BarMax)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BackgroundColor)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>()) // tap anywhere to open the app
    ) {
        // Top: "remaining" and the number on the left, the "+" button on the right.
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .padding(start = SidePadding, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                if (isBudgetSet) {
                    Text(
                        text = "left today",
                        style = TextStyle(color = QuietColor, fontSize = LabelSize)
                    )
                    Text(
                        text = left,
                        style = TextStyle(
                            color = TextColor,
                            fontSize = heroSize(left, numberSize, numberSize * 0.7f),
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Set your budget",
                        style = TextStyle(color = TextColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }
            if (isBudgetSet) AddButton(buttonSize)
        }

        // Bottom: a pill-shaped progress bar with space around it.
        if (isBudgetSet) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(start = SidePadding, end = SidePadding, bottom = BarBottomPadding)
            ) {
                ProgressPill(
                    progress = summary.progress,
                    width = size.width - SidePadding * 2,
                    height = barHeight
                )
            }
        }
    }
}

// Long amounts (like -$1,234.56) get a smaller size so they still fit on one line.
private fun heroSize(text: String, normal: TextUnit, small: TextUnit) =
    if (text.length > 8) small else normal

/** A long pill: a light track with a colored fill (green -> amber -> red) on top. */
@Composable
private fun ProgressPill(progress: Float, width: Dp, height: Dp) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height)
            .background(TrackColor)
            .cornerRadius(height / 2)
    ) {
        if (progress > 0f) {
            // Glance can't do "fill 40% of the width", so work out the fill width in dp ourselves.
            // It is never narrower than the bar is tall, so a tiny fill still looks like a pill.
            val fillWidth = maxOf(width * progress, height)
            val fillColor = progressColor(progress)
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
private fun AddButton(size: Dp) {
    val plusLength = size * PLUS_SHARE

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(size)
            .background(TextColor)
            .cornerRadius(size / 2)
            .clickable(actionStartActivity<QuickAddActivity>())
    ) {
        // The "+" is drawn as two thin bars, so its thickness is exactly what we choose.
        Box(
            modifier = GlanceModifier
                .width(plusLength)
                .height(PlusThickness)
                .background(BackgroundColor)
                .cornerRadius(PlusThickness / 2)
        ) {}
        Box(
            modifier = GlanceModifier
                .width(PlusThickness)
                .height(plusLength)
                .background(BackgroundColor)
                .cornerRadius(PlusThickness / 2)
        ) {}
    }
}
