package com.topdon.commons.observer

import java.lang.ref.WeakReference
import java.lang.reflect.Method

/**
 * date: 2019/8/9 16:19
 * author: chuanfeng.bi
 */
internal class ObserverInfo(
    observer: Observer,
    val methodMap: Map<String, Method>
) {
    val weakObserver = WeakReference(observer)
}