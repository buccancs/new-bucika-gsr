package com.topdon.ble

import com.topdon.ble.callback.RequestCallback
import java.util.UUID

class RequestBuilder<T : RequestCallback>(val type: RequestType) {
    var tag: String? = null
        private set
    var service: UUID? = null
        private set
    var characteristic: UUID? = null
        private set
    var descriptor: UUID? = null
        private set
    var value: Any? = null
        private set
    var priority: Int = 0
        private set
    var callback: RequestCallback? = null
        private set
    var writeOptions: WriteOptions? = null
        private set

    fun setTag(tag: String) = apply {
        this.tag = tag
    }

    fun setPriority(priority: Int) = apply {
        this.priority = priority
    }

    fun setCallback(callback: T) = apply {
        this.callback = callback
    }

    fun build(): Request = GenericRequest(this)
}