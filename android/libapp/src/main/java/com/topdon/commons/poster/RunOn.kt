package com.topdon.commons.poster

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class RunOn(
    
    val value: ThreadMode = ThreadMode.UNSPECIFIED
