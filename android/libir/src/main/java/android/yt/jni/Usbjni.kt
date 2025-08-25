package android.yt.jni

import android.util.Log

/**
 * usb3803_hub是系统中的so库，部分定制的机型有可能会添加应用包名的白名单，也会导致不出图
 */
object Usbjni {

    init {
        try {
            System.loadLibrary("usb3803_hub")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("Usbjni", "Couldn't load lib: - ${e.message}")
        }
    }

    external fun usb3803_mode_setting(i: Int): Int
    
    external fun usb3803_read_parameter(i: Int): Int
}