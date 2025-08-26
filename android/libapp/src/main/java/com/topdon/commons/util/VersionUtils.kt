package com.topdon.commons.util

import android.text.TextUtils

object VersionUtils {
    
    @JvmStatic
    fun compareVersions(v1: String, v2: String): Boolean {
        if (TextUtils.equals(v1, "") || TextUtils.equals(v2, "")) {
            return false
        }
        
        val str1 = v1.split(".")
        val str2 = v2.split(".")
        
        return when {
            str1.size == str2.size -> {
                for (i in str1.indices) {
                    val num1 = str1[i].toInt()
                    val num2 = str2[i].toInt()
                    when {
                        num1 > num2 -> return true
                        num1 < num2 -> return false
                    }
                }
                false
            }
            str1.size > str2.size -> {
                for (i in str2.indices) {
                    val num1 = str1[i].toInt()
                    val num2 = str2[i].toInt()
                    when {
                        num1 > num2 -> return true
                        num1 < num2 -> return false
                        num1 == num2 -> {
                            if (str2.size == 1) continue
                            if (i == str2.size - 1) {
                                for (j in i until str1.size) {
                                    if (str1[j].toInt() != 0) {
                                        return true
                                    }
                                    if (j == str1.size - 1) {
                                        return false
                                    }
                                }
                                return true
                            }
                        }
                    }
                }
                false
            }
            else -> {
                for (i in str1.indices) {
                    val num1 = str1[i].toInt()
                    val num2 = str2[i].toInt()
                    when {
                        num1 > num2 -> return true
                        num1 < num2 -> return false
                        num1 == num2 -> {
                            if (str1.size == 1) continue
                            if (i == str1.size - 1) {
                                return false
                            }
                        }
                    }
                }
                false
            }
        }
    }
}