package com.topdon.ble

import android.bluetooth.BluetoothDevice

fun interface RemoveBondFilter {
    fun accept(device: BluetoothDevice): Boolean
