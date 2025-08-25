package com.topdon.lib.core.bean

/**
 * @author qiang.lv
 */
data class HouseRepPreviewProjectItemBean(
    var projectName: String? = null,
    // 1-没问题 2-需维修 3-需更换
    var state: Int = 1,
    var remark: String? = null
)