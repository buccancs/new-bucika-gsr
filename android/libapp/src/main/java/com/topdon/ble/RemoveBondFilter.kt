package com.topdon.ble

import android.bluetooth.BluetoothDevice

/**
 * 清空已配对设备时的过滤器
 * 
 * date: 2021/8/12 21:11
 * author: bichuanfeng
 */
fun interface RemoveBondFilter {
    fun accept(device: BluetoothDevice): Boolean
