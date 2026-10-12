package com.rishabh.budgetapp

import com.rishabh.budgetapp.widget.CONTENT_BAR_GAP
import com.rishabh.budgetapp.widget.textBlockHeight
import com.rishabh.budgetapp.widget.widgetSizes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests for the widget's sizing rules: whatever size the phone draws the widget at, nothing may overlap.
class WidgetLayoutTest {
    // Heights from the smallest allowed (40) to a tall widget, widths from narrow to very wide.
    private val heights = (40..220 step 5).map { it.toFloat() }
    private val widths = (160..420 step 20).map { it.toFloat() }

    @Test
    fun contentPlusBarsPlusGapAlwaysFitsInsideTheHeight() {
        for (h in heights) for (w in widths) {
            val s = widgetSizes(w, h, amountLength = 8)
            val used = s.topPadding + s.contentHeight + CONTENT_BAR_GAP + s.barBlock + s.bottomPadding
            assertEquals("height $h", h, used, 0.01f) // the pieces add up to exactly the height
        }
    }

    @Test
    fun theNumberAndTheButtonNeverTallerThanTheContentArea() {
        for (h in heights) for (w in widths) {
            val s = widgetSizes(w, h, amountLength = 8)
            assertTrue("number at $w x $h", textBlockHeight(s) <= s.contentHeight + 0.01f)
            assertTrue("button at $w x $h", s.button <= s.contentHeight + 0.01f)
        }
    }

    @Test
    fun thereIsAlwaysTheFiveDpGapAboveTheBars() {
        // The gap is its own slice of the height, so the content (<= contentHeight) can never come closer than it.
        assertEquals(5f, CONTENT_BAR_GAP, 0f)
        for (h in heights) {
            val s = widgetSizes(300f, h, amountLength = 8)
            assertTrue(s.topPadding + s.contentHeight + CONTENT_BAR_GAP + s.barBlock + s.bottomPadding <= h + 0.01f)
        }
    }

    @Test
    fun aLongAmountStillFitsBesideTheButton() {
        for (w in widths) {
            val s = widgetSizes(w, 80f, amountLength = 10) // like -$1,234.56
            val textWidth = s.number * 0.58f * 10
            val room = w - 16 - 12 - s.button - 8
            assertTrue("width $w", textWidth <= room + 0.5f)
        }
    }

    @Test
    fun aTypicalOneCellWidgetShowsBothLabelsAndAReadableNumber() {
        for (h in listOf(60f, 65f, 70f, 75f, 80f)) {
            val s = widgetSizes(300f, h, amountLength = 8)
            assertNotNull("labels at height $h", s.label)
            assertTrue("number at height $h", s.number >= 12f)
            assertTrue("button at height $h", s.button >= 18f)
        }
    }

    @Test
    fun aCommonOneCellHeightKeepsTheNumberAbout18OrBigger() {
        val s = widgetSizes(300f, 70f, amountLength = 8)
        assertTrue(s.number >= 17f)
    }

    @Test
    fun anUnusuallyShortWidgetDropsTheLabelsAsALastResort() {
        val s = widgetSizes(300f, 40f, amountLength = 8)
        assertNull(s.label)
        assertTrue(s.number >= 10f)
    }

    @Test
    fun theLabelsOnlyShowWhenTheNumberStaysReadable() {
        for (h in heights) {
            val s = widgetSizes(300f, h, amountLength = 8)
            if (s.label != null) assertTrue("height $h", s.number >= 11.9f)
        }
    }

    @Test
    fun theNumberNeverGetsBiggerThanForty() {
        assertTrue(widgetSizes(400f, 220f, amountLength = 6).number <= 40f)
    }
}
