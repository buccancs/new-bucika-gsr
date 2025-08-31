package com.guide.zm04c.matrix

object ResultCode {

    val TAG = "mobilelibrary"

    //设备初始状态
    val READY_CONNECT_DEVICE = 1

    //找到匹配设备
    val SUCC_FIND_MATCHED_DEVICE = 2

    //找到接口端口
    val SUCC_FIND_DEVICE_INTERFACE = 3

    //设备连接接口成功
    val SUCC_CONNECT_INTERFACE = 4

    //设备连接成功
    val SUCC_FIND_ENDPOINT = 5

    //USB端口命令发送成功
    val SUCC_USB_SEND_CMD = 6


    // 找到USB设备，型号不匹配
    val ERROR_FIND_DEVICE_NOT_MATCH = -100

    //未发现任何设备
    val ERROR_NOT_FIND_DEVICE = -101

    //未找到设备端口
    val ERROR_NOT_FIND_INTERFACE = -102

    //打开设备失败
    val ERROR_OPEN_DEVICE_FAILD = -103

    //连接设备失败
    val ERROR_CONNECT_DEVICE_FAILD = -104

    //未找到设备输入输出端口号
    val ERROR_FIND_ENDPOINT_FAILD = -105

    //用户不同意开启USB权限
    val ERROR_USE_NOT_AGRREN_PERMISSIONS = -106

    //usbisvalid
    val ERROR_USE_USB_ISVALID = -107

    //USB端口命令发送失败
    val ERROE_USB_SEND_CMD_FAILD = -108
}