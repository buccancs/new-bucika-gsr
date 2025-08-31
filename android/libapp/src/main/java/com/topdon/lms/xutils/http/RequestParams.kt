package com.topdon.lms.xutils.http

/**
 * Stub implementation of RequestParams for compilation
 */
class RequestParams {
    private val parameters = mutableMapOf<String, Any>()
    
    var uri: String = ""
    var isAsJsonContent: Boolean = false
    
    fun addBodyParameter(name: String, value: Any) {
        parameters[name] = value
    }
    
    fun addParameter(name: String, value: Any) {
        parameters[name] = value
    }
    
    fun getParameters(): Map<String, Any> = parameters
}