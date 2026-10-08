package com.rishabh.budgetapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.budgetapp.ui.theme.KdamThmorPro
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.abs

// A "slot machine" time picker: two wheels (hour 00-23, minute 00-59) that spin, snap into place and
// loop around forever. The number in the middle band is the chosen one. Sizes to taste:
private val RowHeight = 48.dp          // height of one number
private const val ROWS_SHOWN = 3       // how many numbers you can see at once (use an odd number)
private val WheelWidth = 64.dp
private const val FADE = 0.75f         // how much the numbers above/below the middle fade (0 = none, 1 = vanish)
private const val SHRINK = 0.25f       // how much they shrink (0 = none)
private const val LOOP_ROWS = 100_000  // a very long list that starts in the middle, so it never "ends"

/** Pick an hour (0-23) and a minute (0-59). Calls back when a wheel comes to rest. */
@Composable
internal fun TimeRoller(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(240.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.outlineVariant)
    ) {
        // The middle band that marks the chosen time.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(RowHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.background)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Wheel(count = 24, selected = hour, onSelected = onHourChange)
            Text(
                text = ":",
                fontFamily = KdamThmorPro,
                fontSize = 30.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Wheel(count = 60, selected = minute, onSelected = onMinuteChange)
        }
    }
}

@Composable
private fun Wheel(count: Int, selected: Int, onSelected: (Int) -> Unit) {
    val rowHeightPx = with(LocalDensity.current) { RowHeight.toPx() }
    val haptics = LocalHapticFeedback.current
    val latestOnSelected by rememberUpdatedState(onSelected)

    // Start in the middle of the long list, on a row that is a multiple of [count], so row % count = value.
    val startRow = LOOP_ROWS / 2 - (LOOP_ROWS / 2) % count
    // The wheel shows ROWS_SHOWN numbers; the chosen one is the middle one, so the first shown is one earlier.
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startRow + selected - ROWS_SHOWN / 2)

    // A small tick for every number that passes the middle, like a slot machine.
    LaunchedEffect(state) {
        snapshotFlow { state.middleRow() }
            .filterNotNull()
            .distinctUntilChanged()
            .drop(1)
            .collect { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    // Report the chosen number once the wheel has stopped moving.
    LaunchedEffect(state) {
        snapshotFlow { if (state.isScrollInProgress) null else state.middleRow() }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { row -> latestOnSelected(row % count) }
    }

    // If the value is changed from outside (e.g. settings finished loading), jump the wheel to it.
    LaunchedEffect(selected) {
        val row = state.middleRow()
        if (row != null && row % count != selected && !state.isScrollInProgress) {
            state.scrollToItem(startRow + selected - ROWS_SHOWN / 2)
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state), // makes it settle exactly on a number
        modifier = Modifier
            .width(WheelWidth)
            .height(RowHeight * ROWS_SHOWN)
    ) {
        items(LOOP_ROWS) { row ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RowHeight)
                    // Numbers fade and shrink the further they are from the middle. This runs while drawing,
                    // so it follows your finger smoothly without redrawing the whole screen.
                    .graphicsLayer {
                        val info = state.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == row }
                        val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                        val distance = if (item == null) 1f else
                            (abs(item.offset + item.size / 2f - middle) / rowHeightPx).coerceAtMost(1f)
                        alpha = 1f - FADE * distance
                        val scale = 1f - SHRINK * distance
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                Text(
                    text = "%02d".format(row % count),
                    fontFamily = KdamThmorPro,
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

/** The row currently in the middle of the wheel (null until the wheel has been laid out). */
private fun LazyListState.middleRow(): Int? {
    val info = layoutInfo
    val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index
}
