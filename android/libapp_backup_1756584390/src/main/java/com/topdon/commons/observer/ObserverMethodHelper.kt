package com.topdon.commons.observer

import com.topdon.commons.poster.MethodInfo
import com.topdon.commons.poster.Tag
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

internal class ObserverMethodHelper(private val isObserveAnnotationRequired: Boolean) {
    
    companion object {
        private val METHOD_CACHE = ConcurrentHashMap<Class<*>, Map<String, Method>>()
    }
    
    fun clearCache() {
        METHOD_CACHE.clear()
    }
    
    fun generateRunnable(observer: Observer, method: Method, info: MethodInfo): Runnable {
        val parameters = info.parameters
        return if (parameters.isNullOrEmpty()) {
            Runnable {
                try {
                    method.invoke(observer)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
            }
        } else {
            val params = arrayOfNulls<Any>(parameters.size)
            parameters.forEachIndexed { i, parameter ->
                params[i] = parameter.value
            }
            Runnable {
                try {
                    method.invoke(observer, *params)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun generateKey(tag: String, name: String, paramTypes: Array<Class<*>>): String {
        return buildString {
            if (tag.isEmpty()) {
                append(name)
            } else {
                append(tag)
            }
            paramTypes.forEach { type ->
                append(",").append(type)
            }
        }
    }
    
    fun findObserverMethod(observer: Observer): Map<String, Method> {
        METHOD_CACHE[observer.javaClass]?.let { return it }
        
        val map = mutableMapOf<String, Method>()
        val methods = mutableListOf<Method>()
        
        var cls: Class<*>? = observer.javaClass
        while (cls != null && !cls.isInterface && Observer::class.java.isAssignableFrom(cls)) {
            try {
                cls.declaredMethods?.let { ms ->
                    ms.forEach { m ->
                        val ignore = Modifier.ABSTRACT or Modifier.STATIC or 0x40 or 0x1000
                        if ((m.modifiers and Modifier.PUBLIC) != 0 && 
                            (m.modifiers and ignore) == 0 && 
                            !methods.contains(m)) {
                            methods.add(m)
                        }
                    }
                }
            } catch (ignore: Throwable) {
                // Ignore reflection errors
            }
            cls = cls.superclass
        }
        
        methods.forEach { method ->
            val anno = method.getAnnotation(Observe::class.java)
            if (anno != null || !isObserveAnnotationRequired) {
                val tagAnno = method.getAnnotation(Tag::class.java)
                val tag = tagAnno?.value ?: ""
                val key = generateKey(tag, method.name, method.parameterTypes)
                map[key] = method
            }
        }
        
        if (map.isNotEmpty()) {
            METHOD_CACHE[observer.javaClass] = map
        }
        return map
    }
    
    private fun List<Method>.contains(method: Method): Boolean {
        return any { m ->
            m.name == method.name && 
            m.returnType == method.returnType &&
            equalParamTypes(m.parameterTypes, method.parameterTypes)
        }
    }
    
    private fun equalParamTypes(params1: Array<Class<*>>, params2: Array<Class<*>>): Boolean {
        if (params1.size != params2.size) return false
        return params1.indices.all { i -> params1[i] == params2[i] }
    }
}