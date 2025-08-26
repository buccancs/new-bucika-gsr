package com.topdon.ble

fun interface BondController {
    
    fun accept(device: Device): Boolean
}