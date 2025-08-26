package com.topdon.commons.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class PreUtil private constructor(context: Context, shareName: String = SHARE_NAME) {
    private val mContext: WeakReference<Context> = WeakReference(context)
    private val preferences: SharedPreferences = context.getSharedPreferences(shareName, Context.MODE_PRIVATE)
    
    private val DATA_URL = "/data/data/"
    private val SHARED_PREFS = "/shared_prefs"

    companion object {
        private const val SHARE_NAME = "ad900_data"
        
        @Volatile
        private var instance: PreUtil? = null

        @JvmStatic
        fun getInstance(context: Context): PreUtil = getInstance(context, SHARE_NAME)

        @JvmStatic
        fun getInstance(context: Context, shareName: String): PreUtil {
            return instance ?: synchronized(this) {
                instance ?: PreUtil(context, shareName).also { instance = it }
            }
        }
    }

    fun put(key: String, value: Boolean) {
        val edit = preferences.edit()
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        edit.putBoolean(normalizedKey, value)
        edit.commit()
    }

    fun put(key: String, value: String) {
        val edit = preferences.edit()
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        edit.putString(normalizedKey, value)
        edit.commit()
    }

    fun put(key: String, value: Int) {
        val edit = preferences.edit()
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        edit.putInt(normalizedKey, value)
        edit.commit()
    }

    fun put(key: String, value: Float) {
        val edit = preferences.edit()
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        edit.putFloat(normalizedKey, value)
        edit.commit()
    }

    fun put(key: String, value: Long) {
        val edit = preferences.edit()
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        edit.putLong(normalizedKey, value)
        edit.commit()
    }

    fun put(key: String, value: Set<String>) {
        val edit = preferences.edit()
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        edit.putStringSet(normalizedKey, value)
        edit.commit()
    }

    fun <T> put(t: T) {
        try {
            val edit = preferences.edit()
            val cls = t!!::class.java
            
            val methods = cls.declaredMethods
            for (method in methods) {
                val methodName = method.name
                if (methodName.startsWith("get")) {
                    val value = method.invoke(t)
                    val saveValue = if (!TextUtils.isEmpty(value.toString())) value.toString() else ""
                    val saveKey = methodName.replace("get", "").lowercase()
                    edit.putString(saveKey, saveValue)
                }
            }
            edit.commit()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }

    fun get(key: String): String {
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        return preferences.getString(normalizedKey, "") ?: ""
    }

    fun get(key: String, defValue: String): String {
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        return preferences.getString(normalizedKey, defValue) ?: defValue
    }

    fun get(key: String, defValue: Boolean): Boolean {
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        return preferences.getBoolean(normalizedKey, defValue)
    }

    fun get(key: String, defValue: Int): Int {
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        return preferences.getInt(normalizedKey, defValue)
    }

    fun get(key: String, defValue: Float): Float {
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        return preferences.getFloat(normalizedKey, defValue)
    }

    fun get(key: String, defValue: Long): Long {
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        return preferences.getLong(normalizedKey, defValue)
    }

    @SuppressLint("NewApi")
    fun get(key: String, defValue: Set<String>?): Set<String>? {
        val normalizedKey = if (!TextUtils.isEmpty(key)) key.lowercase() else key
        return preferences.getStringSet(normalizedKey, defValue)
    }

    @SuppressLint("CommitPrefEdits")
    fun put(key: String, defaultObj: Any) {
        val editor = preferences.edit()
        when (defaultObj) {
            is String -> editor.putString(key, defaultObj)
            is Int -> editor.putInt(key, defaultObj)
            is Boolean -> editor.putBoolean(key, defaultObj)
            is Float -> editor.putFloat(key, defaultObj)
            is Long -> editor.putLong(key, defaultObj)
        }
        editor.commit()
    }

    fun get(key: String, defaultObj: Any): Any? {
        return when (defaultObj) {
            is String -> preferences.getString(key, defaultObj)
            is Int -> preferences.getInt(key, defaultObj)
            is Boolean -> preferences.getBoolean(key, defaultObj)
            is Float -> preferences.getFloat(key, defaultObj)
            is Long -> preferences.getLong(key, defaultObj)
            else -> null
        }
    }

    fun <T> get(cls: Class<T>): Any? {
        return try {
            val obj = cls.newInstance()
            val fields = cls.declaredFields
            
            for (field in fields) {
                val fieldName = field.name
                if ("serialVersionUID" != fieldName) {
                    field.isAccessible = true
                    field.set(obj, get(field.name))
                }
            }
            obj
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearAll() {
        try {
            val fileName = "$SHARE_NAME.xml"
            val path = StringBuilder(DATA_URL)
                .append(mContext.get()?.packageName)
                .append(SHARED_PREFS)
            
            val file = File(path.toString(), fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}