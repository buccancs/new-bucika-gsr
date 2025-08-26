package com.topdon.thermal.bean

data class SelectIndexBean(
    var maxIndex: MutableList<Int> = mutableListOf(),
    var minIndex: MutableList<Int> = mutableListOf()
)