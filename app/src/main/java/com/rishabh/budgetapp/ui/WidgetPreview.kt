package com.rishabh.budgetapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishabh.budgetapp.BudgetSummary
import com.rishabh.budgetapp.currencyFormat
import com.rishabh.budgetapp.ui.theme.fillColorOn
import com.rishabh.budgetapp.ui.theme.progressColor
import com.rishabh.budgetapp.ui.theme.widgetColorsFor
import com.rishabh.budgetapp.widget.PLUS_SHARE
import com.rishabh.budgetapp.widget.BAR_GAP
import com.rishabh.budgetapp.widget.CONTENT_BAR_GAP
import com.rishabh.budgetapp.widget.EndPadding
import com.rishabh.budgetapp.widget.LINE_HEIGHT
import com.rishabh.budgetapp.widget.PlusThickness
import com.rishabh.budgetapp.widget.SidePadding
import com.rishabh.budgetapp.widget.widgetSizes

// A look-alike of the home-screen widget, drawn inside the app, for the color picker.
// Home-screen widgets can't be drawn inside the app, so this is a copy of the layout in
// widget/BudgetWidget.kt. It uses the SAME sizes (they are shared from that file) and the SAME color rules
// (widgetColorsFor), so if you change the widget's look there, this follows. If you change the widget's
// LAYOUT (what sits where), change it here too.

/** How tall the preview is when there is no widget on the home screen to copy the size from. */
private val PreviewHeight = 72.dp

@Composable
internal fun WidgetPreview(
    summary: BudgetSummary,
    background: Color,
    modifier: Modifier = Modifier,
    /** The widget's real width on this phone, or null to use the full width of the screen. */
    width: Dp? = null,
    /** The widget's real height on this phone, or null for a typical one-cell height. */
    height: Dp = PreviewHeight
) {
    // The colors glide to the new ones when you pick a different swatch.
    val target = widgetColorsFor(background)
    val glide = tween<Color>(durationMillis = 250)
    val backgroundColor by animateColorAsState(target.background, glide, label = "background")
    val textColor by animateColorAsState(target.text, glide, label = "text")
    val quietColor by animateColorAsState(target.quietText, glide, label = "quiet")
    val trackColor by animateColorAsState(target.track, glide, label = "track")

    val currency = remember { currencyFormat() }
    val left = currency.format(summary.left)
    val weeklyLeft = currency.format(summary.weeklyLeft)

    // The width is only known once the preview is laid out, so the sizes are worked out inside this.
    BoxWithConstraints(
        modifier = modifier.then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()).height(height)
    ) {
    val sizes = widgetSizes(maxWidth.value, height.value, left.length)
    // Turns one of our sizes into text size for this phone's font-size setting.
    val density = LocalDensity.current
    fun textSize(size: Float) = with(density) { size.dp.toSp() }
    fun labelStyle(size: Float, color: Color) = TextStyle(
        fontSize = textSize(size),
        lineHeight = textSize(size * LINE_HEIGHT),
        letterSpacing = 0.sp,
        color = color
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
    ) {
        // Top: "left today" and the number on the left, the "+" button on the right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = SidePadding, end = EndPadding, top = sizes.topPadding.dp, bottom = CONTENT_BAR_GAP.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = left,
                    // lineHeight and letterSpacing are set so the text is laid out like the widget's own text.
                    // Without them Material's bigger defaults would shift everything.
                    style = TextStyle(
                        fontSize = textSize(sizes.number),
                        lineHeight = textSize(sizes.number * LINE_HEIGHT),
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    ),
                    maxLines = 1
                )
            }
            PreviewAddButton(size = sizes.button.dp, color = textColor, plusColor = backgroundColor)
        }

        // Bottom: the daily bar on top, the weekly bar underneath, each with its label when there is room.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SidePadding, end = SidePadding, bottom = sizes.bottomPadding.dp)
        ) {
            sizes.label?.let { labelSize ->
                Text(text = "left today", style = labelStyle(labelSize, quietColor))
            }
            PreviewBar(
                progress = summary.progress,
                height = sizes.bar.dp,
                trackColor = trackColor,
                fillColor = fillColorOn(progressColor(summary.progress), target.background)
            )
            Spacer(modifier = Modifier.height(BAR_GAP.dp))
            sizes.label?.let { labelSize ->
                // "left this week" on the left, and what is left of the week on the right.
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "left this week",
                        style = labelStyle(labelSize, quietColor),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = weeklyLeft,
                        style = labelStyle(labelSize, textColor).copy(fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                }
            }
            PreviewBar(
                progress = summary.weeklyProgress,
                height = sizes.bar.dp,
                trackColor = trackColor,
                fillColor = fillColorOn(progressColor(summary.weeklyProgress), target.background)
            )
        }
    }
    }
}

@Composable
private fun PreviewAddButton(size: Dp, color: Color, plusColor: Color) {
    val plusLength = size * PLUS_SHARE
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    ) {
        // The "+" is two thin bars, exactly like in the widget.
        Box(
            modifier = Modifier
                .width(plusLength)
                .height(PlusThickness)
                .clip(RoundedCornerShape(50))
                .background(plusColor)
        )
        Box(
            modifier = Modifier
                .width(PlusThickness)
                .height(plusLength)
                .clip(RoundedCornerShape(50))
                .background(plusColor)
        )
    }
}

@Composable
private fun PreviewBar(progress: Float, height: Dp, trackColor: Color, fillColor: Color) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
    ) {
        if (progress > 0f) {
            // Never narrower than the bar is tall, so a tiny fill still looks like a pill (same as the widget).
            val fillWidth = maxOf(maxWidth * progress, height)
            Box(
                modifier = Modifier
                    .width(fillWidth)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(50))
                    .background(fillColor)
            )
        }
    }
}
