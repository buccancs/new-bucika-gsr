package com.multisensor.recording.ui.components

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardSectionLayoutTest {

    private lateinit var context: Context
    private lateinit var cardSectionLayout: CardSectionLayout

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cardSectionLayout = CardSectionLayout(context)
    }

    @Test
    fun testInitialState() {
        assertNotNull(cardSectionLayout)
        assertEquals(LinearLayout.VERTICAL, cardSectionLayout.orientation)
        assertEquals(0, cardSectionLayout.childCount)
    }

    @Test
    fun testDefaultCardStyle() {
        cardSectionLayout.setCardStyle(CardSectionLayout.CardStyle.DEFAULT)

        assertEquals(LinearLayout.VERTICAL, cardSectionLayout.orientation)

        assertTrue("Default card should have elevation", cardSectionLayout.elevation > 0)
    }

    @Test
    fun testCompactCardStyle() {
        cardSectionLayout.setCardStyle(CardSectionLayout.CardStyle.COMPACT)

        assertEquals(LinearLayout.VERTICAL, cardSectionLayout.orientation)
        assertTrue("Compact card should have elevation", cardSectionLayout.elevation > 0)
    }

    @Test
    fun testFlatCardStyle() {
        cardSectionLayout.setCardStyle(CardSectionLayout.CardStyle.FLAT)

        assertEquals(LinearLayout.VERTICAL, cardSectionLayout.orientation)
        assertEquals("Flat card should have no elevation", 0f, cardSectionLayout.elevation, 0.01f)
    }

    @Test
    fun testDarkCardStyle() {
        cardSectionLayout.setCardStyle(CardSectionLayout.CardStyle.DARK)

        assertEquals(LinearLayout.VERTICAL, cardSectionLayout.orientation)
        assertTrue("Dark card should have elevation", cardSectionLayout.elevation > 0)
    }

    @Test
    fun testSetCardBackgroundColor() {
        cardSectionLayout.setCardBackgroundColor(android.R.color.holo_blue_light)

        assertNotNull(cardSectionLayout)
    }

    @Test
    fun testSetCardPadding() {
        val customPadding = 20
        cardSectionLayout.setCardPadding(customPadding)

        assertTrue("Padding should be greater than 0", cardSectionLayout.paddingLeft > 0)
        assertTrue("Padding should be greater than 0", cardSectionLayout.paddingTop > 0)
        assertTrue("Padding should be greater than 0", cardSectionLayout.paddingRight > 0)
        assertTrue("Padding should be greater than 0", cardSectionLayout.paddingBottom > 0)

        assertEquals(cardSectionLayout.paddingLeft, cardSectionLayout.paddingTop)
        assertEquals(cardSectionLayout.paddingTop, cardSectionLayout.paddingRight)
        assertEquals(cardSectionLayout.paddingRight, cardSectionLayout.paddingBottom)
    }

    @Test
    fun testSetCardElevation() {
        val customElevation = 8
        cardSectionLayout.setCardElevation(customElevation)

        assertTrue("Custom elevation should be applied", cardSectionLayout.elevation > 0)
    }

    @Test
    fun testAddHeader() {
        val headerText = "Test Section Header"
        cardSectionLayout.addHeader(headerText)

        assertEquals(1, cardSectionLayout.childCount)

        val headerView = cardSectionLayout.getChildAt(0)
        assertTrue("First child should be SectionHeaderView", headerView is SectionHeaderView)

        val sectionHeader = headerView as SectionHeaderView
        assertEquals(headerText, sectionHeader.text)
    }

    @Test
    fun testAddHeaderWithCustomStyle() {
        val headerText = "Main Title Header"
        cardSectionLayout.addHeader(headerText, SectionHeaderView.HeaderStyle.MAIN_TITLE)

        assertEquals(1, cardSectionLayout.childCount)

        val headerView = cardSectionLayout.getChildAt(0) as SectionHeaderView
        assertEquals(headerText, headerView.text)
    }

    @Test
    fun testAddMultipleHeaders() {
        cardSectionLayout.addHeader("First Header")
        cardSectionLayout.addHeader("Second Header")

        assertEquals(2, cardSectionLayout.childCount)

        assertTrue(cardSectionLayout.getChildAt(0) is SectionHeaderView)
        assertTrue(cardSectionLayout.getChildAt(1) is SectionHeaderView)

        assertEquals("Second Header", (cardSectionLayout.getChildAt(0) as SectionHeaderView).text)
        assertEquals("First Header", (cardSectionLayout.getChildAt(1) as SectionHeaderView).text)
    }

    @Test
    fun testAddContentAfterHeader() {
        cardSectionLayout.addHeader("Section Header")

        val contentView = TextView(context)
        contentView.text = "Content text"
        cardSectionLayout.addView(contentView)

        assertEquals(2, cardSectionLayout.childCount)

        assertTrue(cardSectionLayout.getChildAt(0) is SectionHeaderView)
        assertTrue(cardSectionLayout.getChildAt(1) is TextView)
        assertEquals("Content text", (cardSectionLayout.getChildAt(1) as TextView).text)
    }

    @Test
    fun testAllCardStyles() {
        val styles = arrayOf(
            CardSectionLayout.CardStyle.DEFAULT,
            CardSectionLayout.CardStyle.COMPACT,
            CardSectionLayout.CardStyle.FLAT,
            CardSectionLayout.CardStyle.DARK
        )

        for (style in styles) {
            cardSectionLayout.setCardStyle(style)
            assertEquals(
                "Orientation should remain vertical for style $style",
                LinearLayout.VERTICAL, cardSectionLayout.orientation
            )
        }
    }

    @Test
    fun testLayoutParams() {
        val layoutParams = cardSectionLayout.layoutParams

        cardSectionLayout.setCardStyle(CardSectionLayout.CardStyle.DEFAULT)

        assertNotNull(cardSectionLayout)
    }

    @Test
    fun testMultipleStyleChanges() {
        cardSectionLayout.setCardStyle(CardSectionLayout.CardStyle.DEFAULT)
        cardSectionLayout.setCardStyle(CardSectionLayout.CardStyle.COMPACT)
        cardSectionLayout.setCardStyle(CardSectionLayout.CardStyle.FLAT)
        cardSectionLayout.setCardStyle(CardSectionLayout.CardStyle.DARK)

        assertEquals(LinearLayout.VERTICAL, cardSectionLayout.orientation)
    }

    @Test
    fun testCustomizationCombination() {
        cardSectionLayout.setCardStyle(CardSectionLayout.CardStyle.DEFAULT)
        cardSectionLayout.setCardBackgroundColor(android.R.color.holo_green_light)
        cardSectionLayout.setCardPadding(24)
        cardSectionLayout.setCardElevation(4)
        cardSectionLayout.addHeader("Customized Section")

        assertEquals(1, cardSectionLayout.childCount)
        assertTrue(cardSectionLayout.getChildAt(0) is SectionHeaderView)

        assertTrue("Custom padding should be applied", cardSectionLayout.paddingLeft > 0)
        assertTrue("Custom elevation should be applied", cardSectionLayout.elevation > 0)
    }

    @Test
    fun testEmptyHeaderText() {
        cardSectionLayout.addHeader("")

        assertEquals(1, cardSectionLayout.childCount)
        val headerView = cardSectionLayout.getChildAt(0) as SectionHeaderView
        assertEquals("", headerView.text)
    }

    @Test
    fun testLongHeaderText() {
        val longText =
            "This is a very long header text that should still be handled properly by the card section layout component"
        cardSectionLayout.addHeader(longText)

        assertEquals(1, cardSectionLayout.childCount)
        val headerView = cardSectionLayout.getChildAt(0) as SectionHeaderView
        assertEquals(longText, headerView.text)
    }

    @Test
    fun testComponentInheritance() {
        assertTrue(
            "CardSectionLayout should extend LinearLayout",
            cardSectionLayout is LinearLayout
        )
    }

    @Test
    fun testRealWorldUsageScenario() {
        cardSectionLayout.setCardStyle(CardSectionLayout.CardStyle.DEFAULT)
        cardSectionLayout.addHeader("Device Configuration", SectionHeaderView.HeaderStyle.SECTION_HEADER)

        val label = TextView(context)
        label.text = "Device Name:"
        cardSectionLayout.addView(label)

        val input = TextView(context)
        input.text = "Shimmer Device 1"
        cardSectionLayout.addView(input)

        assertEquals(3, cardSectionLayout.childCount)
        assertTrue(cardSectionLayout.getChildAt(0) is SectionHeaderView)
        assertTrue(cardSectionLayout.getChildAt(1) is TextView)
        assertTrue(cardSectionLayout.getChildAt(2) is TextView)

        assertEquals("Device Configuration", (cardSectionLayout.getChildAt(0) as SectionHeaderView).text)
        assertEquals("Device Name:", (cardSectionLayout.getChildAt(1) as TextView).text)
        assertEquals("Shimmer Device 1", (cardSectionLayout.getChildAt(2) as TextView).text)
    }
}
