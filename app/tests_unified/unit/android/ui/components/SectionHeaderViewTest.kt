package com.multisensor.recording.ui.components

import android.content.Context
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.util.TypedValue

@RunWith(AndroidJUnit4::class)
class SectionHeaderViewTest {

    private lateinit var context: Context
    private lateinit var sectionHeaderView: SectionHeaderView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sectionHeaderView = SectionHeaderView(context)
    }

    @Test
    fun testInitialState() {
        assertNotNull(sectionHeaderView)
        assertEquals(18f, sectionHeaderView.textSize / sectionHeaderView.textSize / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, context.resources.displayMetrics), 0.1f)
    }

    @Test
    fun testSetHeaderWithMainTitle() {
        sectionHeaderView.setHeader("Multi-Sensor Recording", SectionHeaderView.HeaderStyle.MAIN_TITLE)

        assertEquals("Multi-Sensor Recording", sectionHeaderView.text)
        assertEquals(24f, sectionHeaderView.textSize / sectionHeaderView.textSize / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, context.resources.displayMetrics), 0.1f)
        assertEquals(android.widget.TextView.TEXT_ALIGNMENT_CENTER, sectionHeaderView.textAlignment)
    }

    @Test
    fun testSetHeaderWithSectionHeader() {
        sectionHeaderView.setHeader("Device Configuration", SectionHeaderView.HeaderStyle.SECTION_HEADER)

        assertEquals("Device Configuration", sectionHeaderView.text)
        assertEquals(18f, sectionHeaderView.textSize / sectionHeaderView.textSize / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, context.resources.displayMetrics), 0.1f)
        assertEquals(android.widget.TextView.TEXT_ALIGNMENT_TEXT_START, sectionHeaderView.textAlignment)
    }

    @Test
    fun testSetHeaderWithSubHeader() {
        sectionHeaderView.setHeader("Connection Settings", SectionHeaderView.HeaderStyle.SUB_HEADER)

        assertEquals("Connection Settings", sectionHeaderView.text)
        assertEquals(16f, sectionHeaderView.textSize / sectionHeaderView.textSize / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, context.resources.displayMetrics), 0.1f)
        assertEquals(android.widget.TextView.TEXT_ALIGNMENT_TEXT_START, sectionHeaderView.textAlignment)
    }

    @Test
    fun testSetHeaderWithDefaultStyle() {
        sectionHeaderView.setHeader("Default Header")

        assertEquals("Default Header", sectionHeaderView.text)
        assertEquals(18f, sectionHeaderView.textSize / sectionHeaderView.textSize / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, context.resources.displayMetrics), 0.1f)
    }

    @Test
    fun testTypefaceIsBold() {
        val styles = arrayOf(
            SectionHeaderView.HeaderStyle.MAIN_TITLE,
            SectionHeaderView.HeaderStyle.SECTION_HEADER,
            SectionHeaderView.HeaderStyle.SUB_HEADER
        )

        for (style in styles) {
            sectionHeaderView.setHeader("Test Header", style)
            assertTrue(
                "Header should be bold for style $style",
                sectionHeaderView.typeface.isBold || sectionHeaderView.typeface.style == Typeface.BOLD
            )
        }
    }

    @Test
    fun testSetHeaderTextColor() {
        sectionHeaderView.setHeaderTextColor(android.R.color.holo_blue_light)

        assertNotNull(sectionHeaderView)
    }

    @Test
    fun testSetDarkTheme() {
        sectionHeaderView.setDarkTheme()

        assertNotNull(sectionHeaderView)
    }

    @Test
    fun testSetLightTheme() {
        sectionHeaderView.setLightTheme()

        assertNotNull(sectionHeaderView)
    }

    @Test
    fun testTextSizesForAllStyles() {
        sectionHeaderView.setHeader("Test", SectionHeaderView.HeaderStyle.MAIN_TITLE)
        assertEquals(24f, sectionHeaderView.textSize / sectionHeaderView.textSize / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, context.resources.displayMetrics), 0.1f)

        sectionHeaderView.setHeader("Test", SectionHeaderView.HeaderStyle.SECTION_HEADER)
        assertEquals(18f, sectionHeaderView.textSize / sectionHeaderView.textSize / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, context.resources.displayMetrics), 0.1f)

        sectionHeaderView.setHeader("Test", SectionHeaderView.HeaderStyle.SUB_HEADER)
        assertEquals(16f, sectionHeaderView.textSize / sectionHeaderView.textSize / TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1f, context.resources.displayMetrics), 0.1f)
    }

    @Test
    fun testTextAlignmentForAllStyles() {
        sectionHeaderView.setHeader("Test", SectionHeaderView.HeaderStyle.MAIN_TITLE)
        assertEquals(android.widget.TextView.TEXT_ALIGNMENT_CENTER, sectionHeaderView.textAlignment)

        sectionHeaderView.setHeader("Test", SectionHeaderView.HeaderStyle.SECTION_HEADER)
        assertEquals(android.widget.TextView.TEXT_ALIGNMENT_TEXT_START, sectionHeaderView.textAlignment)

        sectionHeaderView.setHeader("Test", SectionHeaderView.HeaderStyle.SUB_HEADER)
        assertEquals(android.widget.TextView.TEXT_ALIGNMENT_TEXT_START, sectionHeaderView.textAlignment)
    }

    @Test
    fun testMultipleHeaderChanges() {
        sectionHeaderView.setHeader("First Header", SectionHeaderView.HeaderStyle.MAIN_TITLE)
        assertEquals("First Header", sectionHeaderView.text)

        sectionHeaderView.setHeader("Second Header", SectionHeaderView.HeaderStyle.SECTION_HEADER)
        assertEquals("Second Header", sectionHeaderView.text)

        sectionHeaderView.setHeader("Third Header", SectionHeaderView.HeaderStyle.SUB_HEADER)
        assertEquals("Third Header", sectionHeaderView.text)
    }

    @Test
    fun testEmptyHeaderText() {
        sectionHeaderView.setHeader("")
        assertEquals("", sectionHeaderView.text)
    }

    @Test
    fun testLongHeaderText() {
        val longText = "This is a very long header text that should still be handled properly by the component"
        sectionHeaderView.setHeader(longText)
        assertEquals(longText, sectionHeaderView.text)
    }

    @Test
    fun testSpecialCharactersInHeader() {
        val specialText = "Header with émojis 📱 and symbols ★ ♦ ♠"
        sectionHeaderView.setHeader(specialText)
        assertEquals(specialText, sectionHeaderView.text)
    }

    @Test
    fun testHeaderWithNumbers() {
        val numberText = "Section 1.2.3 - Configuration"
        sectionHeaderView.setHeader(numberText)
        assertEquals(numberText, sectionHeaderView.text)
    }

    @Test
    fun testThemeChangesAfterHeaderSet() {
        sectionHeaderView.setHeader("Test Header", SectionHeaderView.HeaderStyle.MAIN_TITLE)
        sectionHeaderView.setDarkTheme()
        assertEquals("Test Header", sectionHeaderView.text)

        sectionHeaderView.setLightTheme()
        assertEquals("Test Header", sectionHeaderView.text)
    }
}
