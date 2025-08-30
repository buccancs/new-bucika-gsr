package com.topdon.lms.utils

/**
 * Stub implementation of StringUtils for compilation
 */
object StringUtils {
    @JvmStatic
    fun isEmpty(str: String?): Boolean = str.isNullOrEmpty()
    
    @JvmStatic
    fun isNotEmpty(str: String?): Boolean = !str.isNullOrEmpty()
    
    @JvmStatic
    fun equals(str1: String?, str2: String?): Boolean = str1 == str2
    
    @JvmStatic
    fun format(format: String, vararg args: Any): String = String.format(format, *args)
}