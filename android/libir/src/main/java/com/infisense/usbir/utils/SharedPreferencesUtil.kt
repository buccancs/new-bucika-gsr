package com.infisense.usbir.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

object SharedPreferencesUtil {

    private const val FILE_NAME = "usb_ir"

    fun saveData(context: Context, key: String, data: Any) {
        val sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        when (data) {
            is Int -> editor.putInt(key, data)
            is Boolean -> editor.putBoolean(key, data)
            is String -> editor.putString(key, data)
            is Float -> editor.putFloat(key, data)
            is Long -> editor.putLong(key, data)
        }
        editor.commit()
    }

    fun getData(context: Context, key: String, defValue: Any): Any? {
        val sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

        return when (defValue) {
            is Int -> sharedPreferences.getInt(key, defValue)
            is Boolean -> sharedPreferences.getBoolean(key, defValue)
            is String -> sharedPreferences.getString(key, defValue)
            is Float -> sharedPreferences.getFloat(key, defValue)
            is Long -> sharedPreferences.getLong(key, defValue)
            else -> null
        }
    }

    fun saveByteData(context: Context, key: String, data: ByteArray) {
        val sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val imageString = String(Base64.encode(data, Base64.DEFAULT))
        editor.putString(key, imageString)

        editor.commit()
    }

    fun getByteData(context: Context, key: String): ByteArray {
        val sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

        val string = sharedPreferences.getString(key, "") ?: ""
        return Base64.decode(string.toByteArray(), Base64.DEFAULT)
    }
}
