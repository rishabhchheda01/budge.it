package com.rishabh.budgetapp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rishabh.budgetapp.ui.ColorStrip
import com.rishabh.budgetapp.ui.theme.BudgetTheme
import com.rishabh.budgetapp.ui.theme.WidgetColorChoices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The strip of widget colors at the bottom of the color screen: the arrows and picking a color. */
@RunWith(AndroidJUnit4::class)
class ColorStripTest {
    @get:Rule
    val compose = createComposeRule()

    private var picked: Color? = null

    private fun show(selected: Color = WidgetColorChoices.first(), startColor: Color? = WidgetColorChoices.first()) {
        compose.setContent {
            BudgetTheme(darkTheme = false) {
                ColorStrip(selected = selected, startColor = startColor, onSelect = { picked = it })
            }
        }
    }

    @Test
    fun theFirstPageHasNoPreviousArrowButHasANextOne() {
        show()
        compose.onNodeWithContentDescription("Previous colors").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Next colors").assertIsEnabled()
    }

    @Test
    fun theNextArrowMovesToTheNextPageAndTheBackArrowComesAlive() {
        show()
        compose.onNodeWithContentDescription("Next colors").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Previous colors").assertIsEnabled()
    }

    @Test
    fun theLastPageHasNoNextArrow() {
        show()
        repeat(WidgetColorChoices.size / 5) {
            compose.onNodeWithContentDescription("Next colors").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithContentDescription("Next colors").assertIsNotEnabled()
    }

    @Test
    fun theStripOpensOnThePageOfTheSavedColor() {
        // The 7th color is on the second page (5 colors per page), so the back arrow must be enabled at once.
        show(selected = WidgetColorChoices[6], startColor = WidgetColorChoices[6])
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Previous colors").assertIsEnabled()
    }

    @Test
    fun tappingASwatchReportsOneOfTheChoices() {
        show()
        compose.onAllNodesWithTag("swatch")[1].performClick()
        assertTrue(picked in WidgetColorChoices)
        assertEquals(WidgetColorChoices[1], picked)
    }
}
