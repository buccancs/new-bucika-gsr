package android.yt.jni

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

object Usbcontorl {
    @JvmStatic
    var isload = false
    
    init {
        val file = File("/proc/self/maps")
        if (file.exists() && file.isFile) {
            try {
                BufferedReader(FileReader(file)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        if (line.contains("libusb3803_hub.so")) {
                            isload = true
                            return@forEach
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
