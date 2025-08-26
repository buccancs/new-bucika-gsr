package com.topdon.commons.base.entity

import com.topdon.commons.base.interfaces.Checkable

data class CheckableItem<T>(
    var data: T? = null,
    private var isChecked: Boolean = false
) : Checkable<CheckableItem<T>> {
    
    constructor(data: T) : this(data, false)
    
    override fun isChecked(): Boolean = isChecked
    
    override fun setChecked(isChecked: Boolean): CheckableItem<T> {
        this.isChecked = isChecked
        return this
    }
}
