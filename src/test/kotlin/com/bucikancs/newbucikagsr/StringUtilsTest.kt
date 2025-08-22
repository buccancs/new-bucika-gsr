package com.bucikancs.newbucikagsr

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class StringUtilsTest {

    @Test
    fun `test isPalindrome with valid palindrome`() {
        assertTrue(StringUtils.isPalindrome("racecar"))
    }

    @Test
    fun `test isPalindrome with valid palindrome different case`() {
        assertTrue(StringUtils.isPalindrome("RaceCar"))
    }

    @Test
    fun `test isPalindrome with palindrome containing spaces`() {
        assertTrue(StringUtils.isPalindrome("race car"))
    }

    @Test
    fun `test isPalindrome with non-palindrome`() {
        assertFalse(StringUtils.isPalindrome("hello"))
    }

    @Test
    fun `test isPalindrome with empty string`() {
        assertTrue(StringUtils.isPalindrome(""))
    }

    @Test
    fun `test isPalindrome with single character`() {
        assertTrue(StringUtils.isPalindrome("a"))
    }

    @Test
    fun `test countVowels with mixed case`() {
        assertEquals(2, StringUtils.countVowels("Hello"))
    }

    @Test
    fun `test countVowels with all vowels`() {
        assertEquals(5, StringUtils.countVowels("aeiou"))
    }

    @Test
    fun `test countVowels with no vowels`() {
        assertEquals(0, StringUtils.countVowels("xyz"))
    }

    @Test
    fun `test countVowels with empty string`() {
        assertEquals(0, StringUtils.countVowels(""))
    }

    @Test
    fun `test countVowels with uppercase vowels`() {
        assertEquals(5, StringUtils.countVowels("AEIOU"))
    }

    @Test
    fun `test capitalize normal sentence`() {
        assertEquals("Hello World", StringUtils.capitalize("hello world"))
    }

    @Test
    fun `test capitalize mixed case`() {
        assertEquals("Hello World", StringUtils.capitalize("hELLo WoRLD"))
    }

    @Test
    fun `test capitalize single word`() {
        assertEquals("Hello", StringUtils.capitalize("hello"))
    }

    @Test
    fun `test capitalize empty string`() {
        assertEquals("", StringUtils.capitalize(""))
    }

    @Test
    fun `test capitalize with extra spaces`() {
        assertEquals("Hello  World", StringUtils.capitalize("hello  world"))
    }

    @Test
    fun `test capitalize with empty word between spaces`() {
        assertEquals("Hello  World", StringUtils.capitalize("hello  world"))
    }

    @Test
    fun `test reverseWords normal sentence`() {
        assertEquals("world hello", StringUtils.reverseWords("hello world"))
    }

    @Test
    fun `test reverseWords single word`() {
        assertEquals("hello", StringUtils.reverseWords("hello"))
    }

    @Test
    fun `test reverseWords empty string`() {
        assertEquals("", StringUtils.reverseWords(""))
    }

    @Test
    fun `test reverseWords with multiple words`() {
        assertEquals("there hi hello", StringUtils.reverseWords("hello hi there"))
    }

    @Test
    fun `test countWords normal sentence`() {
        assertEquals(2, StringUtils.countWords("hello world"))
    }

    @Test
    fun `test countWords single word`() {
        assertEquals(1, StringUtils.countWords("hello"))
    }

    @Test
    fun `test countWords empty string`() {
        assertEquals(0, StringUtils.countWords(""))
    }

    @Test
    fun `test countWords blank string`() {
        assertEquals(0, StringUtils.countWords("   "))
    }

    @Test
    fun `test countWords with extra spaces`() {
        assertEquals(2, StringUtils.countWords("  hello   world  "))
    }

    @Test
    fun `test cleanString with extra spaces`() {
        assertEquals("hello world", StringUtils.cleanString("  hello   world  "))
    }

    @Test
    fun `test cleanString with single space`() {
        assertEquals("hello world", StringUtils.cleanString("hello world"))
    }

    @Test
    fun `test cleanString with empty string`() {
        assertEquals("", StringUtils.cleanString(""))
    }

    @Test
    fun `test cleanString with only spaces`() {
        assertEquals("", StringUtils.cleanString("   "))
    }

    @Test
    fun `test cleanString with tabs and newlines`() {
        assertEquals("hello world", StringUtils.cleanString("hello\t\n world"))
    }

    @Test
    fun `test isNumeric with valid number`() {
        assertTrue(StringUtils.isNumeric("12345"))
    }

    @Test
    fun `test isNumeric with zero`() {
        assertTrue(StringUtils.isNumeric("0"))
    }

    @Test
    fun `test isNumeric with letters`() {
        assertFalse(StringUtils.isNumeric("123abc"))
    }

    @Test
    fun `test isNumeric with empty string`() {
        assertFalse(StringUtils.isNumeric(""))
    }

    @Test
    fun `test isNumeric with spaces`() {
        assertFalse(StringUtils.isNumeric("1 2 3"))
    }

    @Test
    fun `test isNumeric with special characters`() {
        assertFalse(StringUtils.isNumeric("123!"))
    }

    @Test
    fun `test isNumeric with decimal point`() {
        assertFalse(StringUtils.isNumeric("12.34"))
    }

    @Test
    fun `test isNumeric with negative number`() {
        assertFalse(StringUtils.isNumeric("-123"))
    }
}