package com.topdon.commons.base.interfaces

/**
 * date: 2019/8/6 10:05
 * author: chuanfeng.bi
 */
interface Checkable<T> {
    fun isChecked(): Boolean
    
    fun setChecked(isChecked: Boolean): T
