package com.energy.utils

import java.lang.reflect.InvocationTargetException

object AppUtils {
    
    fun runMethodByReflectClass(className: String, methodName: String) {
        try {
            val clazz = Class.forName(className)
            val method = clazz.getMethod(methodName)
            method.invoke(null)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }
}
