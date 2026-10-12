package com.rishabh.budgetapp

import androidx.compose.ui.graphics.Color
import com.rishabh.budgetapp.ui.theme.WidgetColorChoices
import com.rishabh.budgetapp.ui.theme.WidgetDarkText
import com.rishabh.budgetapp.ui.theme.WidgetLightText
import com.rishabh.budgetapp.ui.theme.contentColorFor
import com.rishabh.budgetapp.ui.theme.MIN_FILL_CONTRAST
import com.rishabh.budgetapp.ui.theme.contrastRatio
import com.rishabh.budgetapp.ui.theme.fillColorOn
import com.rishabh.budgetapp.ui.theme.progressColor
import com.rishabh.budgetapp.ui.theme.widgetColorsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests for the widget's automatic text color.
class WidgetColorTest {
    @Test
    fun lightBackgroundsGetDarkText() {
        assertEquals(WidgetDarkText, contentColorFor(Color(0xFFECD9BA))) // sand, the default
        assertEquals(WidgetDarkText, contentColorFor(Color(0xFFF4F4F2))) // off-white
    }

    @Test
    fun darkBackgroundsGetLightText() {
        assertEquals(WidgetLightText, contentColorFor(Color(0xFF14161A))) // slate
        assertEquals(WidgetLightText, contentColorFor(Color(0xFF0D0D0D))) // black
        assertEquals(WidgetLightText, contentColorFor(Color(0xFF2F5D9E))) // blue
    }

    @Test
    fun everyChoiceGetsOneOfTheTwoTextColors() {
        for (color in WidgetColorChoices) {
            val text = contentColorFor(color)
            assertTrue(text == WidgetDarkText || text == WidgetLightText)
        }
    }

    @Test
    fun theDefaultIsTheFirstChoice() {
        assertEquals(Color(0xFFECD9BA), WidgetColorChoices.first())
    }

    @Test
    fun derivedColorsDifferFromTheBackground() {
        val colors = widgetColorsFor(Color(0xFFECD9BA))
        assertNotEquals(colors.background, colors.track)
        assertNotEquals(colors.background, colors.quietText)
    }

    @Test
    fun contrastRatioIsOneForTheSameColorAndTwentyOneForBlackOnWhite() {
        assertEquals(1f, contrastRatio(Color(0xFF5BAA6E), Color(0xFF5BAA6E)), 0.001f)
        assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)
    }

    @Test
    fun contrastRatioDoesNotDependOnTheOrder() {
        val a = Color(0xFF5BAA6E)
        val b = Color(0xFFECD9BA)
        assertEquals(contrastRatio(a, b), contrastRatio(b, a), 0.0001f)
    }

    @Test
    fun theBarFillStandsOutOnEverySwatchAtEveryProgress() {
        for (background in WidgetColorChoices) {
            for (step in 0..10) {
                val fill = fillColorOn(progressColor(step / 10f), background)
                // On a mid-tone background even black or white cannot reach a high minimum, so the most we can
                // ask for is the minimum or the best the text color offers, whichever is lower.
                val best = contrastRatio(contentColorFor(background), background)
                assertTrue(
                    "bar on $background at ${step * 10}%",
                    contrastRatio(fill, background) >= minOf(MIN_FILL_CONTRAST, best) - 0.01f
                )
            }
        }
    }

    @Test
    fun aFillThatAlreadyStandsOutIsLeftAlone() {
        val green = progressColor(0f)
        val darkBackground = Color(0xFF14161A)
        assertEquals(green, fillColorOn(green, darkBackground))
    }

    @Test
    fun aGreenFillOnAGreenBackgroundIsShiftedButStaysGreenish() {
        val green = progressColor(0f)
        val shifted = fillColorOn(green, Color(0xFF9CC5A1)) // sage
        assertNotEquals(green, shifted)
        assertTrue(shifted.green > shifted.red && shifted.green > shifted.blue)
    }
}
