package com.topdon.tc001.utils

object VersionTools {
    private var mDownloadId: Long = 0L
    
    fun setMDownloadId(id: Long) {
        mDownloadId = id
    }
    
    fun getMDownloadId(): Long {
        return mDownloadId
    }
}