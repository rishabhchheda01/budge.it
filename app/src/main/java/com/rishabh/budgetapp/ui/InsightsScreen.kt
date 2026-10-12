package com.rishabh.budgetapp.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.budgetapp.BudgetPeriod
import com.rishabh.budgetapp.budgetDate
import com.rishabh.budgetapp.currencyFormat
import com.rishabh.budgetapp.data.Transaction
import com.rishabh.budgetapp.periodStartDate
import com.rishabh.budgetapp.spentPerDay
import com.rishabh.budgetapp.ui.theme.progressColor
import com.rishabh.budgetapp.weekLimits
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// The Insights page: a bar graph of a week's spending, Sunday to Saturday.
// Sizes to taste:
private val ChartHeight = 240.dp      // the whole graph area, including room for the stars
private val StarSpace = 32.dp         // room kept above the tallest bar for the star
private val StarSize = 20.dp
private val BarWidth = 22.dp
private val BarCorner = 8.dp          // how round the bar corners are (4 = slight, 10 = very round)
private val MinBar = 4.dp             // a bar with any spending is never thinner than this
private const val WEEKS_BACK = 3      // how many weeks before this one you can look at (one month is stored)
private const val WEEK_SLIDE_MS = 450 // how long the slide to another week takes

/**
 * The week at a glance: a gray bar per day = that day's budget, with the spending in color on top.
 * One page per week: this week and up to [WEEKS_BACK] weeks before it. Use the "<" and ">" arrows (they slide
 * smoothly) or swipe to move between weeks.
 */
@Composable
internal fun InsightsContent(
    budgetPeriod: BudgetPeriod,
    budgetAmount: Double,
    transactions: List<Transaction>,
    rollover: LocalTime,
    onBack: () -> Unit
) {
    // Which day is today, and where this week started.
    val today = budgetDate(ZonedDateTime.now(), rollover)
    val thisWeekStart = periodStartDate(today, BudgetPeriod.WEEKLY)
    val todayIndex = today.dayOfWeek.value % 7 // Sunday = 0 ... Saturday = 6

    // Page 0 is the oldest week, the last page is this week (where the page opens).
    val pageCount = WEEKS_BACK + 1
    val thisWeekPage = pageCount - 1
    val pagerState = rememberPagerState(initialPage = thisWeekPage) { pageCount }
    val scope = rememberCoroutineScope()
    // The arrows slide to the next page. The 450 is the speed in milliseconds (bigger = slower).
    fun goTo(page: Int) {
        scope.launch {
            pagerState.animateScrollToPage(page, animationSpec = tween(WEEK_SLIDE_MS, easing = FastOutSlowInEasing))
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // The title is centered; the back arrow sits on the left edge (same as Settings).
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-12).dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Insights",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // "<  Oct 4 – Oct 10  >": the dates in the middle, an arrow on each side.
        WeekSwitcher(
            currentPage = pagerState.currentPage,
            pageCount = pageCount,
            weekStartOf = { page -> thisWeekStart.minusWeeks((thisWeekPage - page).toLong()) },
            onPrevious = { goTo(pagerState.currentPage - 1) },
            onNext = { goTo(pagerState.currentPage + 1) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            pageSpacing = 24.dp,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            WeekPage(
                weekStart = thisWeekStart.minusWeeks((thisWeekPage - page).toLong()),
                isThisWeek = page == thisWeekPage,
                todayIndex = todayIndex,
                budgetPeriod = budgetPeriod,
                budgetAmount = budgetAmount,
                transactions = transactions,
                rollover = rollover
            )
        }
    }
}

/** The dates of the shown week, large, between a "<" and a ">" arrow. */
@Composable
private fun WeekSwitcher(
    currentPage: Int,
    pageCount: Int,
    weekStartOf: (Int) -> LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val quietText = MaterialTheme.colorScheme.onSurfaceVariant
    val dateFormat = remember { DateTimeFormatter.ofPattern("MMM d") }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        WeekArrow(forward = false, enabled = currentPage > 0, onClick = onPrevious)
        // The dates fade into the next ones as the page slides.
        Crossfade(
            targetState = currentPage,
            animationSpec = tween(WEEK_SLIDE_MS, easing = FastOutSlowInEasing),
            label = "week",
            modifier = Modifier.weight(1f)
        ) { page ->
            val start = weekStartOf(page)
            val weeksAgo = pageCount - 1 - page
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${start.format(dateFormat)} – ${start.plusDays(6).format(dateFormat)}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when (weeksAgo) {
                        0 -> "THIS WEEK"
                        1 -> "LAST WEEK"
                        else -> "$weeksAgo WEEKS AGO"
                    },
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = quietText
                )
            }
        }
        WeekArrow(forward = true, enabled = currentPage < pageCount - 1, onClick = onNext)
    }
}

@Composable
private fun WeekArrow(forward: Boolean, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = if (forward) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = if (forward) "Next week" else "Previous week",
            // Faded at either end, where there is no week to go to.
            modifier = Modifier
                .size(32.dp)
                .alpha(if (enabled) 1f else 0.3f)
        )
    }
}

/** One week: its graph, and the numbers of the tapped day underneath. */
@Composable
private fun WeekPage(
    weekStart: LocalDate,
    isThisWeek: Boolean,
    todayIndex: Int,
    budgetPeriod: BudgetPeriod,
    budgetAmount: Double,
    transactions: List<Transaction>,
    rollover: LocalTime
) {
    val currency = remember { currencyFormat() }
    val quietText = MaterialTheme.colorScheme.onSurfaceVariant

    // A finished week has no "future" days: all seven count as past. (The budget for the past weeks is worked
    // out with the budget you have NOW, since the app does not remember older budget settings.)
    val lastDay = if (isThisWeek) todayIndex else 6
    val spent = remember(transactions, rollover, weekStart) { spentPerDay(transactions, weekStart, rollover = rollover) }
    val limits = remember(budgetPeriod, budgetAmount, spent, lastDay) {
        weekLimits(budgetPeriod, budgetAmount, spent, lastDay)
    }

    // The day whose numbers are shown under the graph. Starts on today (or the last day of an older week);
    // tap a bar to change it.
    var selected by rememberSaveable(weekStart.toString()) { mutableStateOf(lastDay) }

    Column(modifier = Modifier.fillMaxWidth()) {
        WeekChart(
            spent = spent,
            limits = limits,
            todayIndex = lastDay,
            selected = selected,
            onSelect = { selected = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // "Tue · $12.00 / $20.00" for the tapped day.
        val dayName = weekStart.plusDays(selected.toLong()).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        Text(
            text = dayName,
            fontSize = 13.sp,
            letterSpacing = 1.5.sp,
            color = quietText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${currency.format(spent[selected])} / ${currency.format(limits[selected])}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

/** Seven columns, Sunday first. Each one is tappable. */
@Composable
private fun WeekChart(
    spent: List<Double>,
    limits: List<Double>,
    todayIndex: Int,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    // Every bar is drawn as a share of the tallest value in the week (budget or spending), so the
    // biggest one reaches the top and the rest follow.
    val tallest = (0..6).maxOf { maxOf(limits[it], spent[it]) }.coerceAtLeast(0.01)
    val letters = listOf("S", "M", "T", "W", "T", "F", "S")
    val quietText = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (day in 0..6) {
                DayBar(
                    spent = spent[day],
                    limit = limits[day],
                    tallest = tallest,
                    isFuture = day > todayIndex,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(day) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            for (day in 0..6) {
                Text(
                    text = letters[day],
                    fontSize = 14.sp,
                    fontWeight = if (day == selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (day == selected) MaterialTheme.colorScheme.onBackground else quietText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** One day: the gray budget bar, the colored spending bar over it, and a star when you went over. */
@Composable
private fun DayBar(
    spent: Double,
    limit: Double,
    tallest: Double,
    isFuture: Boolean,
    modifier: Modifier = Modifier
) {
    val isOver = spent > limit + 0.005
    // How full the spending is against that day's budget: this picks the green -> amber -> red color.
    val progress = when {
        limit > 0 -> (spent / limit).toFloat().coerceIn(0f, 1f)
        spent > 0 -> 1f
        else -> 0f
    }
    // Heights as a share of the tallest value. Animated, so the bars grow in when the page opens.
    val limitShare by animateFloatAsState(
        targetValue = (limit / tallest).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "limit"
    )
    val spentShare by animateFloatAsState(
        targetValue = (spent / tallest).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "spent"
    )

    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val barArea = maxHeight - StarSpace
        val limitHeight = barHeight(barArea, limitShare, limit > 0)
        val spentHeight = barHeight(barArea, spentShare, spent > 0 && !isFuture)

        // The gray budget bar.
        Bar(
            height = limitHeight,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        // The colored spending bar, drawn over the gray one. Taller than the gray one = over budget.
        if (!isFuture) {
            Bar(
                height = spentHeight,
                color = progressColor(progress),
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        // The star sits just above whichever bar is taller.
        if (isOver && !isFuture) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Over budget",
                tint = progressColor(1f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -(maxOf(limitHeight, spentHeight) + 6.dp))
                    .size(StarSize)
            )
        }
    }
}

// A share of the bar area as a height, but never thinner than MinBar when there is something to show.
private fun barHeight(area: Dp, share: Float, hasValue: Boolean): Dp =
    if (hasValue) maxOf(area * share, MinBar) else 0.dp

@Composable
private fun Bar(height: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(BarWidth)
            .height(height)
            // A rectangle with softly rounded corners (all four). Change the 4.dp to taste (8 to 10 is soft).
            .clip(RoundedCornerShape(BarCorner))
            .background(color)
    )
}
