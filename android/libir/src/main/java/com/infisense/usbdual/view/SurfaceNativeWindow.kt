package com.infisense.usbdual.view

class SurfaceNativeWindow {

    init {
        System.loadLibrary("native-window")
    }

    external fun onCreateSurface(surface: Any, width: Int, height: Int)

    external fun onDrawFrame(argbData: ByteArray, width: Int, height: Int)

    external fun onReleaseSurface()

    external fun drawBitmap(surface: Any, bitmap: Any)
}