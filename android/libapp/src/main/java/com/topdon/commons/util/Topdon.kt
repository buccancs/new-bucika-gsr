package com.topdon.commons.util

import android.content.Context

object Topdon {
    private var app: Context? = null

    fun init(context: Context) {
        app = context
    }

    fun getApp(): Context? {
        return app
    }
