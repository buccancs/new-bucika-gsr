package com.topdon.lib.core.bean

/**
 * @author qiang.lv
 */
data class HouseRepPreviewBean(
    var housePhoto: String? = null,
    var houseAddress: String? = null,
    var houseName: String? = null,
    var detectTime: String? = null,
    var inspectorName: String? = null,
    var houseYear: String? = null,
    var houseArea: String? = null,
    var expenses: String? = null,
    var itemBeans: List<HouseRepPreviewItemBean>? = null,
    var inspectorWhitePath: String? = null,
    var houseOwnerWhitePath: String? = null
)