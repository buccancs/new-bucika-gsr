package com.topdon.tc001.bean

data class WatermarkBean(
    val text: String,
    val visible: Boolean,
    val position: String
)

data class AlarmBean(
    val enabled: Boolean,
    val threshold: Float,
    val message: String
)