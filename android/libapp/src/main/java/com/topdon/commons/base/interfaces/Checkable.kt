package com.topdon.commons.base.interfaces

interface Checkable<T> {
    fun isChecked(): Boolean
    
    fun setChecked(isChecked: Boolean): T
}
