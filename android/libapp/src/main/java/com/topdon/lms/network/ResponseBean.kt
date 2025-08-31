package com.topdon.lms.network

/**
 * Stub implementation of ResponseBean for compilation
 */
data class ResponseBean<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null,
    val success: Boolean = code == 200
) {
    companion object {
        const val SUCCESS = 200
        const val ERROR = -1
    }
}