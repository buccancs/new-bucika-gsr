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
    fun getSystemLocal(): java.util.Locale = java.util.Locale.getDefault()
    
    @JvmStatic
    fun getSystemLocalString(): String = "en"
    
    @JvmStatic
    fun getLanguageId(context: android.content.Context): String = "1"
    
    @JvmStatic
    fun format(template: String, vararg args: Any): String = 
        String.format(template, *args)
        
    @JvmStatic
    fun convertCommonBean(response: Any?): Any? {
        // Stub conversion method
        return response
    }
}