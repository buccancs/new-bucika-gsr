package com.topdon.tc001.tools

object VersionTools {
    var mDownloadId = 0L
    
    fun setMDownloadId(id: Long) {
        mDownloadId = id
    }
    
    fun getMDownloadId(): Long {
        return mDownloadId
    }
}
