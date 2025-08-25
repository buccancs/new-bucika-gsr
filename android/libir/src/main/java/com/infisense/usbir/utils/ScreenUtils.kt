package com.infisense.usbir.utils

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.PopupWindow
import com.energy.iruvc.utils.CommonParams

object ScreenUtils {

    /**
     * 获得屏幕高度
     *
     * @param context
     * @return
     */
    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.widthPixels
    }

    /**
     * 获得屏幕宽度
     *
     * @param context
     * @return
     */
    fun getScreenHeight(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.heightPixels
    }

    /**
     * 获得状态栏的高度
     *
     * @param context
     * @return
     */
    fun getStatusHeight(context: Context): Int {
        var statusHeight = -1
        try {
            val clazz = Class.forName("com.android.internal.R\$dimen")
            val obj = clazz.newInstance()
            val height = clazz.getField("status_bar_height").get(obj).toString().toInt()
            statusHeight = context.resources.getDimensionPixelSize(height)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return statusHeight
    }

    /**
     * 获取当前屏幕截图，包含状态栏
     *
     * @param activity
     * @return
     */
    fun snapShotWithStatusBar(activity: Activity): Bitmap? {
        val view = activity.window.decorView
        view.isDrawingCacheEnabled = true
        view.buildDrawingCache()
        val bmp = view.drawingCache
        val width = getScreenWidth(activity)
        val height = getScreenHeight(activity)
        val bp = Bitmap.createBitmap(bmp, 0, 0, width, height)
        view.destroyDrawingCache()
        return bp
    }

    /**
     * 得到设备的dpi
     */
    fun getScreenDensityDpi(context: Context): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * 获取当前屏幕截图，不包含状态栏
     *
     * @param activity
     * @return
     */
    fun snapShotWithoutStatusBar(activity: Activity): Bitmap? {
        val view = activity.window.decorView
        view.isDrawingCacheEnabled = true
        view.buildDrawingCache()
        val bmp = view.drawingCache
        val frame = Rect()
        activity.window.decorView.getWindowVisibleDisplayFrame(frame)
        val statusBarHeight = frame.top

        val width = getScreenWidth(activity)
        val height = getScreenHeight(activity)
        val bp = Bitmap.createBitmap(bmp, 0, statusBarHeight, width, height - statusBarHeight)
        view.destroyDrawingCache()
        return bp
    }

    /**
     * 获取 虚拟按键的高度
     *
     * @param context
     * @return
     */
    fun getBottomStatusHeight(context: Context): Int {
        val totalHeight = getDpi(context)
        val contentHeight = getScreenHeight(context)
        return totalHeight - contentHeight
    }

    // 获取屏幕原始尺寸高度，包括虚拟功能键高度
    fun getDpi(context: Context): Int {
        var dpi = 0
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val displayMetrics = DisplayMetrics()
        try {
            val c = Class.forName("android.view.Display")
            val method = c.getMethod("getRealMetrics", DisplayMetrics::class.java)
            method.invoke(display, displayMetrics)
            dpi = displayMetrics.heightPixels
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dpi
    }

    /**
     * dp转px
     * 16dp - 48px
     * 17dp - 51px
     */
    fun dip2px(context: Context, dpValue: Float): Int {
        val scale = getScreenDendity(context)
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 屏幕密度比例
     */
    fun getScreenDendity(context: Context): Float {
        return context.resources.displayMetrics.density // 3
    }

    /**
     * @param info
     * @setIcon 设置对话框图标
     * @setTitle 设置对话框标题
     * @setMessage 设置对话框消息提示
     * setXXX方法返回Dialog对象，因此可以链式设置属性
     */
    fun showNormalDialog(context: Context, info: String, dismissListener: PopupWindow.OnDismissListener): Dialog {
        val normalDialog = AlertDialog.Builder(context)
        normalDialog.setTitle("Info")
        normalDialog.setMessage(info)
        normalDialog.setCancelable(false)
        normalDialog.setPositiveButton("OK") { _, _ ->
            // ...To-do
            dismissListener.onDismiss()
        }
        // 显示
        return normalDialog.show()
    }

    /**
     * 获取出图的帧率
     *
     * @return
     */
    fun getPreviewFPSByDataFlowMode(defaultDataFlowMode: CommonParams.DataFlowMode): Int {
        return if (defaultDataFlowMode == CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT) {
            25
        } else {
            50
        }
    }
