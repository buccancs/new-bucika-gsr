package com.topdon.house.event

/**
 * 一个检测目录下的项目编辑成功事件.
 *
 * Created by LCG on 2024/1/5.
 *
 * @param dirId 目录 id
 */
data class DetectItemListEvent(val dirId: Long)