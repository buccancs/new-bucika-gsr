package com.topdon.commons.observer

/**
 * 观察者
 * 
 * date: 2019/8/3 13:15
 * author: chuanfeng.bi
 */
interface Observer {
    /**
     * 数据变化
     */
    @Observe
    fun onChanged(o: Any?) {}
}