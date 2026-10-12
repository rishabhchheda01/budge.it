package com.rishabh.budgetapp

import com.rishabh.budgetapp.ui.formatAmountForEditing
import org.junit.Assert.assertEquals
import org.junit.Test

// Tests for how a saved amount is written into the amount box (rounded to cents, no useless zeros).
class FormatAmountTest {
    @Test
    fun nothingOrNegativeShowsAnEmptyBox() {
        assertEquals("", formatAmountForEditing(0.0))
        assertEquals("", formatAmountForEditing(-5.0))
    }

    @Test
    fun wholeNumbersHaveNoDecimalPoint() {
        assertEquals("50", formatAmountForEditing(50.0))
        assertEquals("100", formatAmountForEditing(100.0))
        assertEquals("1200", formatAmountForEditing(1200.0))
    }

    @Test
    fun halfAmountsKeepOneDecimal() {
        assertEquals("12.5", formatAmountForEditing(12.5))
    }

    @Test
    fun longDecimalsAreRoundedToCents() {
        assertEquals("14.29", formatAmountForEditing(100.0 / 7))
        assertEquals("33.33", formatAmountForEditing(100.0 / 3))
        assertEquals("2.68", formatAmountForEditing(2.675)) // rounds half up
    }

    @Test
    fun anAmountThatRoundsToAWholeNumberLosesTheDecimals() {
        assertEquals("10", formatAmountForEditing(9.999))
    }
}
