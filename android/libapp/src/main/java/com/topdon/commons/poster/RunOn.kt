package com.topdon.commons.poster

/**
 * 标记方法执行线程
 * 
 * date: 2019/8/2 23:53
 * author: chuanfeng.bi
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class RunOn(
    /**
     * 运行线程
     */
    val value: ThreadMode = ThreadMode.UNSPECIFIED
)