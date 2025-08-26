package com.topdon.commons.util

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

object UTF8StringUtils {

    @JvmStatic
    fun readByUtf8WithBom(path: String): String {
        val file = File(path)
        try {
            if (file.exists() && file.isFile) {
                file.inputStream().use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        BufferedReader(reader).use { bufferedReader ->
                            bufferedReader.lineSequence().forEach { txt ->
                                val trimmed = txt.trim()
                                val flag = trimmed.substring(trimmed.lastIndexOf("|") + 1)
                                return if (flag == "1") {
                                    trimmed.substring(0, trimmed.lastIndexOf("|"))
                                } else {
                                    trimmed
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }

    @JvmStatic
    fun readByUtf8WithOutBom(path: String): String {
        val file = File(path)
        try {
            if (file.exists() && file.isFile) {
                file.inputStream().use { inputStream ->
                    BufferedReader(UnicodeReader(inputStream, "utf-8")).use { bufferedReader ->
                        bufferedReader.lineSequence().forEach { txt ->
                            val trimmed = txt.trim()
                            val flag = trimmed.substring(trimmed.lastIndexOf("|") + 1)
                            return if (flag == "1") {
                                trimmed.substring(0, trimmed.lastIndexOf("|"))
                            } else {
                                trimmed
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }
}