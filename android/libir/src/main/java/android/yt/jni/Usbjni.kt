package android.yt.jni

import android.util.Log

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