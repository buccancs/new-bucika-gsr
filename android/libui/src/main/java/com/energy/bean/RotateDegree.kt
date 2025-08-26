package com.energy.bean

enum class RotateDegree(val value: Int) {
    
    DEGREE_0(0),

    DEGREE_90(1),

    DEGREE_180(2),

    DEGREE_270(3);

    companion object {
        fun valueOf(value: Int): RotateDegree {
            return values().find { it.value == value } ?: DEGREE_0
        }
    }
}
