package com.topdon.ble

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.topdon.ble.callback.RequestCallback
import java.util.*

internal class GenericRequest(builder: RequestBuilder) : Request, Comparable<GenericRequest> {
    var device: Device? = null
    private val tag: String? = builder.tag
    val type: RequestType = builder.type
    val service: UUID? = builder.service
    val characteristic: UUID? = builder.characteristic
    val descriptor: UUID? = builder.descriptor
    val value: Any? = builder.value
    val priority: Int = builder.priority
    val callback: RequestCallback? = builder.callback
    val writeOptions: WriteOptions? = builder.writeOptions
    var descriptorTemp: ByteArray? = null
    
    var remainQueue: Queue<ByteArray>? = null
    var sendingBytes: ByteArray? = null
    
    override fun compareTo(other: GenericRequest): Int {
        return other.priority.compareTo(priority)
    }
    
    @NonNull
    override fun getDevice(): Device {
        return device!!
    }
    
    @NonNull
    override fun getType(): RequestType {
        return type
    }
    
    @Nullable
    override fun getTag(): String? {
        return tag
    }
    
    @Nullable
    override fun getService(): UUID? {
        return service
    }
    
    @Nullable
    override fun getCharacteristic(): UUID? {
        return characteristic
    }
    
    @Nullable
    override fun getDescriptor(): UUID? {
        return descriptor
    }
    
    override fun execute(connection: Connection?) {
        connection?.execute(this)
    }
}