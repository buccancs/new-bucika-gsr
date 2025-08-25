package com.energy.commoncomponent.utils

import java.lang.reflect.InvocationTargetException

/**
 * Created by fengjibo on 2024/4/24.
 */
object AppUtils {
    /**
     * 通过反射类执行静态方法
     * @param className
     * @param methodName
     */
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
