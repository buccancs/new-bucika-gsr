package com.topdon.ble

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.topdon.ble.callback.RequestCallback
import java.util.*

internal class GenericRequest(builder: RequestBuilder<*>) : Request, Comparable<GenericRequest> {
    override lateinit var device: Device
    override val tag: String? = builder.tag
    override val type: RequestType = builder.type
    override val service: UUID? = builder.service
    override val characteristic: UUID? = builder.characteristic
    override val descriptor: UUID? = builder.descriptor
    val value: Any? = builder.value
    val priority: Int = builder.priority
    val callback: RequestCallback? = builder.callback
    var writeOptions: WriteOptions? = builder.writeOptions
    var descriptorTemp: ByteArray? = null
    
    var remainQueue: Queue<ByteArray>? = null
    var sendingBytes: ByteArray? = null
    
    override fun compareTo(other: GenericRequest): Int {
        return other.priority.compareTo(priority)
    }
    
    override fun execute(connection: Connection) {
        connection.execute(this)
    }
}