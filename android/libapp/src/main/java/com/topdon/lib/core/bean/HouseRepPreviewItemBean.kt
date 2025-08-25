package com.topdon.lib.core.bean

/**
 * @author qiang.lv
 */
data class HouseRepPreviewItemBean(
    var itemName: String? = null,
    var projectItemBeans: List<HouseRepPreviewProjectItemBean>? = null,
    var albumItemBeans: List<HouseRepPreviewAlbumItemBean>? = null
)