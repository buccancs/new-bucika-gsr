package com.topdon.ble

/**
 * date: 2019/8/5 16:10
 * author: bichuanfeng
 */
internal object Inspector {
    /**
     * 对象为空时抛EasyBLEException
     *
     * @param obj     要检查的对象
     * @param message 异常概要消息
     */
    fun <T> requireNonNull(obj: T?, message: String): T {
        return obj ?: throw EasyBLEException(message)
    }
}