package com.topdon.ble.callback

import com.topdon.ble.Request

interface NotificationChangeCallback : RequestFailedCallback {
    fun onNotificationChanged(request: Request, isEnabled: Boolean)
}
