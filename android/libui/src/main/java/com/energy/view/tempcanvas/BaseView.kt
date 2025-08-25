package com.energy.view.tempcanvas

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Created by fengjibo on 2023/6/28.
 */
abstract class BaseView {
    protected var mId: String = ""
    protected var mPointSize = 0
    
    var label: String = "" // 标记内容
    var note: String = "" // 备注
    var maxTemp: Double = 0.0 // 最大温度
    var minTemp: Double = 0.0 // 最小温度
    var avgTemp: Double = 0.0 // 平均温度

    fun getId(): String = mId
    fun setId(id: String) {
        this.mId = id
    }

    protected fun getCustomSizeImg(rootImg: Bitmap, goalW: Int, goalH: Int): Bitmap {
        val rootW = rootImg.width
        val rootH = rootImg.height
        val matrix = Matrix().apply {
            postScale(goalW * 1.0f / rootW, goalH * 1.0f / rootH)
        }
        return Bitmap.createBitmap(rootImg, 0, 0, rootW, rootH, matrix, true)
    }
}