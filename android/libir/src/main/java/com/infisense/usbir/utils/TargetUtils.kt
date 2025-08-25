package com.infisense.usbir.utils

import com.infisense.usbir.R
import com.topdon.lib.core.bean.ObserveBean

object TargetUtils {
    
    fun getSelectTargetDraw(targetMeasureMode: Int, targetType: Int, targetColorType: Int): Int {
        return when (targetColorType) {
            ObserveBean.TYPE_TARGET_COLOR_GREEN -> getGreenTargetDraw(targetMeasureMode, targetType)
            ObserveBean.TYPE_TARGET_COLOR_RED -> getRedTargetDraw(targetMeasureMode, targetType)
            ObserveBean.TYPE_TARGET_COLOR_BLUE -> getBlueTargetDraw(targetMeasureMode, targetType)
            ObserveBean.TYPE_TARGET_COLOR_BLACK -> getBlackTargetDraw(targetMeasureMode, targetType)
            ObserveBean.TYPE_TARGET_COLOR_WHITE -> getWhiteTargetDraw(targetMeasureMode, targetType)
            else -> R.drawable.svg_ic_target_horizontal_person_green
        }
    }
    
    private fun getGreenTargetDraw(targetMeasureMode: Int, targetType: Int): Int {
        return when (targetMeasureMode) {
            ObserveBean.TYPE_MEASURE_PERSON -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.svg_ic_target_horizontal_person_green
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_person_green
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_person_green
                    else -> R.drawable.svg_ic_target_horizontal_person_green
                }
            }
            ObserveBean.TYPE_MEASURE_SHEEP -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_sheep_green
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_sheep_green
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_sheep_green
                    else -> R.drawable.ic_target_horizontal_sheep_green
                }
            }
            ObserveBean.TYPE_MEASURE_DOG, ObserveBean.TYPE_MEASURE_BIRD -> {

                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_sheep_green
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_sheep_green
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_sheep_green
                    else -> R.drawable.ic_target_horizontal_sheep_green
                }
            }
            else -> R.drawable.svg_ic_target_horizontal_person_green
        }
    }
    
    private fun getRedTargetDraw(targetMeasureMode: Int, targetType: Int): Int {
        return when (targetMeasureMode) {
            ObserveBean.TYPE_MEASURE_PERSON -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_person_red
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_person_red
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_person_red
                    else -> R.drawable.ic_target_horizontal_person_red
                }
            }
            ObserveBean.TYPE_MEASURE_SHEEP -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_sheep_red
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_sheep_red
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_sheep_red
                    else -> R.drawable.ic_target_horizontal_sheep_red
                }
            }
            ObserveBean.TYPE_MEASURE_DOG, ObserveBean.TYPE_MEASURE_BIRD -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_sheep_red
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_sheep_red
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_sheep_red
                    else -> R.drawable.ic_target_horizontal_sheep_red
                }
            }
            else -> R.drawable.ic_target_horizontal_person_red
        }
    }
    
    private fun getBlueTargetDraw(targetMeasureMode: Int, targetType: Int): Int {
        return when (targetMeasureMode) {
            ObserveBean.TYPE_MEASURE_PERSON -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_person_blue
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_person_blue
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_person_blue
                    else -> R.drawable.ic_target_horizontal_person_blue
                }
            }
            ObserveBean.TYPE_MEASURE_SHEEP -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_sheep_blue
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_sheep_blue
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_sheep_blue
                    else -> R.drawable.ic_target_horizontal_sheep_blue
                }
            }
            ObserveBean.TYPE_MEASURE_DOG, ObserveBean.TYPE_MEASURE_BIRD -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_sheep_blue
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_sheep_blue
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_sheep_blue
                    else -> R.drawable.ic_target_horizontal_sheep_blue
                }
            }
            else -> R.drawable.ic_target_horizontal_person_blue
        }
    }
    
    private fun getBlackTargetDraw(targetMeasureMode: Int, targetType: Int): Int {
        return when (targetMeasureMode) {
            ObserveBean.TYPE_MEASURE_PERSON -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_person_black
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_person_black
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_person_black
                    else -> R.drawable.ic_target_horizontal_person_black
                }
            }
            ObserveBean.TYPE_MEASURE_SHEEP -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_sheep_black
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_sheep_black
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_sheep_black
                    else -> R.drawable.ic_target_horizontal_sheep_black
                }
            }
            ObserveBean.TYPE_MEASURE_DOG, ObserveBean.TYPE_MEASURE_BIRD -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_sheep_black
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_sheep_black
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_sheep_black
                    else -> R.drawable.ic_target_horizontal_sheep_black
                }
            }
            else -> R.drawable.ic_target_horizontal_person_black
        }
    }
    
    private fun getWhiteTargetDraw(targetMeasureMode: Int, targetType: Int): Int {
        return when (targetMeasureMode) {
            ObserveBean.TYPE_MEASURE_PERSON -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_person_white
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_person_white
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_person_white
                    else -> R.drawable.ic_target_horizontal_person_white
                }
            }
            ObserveBean.TYPE_MEASURE_SHEEP -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_sheep_white
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_sheep_white
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_sheep_white
                    else -> R.drawable.ic_target_horizontal_sheep_white
                }
            }
            ObserveBean.TYPE_MEASURE_DOG, ObserveBean.TYPE_MEASURE_BIRD -> {
                when (targetType) {
                    ObserveBean.TYPE_TARGET_HORIZONTAL -> R.drawable.ic_target_horizontal_sheep_white
                    ObserveBean.TYPE_TARGET_VERTICAL -> R.drawable.ic_target_vertical_sheep_white
                    ObserveBean.TYPE_TARGET_CIRCLE -> R.drawable.ic_target_circle_sheep_white
                    else -> R.drawable.ic_target_horizontal_sheep_white
                }
            }
            else -> R.drawable.ic_target_horizontal_person_white
        }
    }

    fun getMeasureSize(targetMeasureMode: Int): Float {
        return when (targetMeasureMode) {
            ObserveBean.TYPE_MEASURE_PERSON -> 180f
            ObserveBean.TYPE_MEASURE_SHEEP -> 100f
            ObserveBean.TYPE_MEASURE_DOG -> 50f
            ObserveBean.TYPE_MEASURE_BIRD -> 20f
            else -> 180f
        }
    }

    fun isScaleMode(targetMeasureMode: Int): Boolean {
        return targetMeasureMode == ObserveBean.TYPE_MEASURE_DOG ||
                targetMeasureMode == ObserveBean.TYPE_MEASURE_BIRD
    }
