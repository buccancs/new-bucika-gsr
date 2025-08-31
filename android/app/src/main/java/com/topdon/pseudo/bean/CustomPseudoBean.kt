package com.topdon.pseudo.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CustomPseudoBean(
    val id: String = "",
    val name: String = "",
    val colors: List<String> = emptyList(),
    var isUseCustomPseudo: Boolean = false,
    var maxTemp: Float = 0f,
    var minTemp: Float = 0f
) : Parcelable