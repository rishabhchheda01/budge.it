package com.rishabh.budgetapp.widget

import androidx.compose.ui.unit.dp

// How big everything in the widget is, worked out from the widget's REAL size. Phones and launchers draw
// the widget at different sizes, so nothing here is a fixed pixel layout. Instead the height is shared out:
//
//   top padding
//   [ content: the number on the left, the "+" button on the right ]
//   gap (always CONTENT_BAR_GAP)
//   [ "left today" label + the daily bar ]
//   (a small space)
//   [ "left this week" label + the weekly bar ]
//   bottom padding
//
// The two bars (and their labels, when there is room) and the paddings are worked out first; whatever height
// is LEFT is the content area, and the number and the button are made to fit inside it. So they can never
// touch the bars or each other. The widget is ONE cell tall, so space is tight: the labels always show, and
// the number and the "+" button take what is left (about 18sp / 23dp on a typical phone). Only on an unusually
// short widget, where the number would drop below NUMBER_FOR_LABELS, are the labels left out as a last resort.
// The numbers below are the rules (change them to taste). Both the real widget (BudgetWidget.kt) and the
// look-alike in the color screen (ui/WidgetPreview.kt) use this file, so they always agree.

// ---- The two progress bars ----
internal const val BAR_SHARE = 0.065f         // thickness as a share of the widget height ...
internal const val BAR_MIN = 4f               // ... but never thinner than this (dp) ...
internal const val BAR_MAX = 8f               // ... or thicker than this (dp)
internal const val BAR_GAP = 3f               // dp of space between the daily block and the weekly block

// ---- Space ----
internal val SidePadding = 16.dp              // left and right of the content and the bar
internal val EndPadding = 12.dp               // right of the "+" button
internal const val CONTENT_BAR_GAP = 5f       // dp of clear space between the content and the bar, ALWAYS
private const val TOP_SHARE = 0.06f           // top and bottom padding scale with the height, between ...
private const val BOTTOM_SHARE = 0.08f
private const val TOP_MIN = 3f                // ... a small value on short widgets ...
private const val TOP_MAX = 6f                // ... and a little more on tall ones
private const val BOTTOM_MIN = 4f
private const val BOTTOM_MAX = 6f

// ---- The "+" button ----
internal const val BUTTON_SHARE = 0.34f       // diameter as a share of the widget height, kept between ...
private const val BUTTON_MIN = 30f            // ... these two (dp), and never taller than the content area
private const val BUTTON_MAX = 44f
internal const val PLUS_SHARE = 0.55f         // the "+" sign's length as a share of the circle
internal val PlusThickness = 2.dp             // how thin the "+" lines are

// ---- Text (sizes are in dp-equivalent: the screen code converts them for the phone's font-size setting) ----
private const val NUMBER_MAX = 40f            // biggest number
private const val NUMBER_GUARD = 10f          // never smaller than this, even on very narrow widgets
private const val LABEL_MIN = 7f             // the "left today" / "left this week" labels above the bars
private const val LABEL_MAX = 10f
private const val LABEL_SHARE = 0.115f        // label size as a share of the widget height, between min and max
private const val NUMBER_FOR_LABELS = 12f     // last resort: the labels are left out if the number would drop below this
internal const val LINE_HEIGHT = 1.25f         // a line of text is about this many times its font size tall
private const val DIGIT_WIDTH = 0.58f         // one character is about this many times the font size wide
private const val TEXT_BUTTON_GAP = 8f        // dp of clear space between the number and the "+" button

/** Everything the widget needs to lay itself out. All values are dp. */
internal data class WidgetSizes(
    val topPadding: Float,
    val bottomPadding: Float,
    val bar: Float,
    val button: Float,
    /** The number's size. */
    val number: Float,
    /** The size of the "left today" / "left this week" labels, or null when the widget is too short for them. */
    val label: Float?,
    /** The height left for the content (number and button) between the top padding and the gap. */
    val contentHeight: Float,
    /** The height of both bars together with their labels and the space between them. */
    val barBlock: Float
)

/**
 * Works out the sizes for a widget [width] x [height] dp showing an amount that is [amountLength] characters long
 * (a longer amount gets a smaller number so it still fits beside the "+" button).
 */
internal fun widgetSizes(width: Float, height: Float, amountLength: Int): WidgetSizes {
    val bar = (height * BAR_SHARE).coerceIn(BAR_MIN, BAR_MAX)
    val top = (height * TOP_SHARE).coerceIn(TOP_MIN, TOP_MAX)
    val bottom = (height * BOTTOM_SHARE).coerceIn(BOTTOM_MIN, BOTTOM_MAX)
    val labelSize = (height * LABEL_SHARE).coerceIn(LABEL_MIN, LABEL_MAX)

    // The height left for the content once the paddings, the gap and the bars have taken their share.
    fun contentFor(barBlock: Float) = (height - top - bottom - CONTENT_BAR_GAP - barBlock).coerceAtLeast(0f)

    // Two bars, each with a label above it, or just two bars. Labels only if the number can stay big.
    val blockWithLabels = 2 * (LINE_HEIGHT * labelSize + bar) + BAR_GAP
    val blockPlain = 2 * bar + BAR_GAP
    val showLabels = contentFor(blockWithLabels) / LINE_HEIGHT >= NUMBER_FOR_LABELS
    val barBlock = if (showLabels) blockWithLabels else blockPlain
    val content = contentFor(barBlock)

    val button = (height * BUTTON_SHARE).coerceIn(BUTTON_MIN, BUTTON_MAX).coerceAtMost(content)

    // The number fills the content height (up to its maximum).
    var number = (content / LINE_HEIGHT).coerceAtMost(NUMBER_MAX)

    // Fit the number's width into the space beside the button.
    val widthRoom = width - SidePadding.value - EndPadding.value - button - TEXT_BUTTON_GAP
    number = minOf(number, widthRoom / (maxOf(amountLength, 1) * DIGIT_WIDTH))

    return WidgetSizes(
        topPadding = top,
        bottomPadding = bottom,
        bar = bar,
        button = button,
        number = number.coerceAtLeast(NUMBER_GUARD),
        label = if (showLabels) labelSize else null,
        contentHeight = content,
        barBlock = barBlock
    )
}

/** How tall the number is, with line spacing. Used by the tests. */
internal fun textBlockHeight(sizes: WidgetSizes): Float = LINE_HEIGHT * sizes.number
