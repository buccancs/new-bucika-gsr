package com.topdon.commons.util

import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

class WeakReferenceHandler<T>(referencedObject: T, looper: Looper? = null) : Handler(looper ?: Looper.getMainLooper()) {
    
    private val mReference = WeakReference(referencedObject)
    
    constructor(referencedObject: T) : this(referencedObject, null)
    
    protected val referencedObject: T?
        get() = mReference.get()
}
