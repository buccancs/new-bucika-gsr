package com.topdon.thermal.video.media

data class EncodingOptions(
    var compressLevel: Int = COMPRESS_LOW
) {
    companion object {
        const val COMPRESS_HIGH = 2
        const val COMPRESS_LOW = 0
        const val COMPRESS_MID = 1
    }

    override fun toString(): String {
        return "EncodingOptions : compLevel = $compressLevel"
    }
