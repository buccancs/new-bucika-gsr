package com.topdon.ble

import com.topdon.ble.callback.RequestCallback
import java.util.UUID

open class RequestBuilder<T : RequestCallback>(val type: RequestType) {
    var tag: String? = null
    var service: UUID? = null
    var characteristic: UUID? = null
    var descriptor: UUID? = null
    var value: Any? = null
    var priority: Int = 0
    var callback: RequestCallback? = null
    var writeOptions: WriteOptions? = null

    open fun setTag(tag: String) = apply {
        this.tag = tag
    }

    open fun setPriority(priority: Int) = apply {
        this.priority = priority
    }

    open fun setCallback(callback: T) = apply {
        this.callback = callback
    }

    fun build(): Request = GenericRequest(this)
}