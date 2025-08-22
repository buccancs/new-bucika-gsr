package com.bucikancs.newbucikagsr

/**
 * A simple string utility class with various string operations
 */
object StringUtils {
    
    /**
     * Checks if a string is palindrome
     */
    fun isPalindrome(str: String): Boolean {
        val cleaned = str.replace(" ", "").lowercase()
        return cleaned == cleaned.reversed()
    }
    
    /**
     * Counts vowels in a string
     */
    fun countVowels(str: String): Int {
        return str.lowercase().count { it in "aeiou" }
    }
    
    /**
     * Capitalizes first letter of each word
     */
    fun capitalize(str: String): String {
        return str.split(" ").joinToString(" ") { word ->
            if (word.isNotEmpty()) {
                word.first().uppercase() + word.substring(1).lowercase()
            } else {
                word
            }
        }
    }
    
    /**
     * Reverses words in a string
     */
    fun reverseWords(str: String): String {
        return str.split(" ").reversed().joinToString(" ")
    }
    
    /**
     * Counts words in a string
     */
    fun countWords(str: String): Int {
        return if (str.isBlank()) 0 else str.trim().split(Regex("\\s+")).size
    }
    
    /**
     * Removes extra spaces and trims
     */
    fun cleanString(str: String): String {
        return str.trim().replace(Regex("\\s+"), " ")
    }
    
    /**
     * Checks if string contains only digits
     */
    fun isNumeric(str: String): Boolean {
        return str.isNotEmpty() && str.all { it.isDigit() }
    }
}