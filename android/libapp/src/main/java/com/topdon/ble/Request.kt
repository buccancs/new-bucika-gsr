package com.topdon.ble

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.util.UUID

interface Request {
    
    @get:NonNull
    val device: Device
    
    @get:NonNull
    val type: RequestType
    
    @get:Nullable
    val tag: String?
    
    @get:Nullable
    val service: UUID?
    
    @get:Nullable
    val characteristic: UUID?
    
    @get:Nullable
    val descriptor: UUID?
    
    fun execute(connection: Connection)
}
