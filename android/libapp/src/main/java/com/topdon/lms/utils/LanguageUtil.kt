package com.topdon.lms.utils

/**
 * Stub implementation of LanguageUtil for compilation
 */
object LanguageUtil {
    @JvmStatic
    fun getCurrentLanguage(): String = "en"
    
    @JvmStatic
    fun setLanguage(language: String) {}
    
    @JvmStatic
    fun getLocalizedString(key: String): String = key
    
    @JvmStatic
    fun getSystemLocal(): String = "en"
}