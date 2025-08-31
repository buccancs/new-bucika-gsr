package com.topdon.commons.poster

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ExecutorService

class PosterDispatcher(
    @NonNull private val executorService: ExecutorService,
    @NonNull private val defaultMode: ThreadMode
) {
    private val backgroundPoster: Poster = BackgroundPoster(executorService)
    private val mainThreadPoster: Poster = MainThreadPoster()
    private val asyncPoster: Poster = AsyncPoster(executorService)

    fun getDefaultMode(): ThreadMode = defaultMode

    fun getExecutorService(): ExecutorService = executorService

    fun clearTasks() {
        backgroundPoster.clear()
        mainThreadPoster.clear()
        asyncPoster.clear()
    }

    fun post(@Nullable method: Method?, @NonNull runnable: Runnable) {
        method?.let {
            val annotation = it.getAnnotation(RunOn::class.java)
            val mode = annotation?.value ?: defaultMode
            post(mode, runnable)
        }
    }

    fun post(@NonNull mode: ThreadMode, @NonNull runnable: Runnable) {
        val actualMode = if (mode == ThreadMode.UNSPECIFIED) defaultMode else mode
        when (actualMode) {
            ThreadMode.MAIN -> mainThreadPoster.enqueue(runnable)
            ThreadMode.POSTING -> runnable.run()
            ThreadMode.BACKGROUND -> backgroundPoster.enqueue(runnable)
            ThreadMode.ASYNC -> asyncPoster.enqueue(runnable)
            ThreadMode.UNSPECIFIED -> defaultMode.let { post(it, runnable) }
        }
    }

    fun post(
        @NonNull owner: Any,
        @NonNull methodName: String,
        @NonNull tag: String,
        @Nullable vararg parameters: MethodInfo.Parameter?
    ) {
        val params = parameters.filterNotNull()
        val classes = params.map { it.type }.toTypedArray()
        val values = params.map { it.value }.toTypedArray()

        val methods = owner.javaClass.declaredMethods
        var taggedMethod: Method? = null
        var namedMethod: Method? = null

        for (method in methods) {
            val annotation = method.getAnnotation(Tag::class.java)
            if (annotation != null && annotation.value.isNotEmpty() && 
                annotation.value == tag && equalParamTypes(method.parameterTypes, classes)) {
                taggedMethod = method
                break
            }
            if (taggedMethod == null && method.name == methodName && 
                equalParamTypes(method.parameterTypes, classes)) {
                namedMethod = method
            }
        }

        val targetMethod = taggedMethod ?: namedMethod ?: return

        try {
            post(targetMethod) {
                try {
                    targetMethod.invoke(owner, *values)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
            }
        } catch (ignore: Exception) {
        }
    }

    private fun equalParamTypes(params1: Array<Class<*>>, params2: Array<Class<*>>): Boolean {
        if (params1.size != params2.size) return false
        return params1.indices.all { params1[it] == params2[it] }
    }

    fun post(
        @NonNull owner: Any,
        @NonNull methodName: String,
        @Nullable vararg parameters: MethodInfo.Parameter?
    ) {
        post(owner, methodName, "", *parameters)
    }

    fun post(@NonNull owner: Any, @NonNull methodInfo: MethodInfo) {
        val parameters = methodInfo.parameters
        if (parameters != null) {
            post(owner, methodInfo.name, methodInfo.tag, *parameters)
        } else {
            post(owner, methodInfo.name, methodInfo.tag)
        }
    }
}