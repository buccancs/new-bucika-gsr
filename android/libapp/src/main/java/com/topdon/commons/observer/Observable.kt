package com.topdon.commons.observer

import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.topdon.commons.poster.MethodInfo
import com.topdon.commons.poster.PosterDispatcher
import java.lang.reflect.Method
import java.util.*

class Observable(
    private val posterDispatcher: PosterDispatcher,
    isObserveAnnotationRequired: Boolean
) {
    private val observerInfos = mutableListOf<ObserverInfo>()
    private val helper = ObserverMethodHelper(isObserveAnnotationRequired)
    
    fun getPosterDispatcher(): PosterDispatcher = posterDispatcher
    
    fun registerObserver(@NonNull observer: Observer) {
        Objects.requireNonNull(observer, "observer can't be null")
        
        synchronized(observerInfos) {
            var registered = false
            
            observerInfos.removeAll { info ->
                val o = info.weakObserver.get()
                when {
                    o == null -> true // Remove if observer was garbage collected
                    o === observer -> {
                        registered = true
                        false // Don't remove, just mark as registered
                    }
                    else -> false
                }
            }
            
            if (registered) {
                Log.e("Observable", "", Error("Observer $observer is already registered."))
                return
            }
            
            val methodMap = helper.findObserverMethod(observer)
            observerInfos.add(ObserverInfo(observer, methodMap))
        }
    }
    
    fun isRegistered(@NonNull observer: Observer): Boolean {
        synchronized(observerInfos) {
            return observerInfos.any { info ->
                info.weakObserver.get() === observer
            }
        }
    }
    
    fun unregisterObserver(@NonNull observer: Observer) {
        synchronized(observerInfos) {
            observerInfos.removeAll { info ->
                val o = info.weakObserver.get()
                o == null || observer === o
            }
        }
    }
    
    fun unregisterAll() {
        synchronized(observerInfos) {
            observerInfos.clear()
        }
        helper.clearCache()
    }
    
    private fun getObserverInfos(): List<ObserverInfo> {
        synchronized(observerInfos) {
            return observerInfos.mapNotNull { info ->
                info.takeIf { it.weakObserver.get() != null }
            }
        }
    }
    
    fun notifyObservers(
        @NonNull methodName: String,
        @Nullable vararg parameters: MethodInfo.Parameter?
    ) {
        notifyObservers(MethodInfo(methodName, *parameters))
    }
    
    fun notifyObservers(@NonNull info: MethodInfo) {
        val infos = getObserverInfos()
        
        infos.forEach { oi ->
            oi.weakObserver.get()?.let { observer ->
                val key = helper.generateKey(info.tag, info.name, info.parameterTypes)
                oi.methodMap[key]?.let { method ->
                    val runnable = helper.generateRunnable(observer, method, info)
                    posterDispatcher.post(method, runnable)
                }
            }
        }
    }
}