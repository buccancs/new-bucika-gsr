package com.topdon.tc004.bean

data class MonocularBean(
    val res: Int,
    val name: String,
    val code: Int,
    var isSelect : Boolean = false,
)

class MenuBean {
    companion object {
        const val TYPE_WHITE_HOT = 1
        const val TYPE_BLACK_HOT = 2
        const val TYPE_RED_HOT = 12
        const val TYPE_BIRD= 16
        const val TYPE_MIX = 5

        const val TYPE_GAIN_X1 = 1
        const val TYPE_GAIN_X2 = 2
        const val TYPE_GAIN_X4 = 4
        const val TYPE_GAIN_X8 = 8

        const val TYPE_LIGHT_HIGH = 100
        const val TYPE_LIGHT_MIDDLE = 80
        const val TYPE_LIGHT_LOW = 60
    }
}
