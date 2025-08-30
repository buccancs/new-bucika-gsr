package com.topdon.lms.sdk

/**
 * Stub implementation of LMS SDK for compilation
 * This provides minimal implementation to allow compilation when LMS AAR is in app module
 */
class LMS private constructor() {
    companion object {
        @Volatile
        private var instance: LMS? = null
        
        @JvmStatic
        fun getInstance(): LMS {
            return instance ?: synchronized(this) {
                instance ?: LMS().also { instance = it }
            }
        }
    }
    
    var loginName: String = ""
    
    // Minimal stub methods for compilation
    fun init(context: android.content.Context) {}
    
    fun isLogin(): Boolean = false
    
    fun login(username: String, password: String, callback: Any?) {}
    
    fun checkAppUpdate(callback: Any?) {}
    
    fun getStatement(request: Any?, callback: Any?) {}
}