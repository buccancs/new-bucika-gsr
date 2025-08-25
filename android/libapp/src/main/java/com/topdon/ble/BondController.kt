package com.topdon.ble

/**
 * 配对控制器
 * 
 * date: 2021/8/12 12:59
 * author: bichuanfeng
 */
fun interface BondController {
    /**
     * 配对控制
     * 
     * @param device 设备
     */
    fun accept(device: Device): Boolean
}