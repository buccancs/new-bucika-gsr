package com.infisense.usbir.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

/**
 * Created by fengjibo on 2023/4/12.
 */
object SharedPreferencesUtil {

    private const val FILE_NAME = "usb_ir"

    /**
     * 保存数据到文件
     *
     * @param context
     * @param key
     * @param data
     */
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

    /**
     * 从文件里读取数据
     *
     * @param context
     * @param key
     * @param defValue
     * @return
     */
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

    /**
     *
     * @param context
     * @param key
     * @param data
     */
    fun saveByteData(context: Context, key: String, data: ByteArray) {
        val sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val imageString = String(Base64.encode(data, Base64.DEFAULT))
        editor.putString(key, imageString)

        editor.commit()
    }

    /**
     *
     * @param context
     * @param key
     * @return
     */
    fun getByteData(context: Context, key: String): ByteArray {
        val sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

        val string = sharedPreferences.getString(key, "") ?: ""
        return Base64.decode(string.toByteArray(), Base64.DEFAULT)
    }
