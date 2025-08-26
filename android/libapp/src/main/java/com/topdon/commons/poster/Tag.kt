package com.topdon.commons.poster

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Tag(
    val value: String = ""
)
