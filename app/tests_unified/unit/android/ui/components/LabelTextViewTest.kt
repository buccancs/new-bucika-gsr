package com.multisensor.recording.ui.components

import android.content.Context
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LabelTextViewTest {

    private lateinit var context: Context
    private lateinit var labelTextView: LabelTextView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        labelTextView = LabelTextView(context)
    }

    @Test
    fun testInitialState() {
        assertNotNull(labelTextView)
        val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedSizeInPixels, labelTextView.textSize, 0.1f)
    }

    @Test
    fun testSetLabelWithFormLabel() {
        labelTextView.setLabel("Device Name:", LabelTextView.LabelStyle.FORM_LABEL)

        assertEquals("Device Name:", labelTextView.text)
        val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedSizeInPixels, labelTextView.textSize, 0.1f)
    }

    @Test
    fun testSetLabelWithDescription() {
        labelTextView.setLabel("Enter the device identifier", LabelTextView.LabelStyle.DESCRIPTION)

        assertEquals("Enter the device identifier", labelTextView.text)
        val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
        assertEquals(expectedSizeInPixels, labelTextView.textSize, 0.1f)
    }

    @Test
    fun testSetLabelWithInstruction() {
        labelTextView.setLabel("Please configure the following settings", LabelTextView.LabelStyle.INSTRUCTION)

        assertEquals("Please configure the following settings", labelTextView.text)
        val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedSizeInPixels, labelTextView.textSize, 0.1f)
    }

    @Test
    fun testSetLabelWithError() {
        labelTextView.setLabel("Invalid configuration", LabelTextView.LabelStyle.ERROR)

        assertEquals("Invalid configuration", labelTextView.text)
        val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedSizeInPixels, labelTextView.textSize, 0.1f)
    }

    @Test
    fun testSetLabelWithSuccess() {
        labelTextView.setLabel("Configuration saved successfully", LabelTextView.LabelStyle.SUCCESS)

        assertEquals("Configuration saved successfully", labelTextView.text)
        val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedSizeInPixels, labelTextView.textSize, 0.1f)
    }

    @Test
    fun testSetLabelWithDefaultStyle() {
        labelTextView.setLabel("Default Label")

        assertEquals("Default Label", labelTextView.text)
        val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedSizeInPixels, labelTextView.textSize, 0.1f)
    }

    @Test
    fun testSetLabelTextColorWithResource() {
        labelTextView.setLabelTextColor(android.R.color.holo_blue_light)

        assertNotNull(labelTextView)
    }

    @Test
    fun testSetLabelTextColorWithHex() {
        labelTextView.setLabelTextColor("#FF0000")

        assertNotNull(labelTextView)
    }

    @Test
    fun testSetDarkTheme() {
        labelTextView.setDarkTheme()

        assertNotNull(labelTextView)
    }

    @Test
    fun testSetLightTheme() {
        labelTextView.setLightTheme()

        assertNotNull(labelTextView)
    }

    @Test
    fun testSetRequired() {
        labelTextView.setLabel("Username")
        labelTextView.setRequired(true)

        assertTrue("Required field should have asterisk", labelTextView.text.toString().endsWith("*"))
        assertEquals("Username*", labelTextView.text.toString())
    }

    @Test
    fun testSetRequiredMultipleTimes() {
        labelTextView.setLabel("Password")
        labelTextView.setRequired(true)
        labelTextView.setRequired(true)

        assertEquals("Password*", labelTextView.text.toString())
        assertEquals(1, labelTextView.text.toString().count { it == '*' })
    }

    @Test
    fun testSetRequiredFalse() {
        labelTextView.setLabel("Optional Field")
        labelTextView.setRequired(false)

        assertEquals("Optional Field", labelTextView.text.toString())
        assertFalse("Non-required field should not have asterisk", labelTextView.text.toString().contains("*"))
    }

    @Test
    fun testSetClickableHelp() {
        var clicked = false
        val clickListener = android.view.View.OnClickListener { clicked = true }

        labelTextView.setClickableHelp(clickListener)

        assertTrue("Help text should be clickable", labelTextView.isClickable)

        labelTextView.performClick()
        assertTrue("Click listener should be triggered", clicked)
    }

    @Test
    fun testSetClickableHelpWithNull() {
        labelTextView.setClickableHelp(null)

        assertTrue("Should still be clickable even with null listener", labelTextView.isClickable)

        labelTextView.performClick()
    }

    @Test
    fun testAllLabelStyles() {
        val styles = arrayOf(
            LabelTextView.LabelStyle.FORM_LABEL,
            LabelTextView.LabelStyle.DESCRIPTION,
            LabelTextView.LabelStyle.INSTRUCTION,
            LabelTextView.LabelStyle.ERROR,
            LabelTextView.LabelStyle.SUCCESS
        )

        for (style in styles) {
            labelTextView.setLabel("Test Label", style)
            assertEquals("Text should be set for style $style", "Test Label", labelTextView.text)
        }
    }

    @Test
    fun testTextSizesForAllStyles() {
        labelTextView.setLabel("Test", LabelTextView.LabelStyle.FORM_LABEL)
        val expectedFormLabelSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedFormLabelSize, labelTextView.textSize, 0.1f)

        labelTextView.setLabel("Test", LabelTextView.LabelStyle.DESCRIPTION)
        val expectedDescriptionSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, context.resources.displayMetrics)
        assertEquals(expectedDescriptionSize, labelTextView.textSize, 0.1f)

        labelTextView.setLabel("Test", LabelTextView.LabelStyle.INSTRUCTION)
        val expectedInstructionSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedInstructionSize, labelTextView.textSize, 0.1f)

        labelTextView.setLabel("Test", LabelTextView.LabelStyle.ERROR)
        val expectedErrorSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedErrorSize, labelTextView.textSize, 0.1f)

        labelTextView.setLabel("Test", LabelTextView.LabelStyle.SUCCESS)
        val expectedSuccessSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedSuccessSize, labelTextView.textSize, 0.1f)
    }

    @Test
    fun testMultipleLabelChanges() {
        labelTextView.setLabel("First Label", LabelTextView.LabelStyle.FORM_LABEL)
        assertEquals("First Label", labelTextView.text)

        labelTextView.setLabel("Second Label", LabelTextView.LabelStyle.DESCRIPTION)
        assertEquals("Second Label", labelTextView.text)

        labelTextView.setLabel("Third Label", LabelTextView.LabelStyle.ERROR)
        assertEquals("Third Label", labelTextView.text)
    }

    @Test
    fun testEmptyLabelText() {
        labelTextView.setLabel("")
        assertEquals("", labelTextView.text)
    }

    @Test
    fun testLongLabelText() {
        val longText =
            "This is a very long label text that should still be handled properly by the component and wrap appropriately"
        labelTextView.setLabel(longText)
        assertEquals(longText, labelTextView.text)
    }

    @Test
    fun testSpecialCharactersInLabel() {
        val specialText = "Label with émojis 📝 and symbols ★ ♦ ♠"
        labelTextView.setLabel(specialText)
        assertEquals(specialText, labelTextView.text)
    }

    @Test
    fun testLabelWithNumbers() {
        val numberText = "Field 1.2.3:"
        labelTextView.setLabel(numberText)
        assertEquals(numberText, labelTextView.text)
    }

    @Test
    fun testRequiredWithSpecialCharacters() {
        val specialText = "Spéciál Fïeld"
        labelTextView.setLabel(specialText)
        labelTextView.setRequired(true)
        assertEquals("Spéciál Fïeld*", labelTextView.text.toString())
    }

    @Test
    fun testThemeChangesAfterLabelSet() {
        labelTextView.setLabel("Test Label", LabelTextView.LabelStyle.FORM_LABEL)
        labelTextView.setDarkTheme()
        assertEquals("Test Label", labelTextView.text)

        labelTextView.setLightTheme()
        assertEquals("Test Label", labelTextView.text)
    }

    @Test
    fun testFormFieldScenario() {
        labelTextView.setLabel("Email Address:", LabelTextView.LabelStyle.FORM_LABEL)
        labelTextView.setRequired(true)

        assertEquals("Email Address:*", labelTextView.text.toString())
        val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedSizeInPixels, labelTextView.textSize, 0.1f)
    }

    @Test
    fun testErrorMessageScenario() {
        labelTextView.setLabel("Invalid email format", LabelTextView.LabelStyle.ERROR)

        assertEquals("Invalid email format", labelTextView.text)
        val expectedSizeInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        assertEquals(expectedSizeInPixels, labelTextView.textSize, 0.1f)
    }

    @Test
    fun testHelpTextScenario() {
        var helpClicked = false
        labelTextView.setLabel("Need help?", LabelTextView.LabelStyle.DESCRIPTION)
        labelTextView.setClickableHelp { helpClicked = true }

        assertTrue(labelTextView.isClickable)
        labelTextView.performClick()
        assertTrue(helpClicked)
    }

    @Test
    fun testCombinedFeatures() {
        labelTextView.setLabel("Required Field", LabelTextView.LabelStyle.FORM_LABEL)
        labelTextView.setRequired(true)
        labelTextView.setLabelTextColor("#0000FF")

        assertEquals("Required Field*", labelTextView.text.toString())
        assertTrue(labelTextView.text.toString().endsWith("*"))
    }
}
