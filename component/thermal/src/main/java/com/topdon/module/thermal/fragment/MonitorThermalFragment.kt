package com.topdon.module.thermal.fragment

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ToastUtils
import com.guide.zm04c.matrix.GuideInterface
import com.guide.zm04c.matrix.IrSurfaceView
import com.topdon.lib.core.bean.tools.ScreenBean
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.FileConfig.galleryPath
import com.topdon.lib.core.db.AppDatabase
import com.topdon.lib.core.db.entity.ThermalEntity
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.utils.ByteUtils.getIndex
import com.topdon.lib.core.utils.ScreenShotUtils
import com.topdon.lib.ui.fence.FenceLineView
import com.topdon.lib.ui.fence.FencePointView
import com.topdon.lib.ui.fence.FenceView
import com.topdon.module.thermal.R
import com.topdon.module.thermal.activity.MonitorActivity
import com.topdon.module.thermal.base.BaseThermalFragment
import com.topdon.module.thermal.fragment.event.ThermalActionEvent
import com.topdon.module.thermal.tools.Fence
import com.topdon.module.thermal.tools.ThermalTool
import com.topdon.module.thermal.tools.medie.IYapVideoProvider
import com.topdon.module.thermal.tools.medie.YapVideoEncoder
import com.topdon.module.thermal.utils.ArrayUtils
import kotlinx.android.synthetic.main.fragment_thermal.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.math.BigDecimal
import java.util.*

/**
 * 热成像
 */
class MonitorThermalFragment : BaseThermalFragment(), IYapVideoProvider<Bitmap> {

    protected var mIrSurfaceViewLayout: FrameLayout? = null
    protected var mIrSurfaceView: IrSurfaceView? = null

    override fun initContentView() = R.layout.fragment_monitor_thermal
    private val msgLiveData by lazy { MutableLiveData<Int>() }

    //设置温度展示的位置
    private fun setViewPosition(imageView: ImageView, index: Int) {
        if (rawWidth == 0 || rawHeight == 0) {
            return
        }
        val vg = imageView.parent as ViewGroup
        val pw = vg.width
        val ph = vg.height
        val y = index / rawWidth
        val x = index - y * rawWidth
        val x1 = x * pw / rawWidth
        val y1 = y * ph / rawHeight
        val maxX = x1 - imageView.width / 2
        val maxY = y1 - imageView.height / 2
//        Log.w("123", "真实位置 maxX:$maxX, maxY:$maxY")
        imageView.x = maxX.toFloat()
        imageView.y = maxY.toFloat()
    }

    private var mGuideInterface: GuideInterface? = null


    override fun initView() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        rotateType = 3//默认旋转270度
        mCenterTextView = temp_display
        mMaxTextView = max_temp_display
        mMinTextView = min_temp_display
        maxImg = max_img
        minImg = min_img
        mDisplayFrameLayout = temp_display_layout
        mFenceLayout = fence_lay
        mDisplayFrameLayout!!.visibility = View.GONE
        mFenceLayout!!.visibility = View.GONE
        mIrSurfaceViewLayout = final_ir_layout
        mIrSurfaceView = IrSurfaceView(requireContext())
        val ifrSurfaceViewLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER
        )
        mIrSurfaceView!!.layoutParams = ifrSurfaceViewLayoutParams
        mIrSurfaceView!!.setMatrix(ThermalTool.getRotate(rotateType), 256f, 192f)
        mIrSurfaceViewLayout!!.addView(mIrSurfaceView)
//        width = resources.getDimension(R.dimen.ir_width).toInt()
//        height = resources.getDimension(R.dimen.ir_height).toInt()
        val screenWidth = ScreenUtils.getScreenWidth()
        val screenHeight = screenWidth * 270 / 360
        Log.w("123", "screenWidth比例:${screenWidth} / $screenHeight")
        Log.w("123", "screenWidth比例:${screenWidth.toFloat() / screenHeight}")
        width = screenWidth
        height = screenHeight
        highCrossWidth = resources.getDimension(R.dimen.high_cross_width).toInt()
        highCrossHeight = resources.getDimension(R.dimen.high_cross_height).toInt()
        mIrSurfaceViewLayout!!.viewTreeObserver.addOnGlobalLayoutListener {
            irSurfaceViewLayoutParams =
                mIrSurfaceViewLayout!!.layoutParams as ConstraintLayout.LayoutParams?
            displayViewLayoutParams =
                mDisplayFrameLayout!!.layoutParams as FrameLayout.LayoutParams
            fenceLayoutParams = mFenceLayout!!.layoutParams as FrameLayout.LayoutParams
            cameraLayoutParams = mFenceLayout!!.layoutParams as FrameLayout.LayoutParams
            when (rotateType) {
                1, 3 -> {
                    irSurfaceViewWidth = height
                    irSurfaceViewHeight = width
                    if (irSurfaceViewWidth < width) {
                        irSurfaceViewWidth = width
                        irSurfaceViewHeight = screenWidth * 360 / 270
                    }
                }
                0, 2 -> {
                    irSurfaceViewWidth = width
                    irSurfaceViewHeight = height
                }
            }
            irSurfaceViewLayoutParams!!.width = irSurfaceViewWidth
            irSurfaceViewLayoutParams!!.height = irSurfaceViewHeight
            mIrSurfaceViewLayout!!.layoutParams = irSurfaceViewLayoutParams
            displayViewLayoutParams!!.width = irSurfaceViewWidth
            displayViewLayoutParams!!.height = irSurfaceViewHeight
            mDisplayFrameLayout!!.layoutParams = displayViewLayoutParams
            fenceLayoutParams!!.width = irSurfaceViewWidth
            fenceLayoutParams!!.height = irSurfaceViewHeight
            mFenceLayout!!.layoutParams = fenceLayoutParams

//            Log.i("123", "修改后w:${mIrSurfaceView!!.width}, h:${mIrSurfaceView!!.height}")
        }
        //初始选取范围
        initFence()
        //初始图像
        onIrVideoStart()
        mIrSurfaceView!!.post {
            Log.w("123", "w:${mIrSurfaceView!!.width}, h:${mIrSurfaceView!!.height}")
        }

        msgLiveData.observe(this) { msg ->
            if (msg == 0) {
                mCenterTextView!!.text = "中心温 $mCenter"
                mMaxTextView!!.text = "最高温 $mMaxTemp"
                mMinTextView!!.text = "最低温 $mMinTemp"
                maxImg?.let { setViewPosition(it, maxIndex) }
                minImg?.let { setViewPosition(it, minIndex) }
            }
        }
    }

    override fun initData() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        isRecord = false
        onIrVideoStop()

    }


    /**
     * 开启视频流
     */
    fun onIrVideoStart() {
        mIsIrVideoStart = if (mIsIrVideoStart) {
            ToastUtils.showShort("视频流已开启")
            return
        } else {
            true
        }
        mGuideInterface = GuideInterface()
        val ret = mGuideInterface!!.init(requireContext(), object : GuideInterface.IrDataCallback {
            override fun processIrData(yuv: ByteArray, temp: FloatArray) {
                //刷新图像
                if (mIrBitmap == null) {
                    mIrBitmap = Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
                }
                mGuideInterface!!.yuv2Bitmap(mIrBitmap, yuv)//视频转码yuv
//                mIrBitmap = mIrBitmap?.let { rotateBitmap(it, 90f) }
                try {
                    mIrSurfaceView!!.doDraw(mIrBitmap, mGuideInterface!!.getImageStatus())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (rotateType == 1 || rotateType == 3) {
                    rawWidth = SRC_WIDTH
                    rawHeight = SRC_HEIGHT
                } else {
                    rawWidth = SRC_HEIGHT
                    rawHeight = SRC_WIDTH
                }
                val centerIndex = rawWidth * (rawHeight / 2) + rawWidth / 2
                try {
                    //选取区域
                    val maxTempIndex = ArrayUtils.getMaxIndex(temp, rotateType, selectIndex)
                    val minTempIndex = ArrayUtils.getMinIndex(temp, rotateType, selectIndex)
                    maxIndex = maxTempIndex
                    minIndex = minTempIndex
                    val rotateData = ArrayUtils.matrixRotate(srcData = temp, rotateType)
                    val bigDecimal = BigDecimal.valueOf(rotateData[centerIndex].toDouble())
                    val maxBigDecimal = BigDecimal.valueOf(rotateData[maxTempIndex].toDouble())
                    val minBigDecimal = BigDecimal.valueOf(rotateData[minTempIndex].toDouble())
                    mCenter = bigDecimal.setScale(1, BigDecimal.ROUND_HALF_UP).toFloat()
                    mMaxTemp = maxBigDecimal.setScale(1, BigDecimal.ROUND_HALF_UP).toFloat()
                    mMinTemp = minBigDecimal.setScale(1, BigDecimal.ROUND_HALF_UP).toFloat()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "提取温度异常:${e.message}")
                }
            }

        })

        if (ret == 5) {
            Log.w("123", "视频流开启完成")
        } else {
//            ToastUtils.showShort("视频流开启失败")
            Log.w("123", "视频流开启失败")
            mGuideInterface = null
            mIsIrVideoStart = false
        }
    }

    private fun rotateBitmap(origin: Bitmap, rotate: Float): Bitmap? {
        try {
            if (origin == null) {
                return null
            }
            val width = origin.width
            val height = origin.height
            val matrix = Matrix()
            matrix.setRotate(rotate)
            val newBitmap = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false)
            if (newBitmap.equals(origin)) {
                return newBitmap
            }
            origin.recycle()
            return newBitmap
        } catch (e: Exception) {
            Log.e("123", "error:${e.message}")
            return origin
        }
    }

    /**
     * 停止视频流
     */
    fun onIrVideoStop() {
        mIsIrVideoStart = if (!mIsIrVideoStart) {
            Log.w("123", "视频流已停止")
            return
        } else {
            false
        }
        mGuideInterface!!.exit()
        mGuideInterface = null
        Log.w("123", "视频流停止完成")
    }


    fun onLowRangeBtnClick(view: View?) {
        if (mGuideInterface == null) {
            ToastUtils.showShort("请先开启视频流")
            return
        }
        mGuideInterface!!.setRange(1)
        ToastUtils.showShort("切换到常温档成功")
    }

    fun onHighRangeBtnClick(view: View?) {
        if (mGuideInterface == null) {
            ToastUtils.showShort("请先开启视频流")
            return
        }
        mGuideInterface!!.setRange(2)
        ToastUtils.showShort("切换到高温档成功")
    }

    /**
     * 温度显示
     */
    fun onTempBtnClick() {
        if (mGuideInterface == null) {
            ToastUtils.showShort("请先开启视频流")
            return
        }
        isDispLayTemp = !isDispLayTemp
        if (isDispLayTemp) {
            mDisplayFrameLayout!!.visibility = View.VISIBLE
            timerJob = lifecycleScope.launch {
                repeat(Int.MAX_VALUE) {
                    msgLiveData.postValue(0)
                    delay(1000)
                }
            }
        } else {
            mDisplayFrameLayout!!.visibility = View.GONE
            if (timerJob != null && timerJob!!.isActive) {
                timerJob!!.cancel()
                timerJob = null
            }
        }
    }


    //***************************************专家模式**********************************************
    /**
     * 专家模式
     */
    fun onExpertModeClick(view: View?) {
        System.arraycopy(EXPERT_HITS, 1, EXPERT_HITS, 0, EXPERT_HITS.size - 1)
        EXPERT_HITS[EXPERT_HITS.size - 1] = System.currentTimeMillis()
        if (EXPERT_HITS[0] >= System.currentTimeMillis() - EXPERT_MODE_HIT_DURATION) {
            if (mExpertLayout!!.visibility == View.GONE) {
                mExpertLayout!!.visibility = View.VISIBLE
            } else {
                mExpertLayout!!.visibility = View.GONE
            }
            EXPERT_HITS = LongArray(EXPERT_MODE_HIT_COUNT)
        }
    }

    fun onNucShutterClick(view: View?) {
        if (mGuideInterface == null) {
            ToastUtils.showShort("请先开启视频流")
            return
        }
        mGuideInterface!!.nuc()
    }

//    private fun showTipDialog(tip: String, type: Int) {
//        val tipDialog = TipDialog.Builder(requireContext())
//            .setIconType(type)
//            .setTipWord(tip)
//            .create()
//        tipDialog.show()
//        mHandler.postDelayed({
//            try {
//                tipDialog.dismiss()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }, 1500)
//    }

    fun onLut(view: View) {
        mIrSurfaceView!!.setOpenLut()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun action(event: ThermalActionEvent) {
        Log.w("123", "event:${event.action}")
        when (event.action) {
            1001 -> {
                //拍照
                ToastUtils.showShort("拍照")
                picture()
            }
            1002 -> {
                //录制
                ToastUtils.showShort("录制")
                video()
            }
            2001 -> {
                //添加点
                addPoint()
            }
            2002 -> {
                //添加线
                addLine()
            }
            2003 -> {
                //添加围栏
                addFence()
            }
            2004 -> {
                //添加温度
                onTempBtnClick()
            }
            2006 -> {
                //清除还原
                clearFence()
            }
            in 3000..3010 -> {
                //设置伪彩
                setColor(event.action)
            }
            in 5000..5010 -> {
                //全屏
                full()
            }
            10001 -> {
                //开始记录
                recordThermal()
            }
            10003 -> {
                //停止记录
                isRecord = false
            }
        }
    }

    private fun clearFence() {
        fenceFlag = 0x000
        mFenceLayout!!.visibility = View.GONE
        selectIndex.clear()
    }

    /**
     * 设置伪彩
     */
    private fun setColor(action: Int) {
        var type: Int = action % 3000 - 1
        if (type < 0 || type > 10) {
            type = 0
        }
        updatePalette(type)
    }

    /**
     * 色带
     */
    private fun updatePalette(index: Int) {
        if (mGuideInterface == null) {
            ToastUtils.showShort("请先开启视频流")
            return
        }
        mGuideInterface!!.changePalette(index)
    }

    var fenceFlag = 0x000

    private fun addPoint() {
        showFence(1)
        type = "point"
    }

    private fun addLine() {
        showFence(2)
        type = "line"
    }

    private fun addFence() {
        showFence(3)
        type = "fence"
    }

    //显示点线面布局
    private fun showFence(index: Int) {
        if (fenceFlag.getIndex(index) == 0) {
            fenceFlag = 1.shl(4 * (index - 1)) //设置001 or 010 or 100
            mFenceLayout!!.visibility = View.VISIBLE
            fence_point_view.visibility = if (fenceFlag.getIndex(1) > 0) View.VISIBLE else View.GONE
            fence_line_view.visibility = if (fenceFlag.getIndex(2) > 0) View.VISIBLE else View.GONE
            fence_view.visibility = if (fenceFlag.getIndex(3) > 0) View.VISIBLE else View.GONE
        } else {
            fenceFlag = 0x000
            mFenceLayout!!.visibility = View.GONE
        }
    }

    var selectIndex: ArrayList<Int> = arrayListOf()//选取点

    private fun initFence() {
        fence_point_view.listener = object : FencePointView.CallBack {
            override fun callback(startPoint: IntArray, srcRect: IntArray) {
                //获取点
                val activity: MonitorActivity = requireActivity() as MonitorActivity
                selectIndex.clear()
                selectIndex =
                    Fence(srcRect = srcRect, rotateType = rotateType).getPointIndex(startPoint)
                activity.select(1, selectIndex)
            }
        }
        fence_line_view.listener = object : FenceLineView.CallBack {
            override fun callback(startPoint: IntArray, endPoint: IntArray, srcRect: IntArray) {
                //获取线
                selectIndex = Fence(srcRect = srcRect, rotateType = rotateType)
                    .getLineIndex(startPoint, endPoint)
                val activity: MonitorActivity = requireActivity() as MonitorActivity
                activity.select(2, selectIndex)
            }

        }
        fence_view.listener = object : FenceView.CallBack {
            override fun callback(startPoint: IntArray, endPoint: IntArray, srcRect: IntArray) {
                //获取面
                selectIndex = Fence(srcRect = srcRect, rotateType = rotateType)
                    .getAreaIndex(startPoint, endPoint)
                val activity: MonitorActivity = requireActivity() as MonitorActivity
                activity.select(3, selectIndex)
            }

        }
    }

    private fun picture() {
//        ScreenShotUtils.shotScreen(requireContext(), temp_display_lay, 1, ScreenBean())
        ScreenShotUtils.shotScreenBitmap(requireContext(), mIrBitmap, 1, ScreenBean())
    }

    var isVideoRunning = false

    private fun video() {
        if (isVideoRunning) {
            Log.w("123", "正在录制")
            return
        }
        val latestResultPath = "${galleryPath}YapBitmapToMp4_${System.currentTimeMillis()}.mp4"
        Log.w("123", "latestResultPath:$latestResultPath")
        YapVideoEncoder(this, File(latestResultPath)).start()
    }

    private fun full() {
        rotateType = if (rotateType == 0) {
            Log.w("123", "横屏显示")
            1
        } else {
            0
        }
        mIrSurfaceView!!.setMatrix(ThermalTool.getRotate(rotateType), 256f, 192f)
    }

    override fun size(): Int = 5 * 60

    override fun next(): Bitmap {
        return if (mIrBitmap == null) {
            Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
        } else {
            mIrBitmap!!
        }
    }

    override fun progress(progress: Float) {
        Log.w("123", "progress:$progress")
        isVideoRunning = progress > 0 || progress < 100
    }

    var isRecord = false
    var type = ""
    var timeMillis = 1000L //间隔1s

    private fun recordThermal() {
        val thermalId = TimeTool.showDateSecond()
        lifecycleScope.launch {
            isRecord = true
            val activity: MonitorActivity = requireActivity() as MonitorActivity
            var time = 0L
            while (isRecord) {
                activity.updateTime(time)
                val bean = ThermalEntity()
                bean.userId = SharedManager.getUserId()
                bean.thermalId = thermalId
                bean.thermal = mCenter
                bean.thermalMax = mMaxTemp
                bean.thermalMin = mMinTemp
                bean.type = type
                bean.createTime = System.currentTimeMillis()
                AppDatabase.getInstance().thermalDao().insert(bean)
                delay(timeMillis)
                time++
            }
            Log.w("123", "停止记录, 数据量:$time")
        }
    }
}