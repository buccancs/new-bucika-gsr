package com.topdon.commons.observer

import java.lang.ref.WeakReference
import java.lang.reflect.Method

internal class ObserverInfo(
    observer: Observer,
    val methodMap: Map<String, Method>
) {
    val weakObserver = WeakReference(observer)
}