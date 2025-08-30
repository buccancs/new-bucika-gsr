package com.topdon.pseudo.constant

/**
 * @author: CaiSongL
 * @date: 2023/8/8 19:37
 */
object ColorRecommend {

    val colorList1 = intArrayOf(
        0xff0000ff.toInt(),
        0xffff0000.toInt(),
        0xffffff00.toInt(),
    )

    val colorList2 = intArrayOf(
        0xff000000.toInt(),
        0xffffffff.toInt(),
        0xffff0000.toInt(),
    )
    val colorList3 = intArrayOf(
        0xff0000ff.toInt(),
        0xff00ff00.toInt(),
        0xffffff00.toInt(),
        0xffff0000.toInt(),
    )
    val colorList3TC007 = intArrayOf(
        0xff0000ff.toInt(),
        0xff00ff00.toInt(),
        0xffff0000.toInt(),
    )
    val colorList4 = intArrayOf(
        0xff000000.toInt(),
        0xFF840000.toInt(),
        0xffff0000.toInt(),
    )
    val colorList5 = intArrayOf(
        0xff0000ff.toInt(),
        0xFF7B7B83.toInt(),
        0xffffff00.toInt(),
    )

    /**
     *
     * @param index 0-铁红 1-黑红 2-自然 3-岩浆 4-辉金
     */
    fun getColorByIndex(isTC007: Boolean, index: Int): IntArray = when (index) {
        0 -> colorList1
        1 -> colorList2
        2 -> if (isTC007) colorList3TC007 else colorList3
        3 -> colorList4
        else -> colorList5
    }

}