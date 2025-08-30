package com.topdon.lms.sdk

import com.topdon.lms.sdk.bean.CommonBean

/**
 * Stub implementation of LMS SDK for compilation
 * This provides minimal implementation to allow compilation when LMS AAR is in app module
 */
class LMS private constructor() {
    companion object {
        @Volatile
        private var instance: LMS? = null
        
        var mContext: android.content.Context? = null
        
        // Response codes
        const val SUCCESS = 200
        const val ERROR = -1
        
        @JvmStatic
        fun getInstance(): LMS {
            return instance ?: synchronized(this) {
                instance ?: LMS().also { instance = it }
            }
        }
    }
    
    var loginName: String = ""
    var token: String = ""
    
    // Minimal stub methods for compilation
    fun init(context: android.content.Context) {
        LMS.mContext = context
    }
    
    // Property instead of function to match usage
    val isLogin: Boolean = false
    
    fun login(username: String, password: String, callback: Any?) {}
    
    fun checkAppUpdate(callback: (CallbackData) -> Unit) {
        // Stub implementation - calls back with empty data
        callback.invoke(CallbackData())
    }
    
    fun getStatement(type: String, callback: Any) {
        // Stub implementation for IRCamera compatibility
    }
    
    fun syncUserInfo() {}
    
    // Method that takes callback parameter to match usage
    fun getUserInfo(callback: ((CommonBean) -> Unit)? = null): CommonBean {
        val bean = CommonBean()
        callback?.invoke(bean)
        return bean
    }
    
    fun bindDevice(sn: String, randomNum: String, param1: String, param2: String, callback: (CallbackData) -> Unit) {
        // Stub implementation - simulates binding success
        callback.invoke(CallbackData(code = SUCCESS))
    }
    
    // Mock callback data
    data class CallbackData(
        val code: Int = 2000,
        val data: String = "{}"
    )
}