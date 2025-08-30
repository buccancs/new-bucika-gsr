package com.topdon.lms.utils

/**
 * Stub implementation of SPUtils for compilation
 */
class SPUtils private constructor() {
    companion object {
        @Volatile
        private var instance: SPUtils? = null
        
        @JvmStatic
        fun getInstance(): SPUtils {
            return instance ?: synchronized(this) {
                instance ?: SPUtils().also { instance = it }
            }
        }
    }
    
    fun put(key: String, value: String) {}
    
    fun put(key: String, value: Int) {}
    
    fun put(key: String, value: Boolean) {}
    
    fun getString(key: String): String = ""
    
    fun getString(key: String, defaultValue: String): String = defaultValue
    
    fun getInt(key: String): Int = 0
    
    fun getInt(key: String, defaultValue: Int): Int = defaultValue
    
    fun getBoolean(key: String): Boolean = false
    
    fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
}