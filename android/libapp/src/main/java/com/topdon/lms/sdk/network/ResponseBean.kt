package com.topdon.lms.sdk.network

/**
 * Stub implementation of ResponseBean for compilation
 */
data class ResponseBean<T>(
    var code: Int = 0,
    var message: String = "",
    val data: T? = null,
    val success: Boolean = code == 200
) {
    companion object {
        const val SUCCESS = 200
        const val ERROR = -1
        
        fun convertCommonBean(response: String?, clazz: Class<*>?): com.topdon.lms.sdk.bean.CommonBean {
            // Stub implementation
            val bean = com.topdon.lms.sdk.bean.CommonBean()
            bean.code = 200
            bean.data = response ?: "{}"
            return bean
        }
    }
}