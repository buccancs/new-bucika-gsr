package com.topdon.commons.observer

interface Observer {
    
    @Observe
    fun onChanged(o: Any?) {}
}
