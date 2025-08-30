package com.topdon.thermal.report.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImageTempBean(
    val minTemp: Float,
    val maxTemp: Float,
    val avgTemp: Float
) : Parcelable