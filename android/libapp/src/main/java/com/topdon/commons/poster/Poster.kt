package com.topdon.commons.poster

/**
 * date: 2019/8/7 09:44
 * author: chuanfeng.bi
 */
internal interface Poster {
    /**
     * 将要执行的任务加入队列
     * 
     * @param runnable 要执行的任务
     */
    fun enqueue(runnable: Runnable)

    /**
     * 清除队列任务
     */
    fun clear()
}