package com.infisense.usbdual.view

/*
 * @Description: 使用GPU绘制，减少CPU和内存占用，可以自定义
 * @Author:         brilliantzhao
 * @CreateDate:     2022.9.8 10:26
 * @UpdateUser:
 * @UpdateDate:     2022.9.8 10:26
 * @UpdateRemark:
 */
class SurfaceNativeWindow {

    init {
        System.loadLibrary("native-window")
    }

    external fun onCreateSurface(surface: Any, width: Int, height: Int)

    external fun onDrawFrame(argbData: ByteArray, width: Int, height: Int)

    external fun onReleaseSurface()

    external fun drawBitmap(surface: Any, bitmap: Any)
}