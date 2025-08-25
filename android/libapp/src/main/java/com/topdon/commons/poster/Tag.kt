package com.topdon.commons.poster

/**
 * 可用于方法的唯一标识
 * 
 * date: 2019/8/2 23:53
 * author: chuanfeng.bi
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Tag(
    val value: String = ""
