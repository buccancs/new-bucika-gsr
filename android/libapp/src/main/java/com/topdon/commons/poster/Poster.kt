package com.topdon.commons.poster

internal interface Poster {
    
    fun enqueue(runnable: Runnable)

    fun clear()
}