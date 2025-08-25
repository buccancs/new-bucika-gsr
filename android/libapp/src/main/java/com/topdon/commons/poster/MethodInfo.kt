package com.topdon.commons.poster

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.lang.reflect.Method

class MethodInfo {
    @NonNull
    var name: String
        private set
    
    @Nullable
    var parameters: Array<Parameter>?
        private set
    
    @NonNull
    var tag: String
        private set
    
    constructor(@NonNull name: String, @Nullable vararg parameters: Parameter) : this(name, name, *parameters)
    
    constructor(@NonNull name: String, @NonNull tag: String, @Nullable vararg parameters: Parameter) {
        this.name = name
        this.tag = tag
        this.parameters = if (parameters.isNotEmpty()) parameters else null
    }
    
    constructor(@NonNull name: String, @Nullable parameterTypes: Array<Class<*>>?) : this(name, name, parameterTypes)
    
    constructor(@NonNull name: String, @NonNull tag: String, @Nullable parameterTypes: Array<Class<*>>?) : this(name, tag, *toParameters(parameterTypes))
    
    companion object {
        @JvmStatic
        fun valueOf(@NonNull method: Method): MethodInfo {
            val annotation = method.getAnnotation(Tag::class.java)
            return MethodInfo(
                method.name,
                annotation?.value ?: method.name,
                method.parameterTypes
            )
        }
        
        private fun toParameters(parameterTypes: Array<Class<*>>?): Array<Parameter> {
            return parameterTypes?.map { Parameter(it, null) }?.toTypedArray() ?: emptyArray()
        }
    }
    
    fun setName(@NonNull name: String) {
        this.name = name
    }
    
    fun setTag(@NonNull tag: String) {
        this.tag = tag
    }
    
    fun setParameters(@Nullable parameters: Array<Parameter>?) {
        this.parameters = parameters
    }
    
    @Nullable
    fun getParameterTypes(): Array<Class<*>>? {
        return parameters?.map { it.type }?.toTypedArray()
    }
    
    @Nullable
    fun getParameterValues(): Array<Any?>? {
        return parameters?.map { it.value }?.toTypedArray()
    }
    
    class Parameter {
        @Nullable
        var value: Any?
            private set
        
        @NonNull
        var type: Class<*>
            private set
        
        constructor(@NonNull type: Class<*>, @Nullable value: Any?) {
            this.type = type
            this.value = value
        }
        
        fun setValue(@Nullable value: Any?) {
            this.value = value
        }
        
        fun setType(@NonNull type: Class<*>) {
            this.type = type
        }
    }
}