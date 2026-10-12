package com.rishabh.budgetapp.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rishabh.budgetapp.ui.theme.WidgetColorChoices
import com.rishabh.budgetapp.ui.theme.contentColorFor
import kotlinx.coroutines.launch

// Sizes to taste:
private const val ColorsPerPage = 6   // how many colors you see at once (the colors are in ui/theme/Theme.kt)
private val SwatchSize = 40.dp

/**
 * The color strip at the bottom of the color screen: a row of colors with an arrow on each side. The arrow
 * slides to the next (or previous) page of colors smoothly, and you can also just swipe.
 * [startColor] is the color that was saved before; the strip opens on the page that has it.
 */
@Composable
internal fun ColorStrip(
    selected: Color,
    startColor: Color?,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val pages = remember { WidgetColorChoices.chunked(ColorsPerPage) }
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    // Once the saved color is known, jump to the page that holds it.
    LaunchedEffect(startColor) {
        if (startColor != null) {
            val index = WidgetColorChoices.indexOf(startColor)
            if (index >= 0) pagerState.scrollToPage(index / ColorsPerPage)
        }
    }

    // Slide to another page. The 450 is the speed in milliseconds (bigger = slower).
    fun goTo(page: Int) {
        scope.launch {
            pagerState.animateScrollToPage(page, animationSpec = tween(450, easing = FastOutSlowInEasing))
        }
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
        ) {
            ArrowButton(
                forward = false,
                enabled = pagerState.currentPage > 0,
                onClick = { goTo(pagerState.currentPage - 1) }
            )
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages[page].forEach { color ->
                        Swatch(color = color, isSelected = color == selected, onClick = { onSelect(color) })
                    }
                    // Keep the last page's circles lined up with the pages before it.
                    repeat(ColorsPerPage - pages[page].size) { Box(modifier = Modifier.size(SwatchSize)) }
                }
            }
            ArrowButton(
                forward = true,
                enabled = pagerState.currentPage < pages.lastIndex,
                onClick = { goTo(pagerState.currentPage + 1) }
            )
        }
    }
}

@Composable
private fun ArrowButton(forward: Boolean, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = if (forward) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = if (forward) "Next colors" else "Previous colors",
            // Faded at either end, where there is nothing more to show.
            modifier = Modifier.alpha(if (enabled) 1f else 0.3f)
        )
    }
}

/** One round color. The chosen one has a check mark and a thicker ring. */
@Composable
private fun Swatch(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val ring = if (isSelected) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.outline
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(SwatchSize)
            .testTag("swatch") // lets automated UI tests find the swatches
            .clip(CircleShape)
            .background(color)
            .border(width = if (isSelected) 2.dp else 1.dp, color = ring, shape = CircleShape)
            .clickable(onClick = onClick)
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = contentColorFor(color),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
