package com.example.thermal_lite.camera;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.Utils;
import com.elvishew.xlog.XLog;
import com.energy.ac020library.IrcamEngine;
import com.energy.ac020library.IrcmdEngine;
import com.energy.ac020library.bean.AutoGainImageRes;
import com.energy.ac020library.bean.AutoGainSwitchCallback;
import com.energy.ac020library.bean.AutoGainSwitchInfo;
import com.energy.ac020library.bean.AutoGainSwitchParam;
import com.energy.ac020library.bean.CommonParams;
import com.energy.ac020library.bean.DevHandleParam;
import com.energy.ac020library.bean.ErrorCode;
import com.energy.ac020library.bean.HandleInitCallback;
import com.energy.ac020library.bean.IIrFrameCallback;
import com.energy.ac020library.bean.InfoLineBean;
import com.energy.ac020library.bean.IrcmdError;
import com.energy.ac020library.bean.UvcHandleParam;

import com.energy.commoncomponent.Const;
import com.energy.commoncomponent.bean.DeviceType;
import com.energy.commoncomponent.bean.RotateDegree;
import com.energy.commonlibrary.util.FileUtil;
import com.energy.commonlibrary.util.SharedPreferencesUtils;
import com.energy.commonlibrary.view.SurfaceNativeWindow;
import com.energy.irutilslibrary.LibIRParse;
import com.energy.irutilslibrary.LibIRProcess;
import com.energy.irutilslibrary.LibIRTemp;
import com.energy.irutilslibrary.bean.IRPROCSRCFMTType;
import com.energy.irutilslibrary.bean.LogLevel;
import com.energy.iruvccamera.bean.CameraSize;
import com.energy.iruvccamera.bean.UvcParams;
import com.energy.iruvccamera.usb.USBMonitor;
import com.example.thermal_lite.BuildConfig;
import com.example.thermal_lite.IrConst;
import com.example.thermal_lite.ui.activity.IrDisplayActivity;
import com.example.thermal_lite.util.CommonUtil;
import com.infisense.usbir.utils.IRImageHelp;
import com.infisense.usbir.utils.PseudocodeUtils;
import com.topdon.lib.core.bean.AlarmBean;
import com.topdon.lib.ui.widget.LiteSurfaceView;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by fengjibo on 2023/3/17.
 */
public class CameraPreviewManager {

    private final String TAG = "CameraPreviewManager";
    private IIrFrameCallback mIIrFrameCallback;
    public LiteSurfaceView mSurfaceView;

    private CommonParams.FrameOutputFormat FRAME_OUT_PUT_FORMAT = CommonParams.FrameOutputFormat.YUYV_AND_TEMP_OUTPUT;
    //出图预览宽高
    private int mPreviewWidth;
    private int mPreviewHeight;
    //机芯图像数据宽高
    private int mStreamWidth;
    private int mStreamHeight;
    //最终渲染宽高
    private int mFinalImageWidth = 0;
    private int mFinalImageHeight = 0;
    //UVCCamera请求数据格式
    private UvcParams.FrameFormatType mFrameFormatType = UvcParams.FrameFormatType.FRAME_FORMAT_YUYV;

    //出图交互类
    private IrcamEngine mIrcamEngine;

    private LibIRTemp mLibIRTemp;

    //图像当前角度旋转
    private RotateDegree mImageRotate = RotateDegree.DEGREE_270;
    private LibIRProcess.ImageRes_t mImageRes;

    //暂停输出图像标记
    private boolean mFramePause = false;

    //双图模式是否显示完整红外+温度图像
    private boolean mShowDoubleImage = false;
    private IRImageHelp irImageHelp;


    private OnTempDataChangeCallback mOnTempDataChangeCallback;

    private CameraPreviewManager() {
        irImageHelp = new IRImageHelp();
    }

    private static CameraPreviewManager mInstance;

    public static synchronized CameraPreviewManager getInstance() {
        if (mInstance == null) {
            mInstance = new CameraPreviewManager();
        }
        return mInstance;
    }

    private Handler mMainHandler;

    //红外数据
    private byte[] mIrData;
    //红外数据长度
    private int mIrLength;
    //信息行数据
    private int mInfoDataHeight = 0;
    private byte[] mInfoData;
    private int mInfoLength;
    //红外argb
    private byte[] mIrARGBData;
    //红外argb数据长度
    private int mIrARGBLength;
    private byte[] mIrYuvData;
    //旋转后的红外argb
    private byte[] mIrRotateData;
    //温度数据
    private byte[] mTempData;
    //旋转后的温度argb
    private byte[] mTempRotateData;
    //温度数据长度
    private int mTempLength;
    //zeta zoom code
    private byte[] mResultARBGDataForZetaZoom;
    public byte[] frameIrAndTempData = new byte[192 * 256 * 4];
    public byte[] takePhotoIrAndTempData = new byte[192 * 256 * 4];


    private boolean mIsShowFPS = true;
    private boolean mSaveData = false;
    private boolean mTakePhoto = false;

    private SurfaceNativeWindow mSurfaceNativeWindow;
    private Surface mSurface;
    private Bitmap mPhotoBitmap;

    private boolean mSunProtectEnable = false;

    private float max = Float.MAX_VALUE;
    private float min = Float.MIN_VALUE;
    private int pseudocolorMode = 3;
    private AlarmBean alarmBean;
    private int maxColor;
    private int minColor;

    //自动增益切换参数
    private boolean mAutoSwitchGainEnable = false;
    private AutoGainImageRes mAutoGainImageRes = new AutoGainImageRes();
    private AutoGainSwitchInfo mAutoGainSwitchInfo = new AutoGainSwitchInfo();
    private AutoGainSwitchParam mGainSwitchParam = new AutoGainSwitchParam();


    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    public boolean isSunProtectEnable() {
        return mSunProtectEnable;
    }

    public void setSunProtectEnable(boolean mSunProtectEnable) {
        this.mSunProtectEnable = mSunProtectEnable;
    }

    public void init(LiteSurfaceView surfaceView, Handler mainHandler) {
        this.mSurfaceView = surfaceView;
        this.mMainHandler = mainHandler;
        initData();

        mSurfaceNativeWindow = new SurfaceNativeWindow();
        mIIrFrameCallback = new IIrFrameCallback() {
            /**
             * 数据流回调
             * 根据设置的出图格式setFrameOutputFormat，处理数据流数据
             * @param frame 数据源
             * YUYV_IMAGE_OUTPUT(0)：图像 YUYV；分辨率 256*192； 每帧数据大小（字节）256*192*2=98304
             * NV12_IMAGE_OUTPUT(1)：图像 NV12；分辨率 256*192； 每帧数据大小（字节）256*192*1.5=73782
             * NV12_AND_TEMP_OUTPUT(2)：图像 NV12+信息行+温度Y16；分辨率 256*386； 每帧数据大小（字节）256*192*1.5+256*2*2+256*192*2=173110
             * YUYV_AND_TEMP_OUTPUT(0)：图像 YUYV+信息行+温度Y16；分辨率 256*386； 每帧数据大小（字节）256*192*2+256*2*2+256*192*2=197632
             * @param length 数据总长度
             */
            @Override
            public void onFrame(byte[] frame, int length) {
                try {
                    if (mFramePause) {
                        return;
                    }

                    // 帧率展示
                    if (mIsShowFPS) {
                        double fps = CommonUtil.showFps();
                        Log.d(TAG, "onFrame frame.length = " + length + " onFrame fps=" + String.format("%.1f", fps));
                        Message message = Message.obtain(mMainHandler, IrDisplayActivity.HANDLE_SHOW_FPS, fps);
                        mMainHandler.sendMessage(message);
                    }

                    //获取红外数据
                    System.arraycopy(frame, 0, mIrData, 0, mIrLength);
                    //保存红外数据到frameIrAndTempData
                    System.arraycopy(mIrData, 0, frameIrAndTempData, 0, mIrLength);


                    //信息行处理
                    if (!mShowDoubleImage) {
                        if (mInfoLength != 0 && mSunProtectEnable) {
                            //获取信息行数据
                            System.arraycopy(frame, mIrLength, mInfoData, 0, mInfoLength);
                            InfoLineBean infoLineBean = mIrcamEngine.getInfoLineBean(mInfoData);
                            //防灼伤保护，具体生效参数需要算法提供烧录参数的wholedata
                            if (infoLineBean.getSunProtectFlag() == 1 || infoLineBean.getHardwareSunProtectFlag() == 1) {
                                mMainHandler.sendEmptyMessage(IrDisplayActivity.HANDLE_SHOW_SUN_PROTECT_FLAG);
                            }
                        }
                    }

                    //获取温度数据
                    if (FRAME_OUT_PUT_FORMAT == CommonParams.FrameOutputFormat.YUYV_AND_TEMP_OUTPUT) {
                        if (!mShowDoubleImage) {
                            //获取温度数据
                            System.arraycopy(frame, mIrLength + mInfoLength, mTempData, 0, mTempLength);
                            //保存温度数据到frameIrAndTempData
                            System.arraycopy(frame, mIrLength + mInfoLength, frameIrAndTempData, mIrLength, mTempLength);

                        }
                        if (mOnTempDataChangeCallback != null) {
                            mOnTempDataChangeCallback.onTempDataChange(mTempData);
                        }
//                    mLibIRTemp.setTempData(mTempData);
//                    LibIRTemp.TemperatureSampleResult temperatureSampleResult =
//                            mLibIRTemp.getTemperatureOfRect(new Rect(0, 0, mPreviewWidth / 2, mPreviewHeight - 1));
//                    float maxTemperature = temperatureSampleResult.maxTemperature;
//                    float minTemperature = temperatureSampleResult.minTemperature;
//                    Log.d(TAG, "max temp : " + maxTemperature + " min temp : " + minTemperature);
                    } else if (FRAME_OUT_PUT_FORMAT == CommonParams.FrameOutputFormat.NV12_AND_TEMP_OUTPUT) {
                        //获取温度数据
                        System.arraycopy(frame, mIrLength + mInfoLength, mTempData, 0, mTempLength);

//                    mLibIRTemp.setTempData(mTempData);
//                    LibIRTemp.TemperatureSampleResult temperatureSampleResult =
//                            mLibIRTemp.getTemperatureOfRect(new Rect(0, 0, mPreviewWidth / 2, mPreviewHeight - 1));
//                    float maxTemperature = temperatureSampleResult.maxTemperature;
//                    float minTemperature = temperatureSampleResult.minTemperature;
//                    Log.d(TAG, "max temp : " + maxTemperature + " mix temp : " + minTemperature);
                    }

                    //数据格式转化yuv to argb
                    switch (FRAME_OUT_PUT_FORMAT) {
                        case YUYV_IMAGE_OUTPUT:
                        case YUYV_AND_TEMP_OUTPUT:
                            if (Const.DEVICE_TYPE == DeviceType.DEVICE_TYPE_GL1280) {
                                CommonUtil.convertArrayY16ToY14(mIrData, 2 * mPreviewWidth * mPreviewHeight, mIrYuvData);
                                LibIRParse.convertArrayY14ToARGB(mIrYuvData, mPreviewWidth * mPreviewHeight, mIrARGBData);
//                            com.infisense.iruvc.sdkisp.LibIRProcess.convertYuyvMapToARGBPseudocolor(mIrYuvData,
//                                    mPreviewWidth * mPreviewHeight,
//                                    PseudocodeUtils.INSTANCE.changePseudocodeModeByOld(3), mIrARGBData);
                            } else {
                                LibIRParse.converyArrayYuv422ToARGB(mIrData, mPreviewWidth * mPreviewHeight, mIrARGBData);
                                if (irImageHelp.getColorList() == null) {
                                    com.energy.iruvc.sdkisp.LibIRProcess.convertYuyvMapToARGBPseudocolor(mIrData,
                                            mPreviewWidth * mPreviewHeight,
                                            PseudocodeUtils.INSTANCE.changePseudocodeModeByOld(pseudocolorMode), mIrARGBData);
                                }else {
                                    //先灰度化
                                    com.energy.iruvc.sdkisp.LibIRProcess.convertYuyvMapToARGBPseudocolor(mIrData,
                                            mPreviewWidth * mPreviewHeight,
                                            PseudocodeUtils.INSTANCE.changePseudocodeModeByOld(1), mIrARGBData);
                                }
                                irImageHelp.customPseudoColor(mIrARGBData,mTempData,mPreviewWidth,mPreviewHeight);
                                /*
                                 * 等温尺处理,展示伪彩的温度范围内信息
                                 */
                                irImageHelp.setPseudoColorMaxMin(mIrARGBData,mTempData,max,min,mPreviewWidth,mPreviewHeight);
                                mIrARGBData = irImageHelp.contourDetection(alarmBean,
                                        mIrARGBData, mTempData, mPreviewWidth, mPreviewHeight);
                            }
                            break;
                        case NV12_IMAGE_OUTPUT:
                        case NV12_AND_TEMP_OUTPUT:
                            Log.d(TAG, "NV12_AND_TEMP_OUTPUT");
                            LibIRParse.NV12ToRGBA(mIrData, mPreviewWidth, mPreviewHeight, mIrARGBData);

                            //todo 切换数据源后，temp data 被切换成中间出图数据，数据格式y16
                            break;
                        default:
                            break;
                    }
//
                    //处理图像旋转角度
                    mFinalImageWidth = 0;
                    mFinalImageHeight = 0;


                    handleSurfaceDisplay();

                    //自动增益切换
                    //内部逻辑，在ac020上, 增益切换长命令调用后, 直接返回success,需要调用basic_long_time_vdcmd_state_get不断的获取状态
                    if (mAutoSwitchGainEnable && FRAME_OUT_PUT_FORMAT == CommonParams.FrameOutputFormat.YUYV_AND_TEMP_OUTPUT) {
                        Log.d(TAG, "onAutoGainSwitchState switch");
                        mIrcamEngine.advAutoGainSwitch(mTempData, mAutoGainImageRes, mAutoGainSwitchInfo, mGainSwitchParam, new AutoGainSwitchCallback() {
                            @Override
                            public void onAutoGainSwitchState(int gainselStatus) {
                                Log.d(TAG, "onAutoGainSwitchState : " + gainselStatus);
                            }

                            @Override
                            public void onAutoGainSwitchResult(int gainselStatus, int result) {
                                Log.d(TAG, "onAutoGainSwitchResult : " + gainselStatus);
                                Log.d(TAG, "onAutoGainSwitchResult : " + result);
                            }
                        });
                    }
                }catch (Exception e){
                    XLog.e(TAG,"Lite图像处理异常"+e.getMessage());
                }
            }
        };
    }

    public void initData() {
        //根据机芯图像数据格式决定出图模式
        //如果FrameOutputFormat为NV12_IMAGE_OUTPUT或者NV12_IMAGE_OUTPUT，mFrameFormatType改成FRAME_FORMAT_NV12
        //如果FrameOutputFormat为YUYV_AND_TEMP_OUTPUT或YUYV_IMAGE_OUTPUT，mFrameFormatType改成FRAME_FORMAT_YUYV

        // 枚举出来的分辨率，并传入到模组
        mStreamWidth = IrConst.DEFAULT_STREAM_WIDTH;
        mStreamHeight = IrConst.DEFAULT_STREAM_HEIGHT;

        boolean isDoubleImage = IrConst.DEFAULT_DOUBLE_IMAGE;
        if (isDoubleImage) {
            setFrameOutPutFormat(CommonParams.FrameOutputFormat.YUYV_AND_TEMP_OUTPUT);
        } else {
            setFrameOutPutFormat(CommonParams.FrameOutputFormat.YUYV_IMAGE_OUTPUT);
        }
        //信息行
        if (Const.DEVICE_TYPE == DeviceType.DEVICE_TYPE_X3
                || Const.DEVICE_TYPE == DeviceType.DEVICE_TYPE_P2L
                || Const.DEVICE_TYPE == DeviceType.DEVICE_TYPE_X2PRO
                || Const.DEVICE_TYPE == DeviceType.DEVICE_TYPE_TC2C) {
            mInfoDataHeight = 2;
        } else {
            mInfoDataHeight = 0;
        }
        switch (FRAME_OUT_PUT_FORMAT) {
            case YUYV_IMAGE_OUTPUT:
                /**
                 * 图像 YUYV；分辨率 256*192； 每帧数据大小（字节）256*192*2=98304
                 */
                mFrameFormatType = UvcParams.FrameFormatType.FRAME_FORMAT_YUYV;

                // 预览画面处理，需要考虑信息行
                mPreviewWidth = mStreamWidth;
                mPreviewHeight = mStreamHeight - mInfoDataHeight;
                // 原始红外数据
                mIrLength = mPreviewWidth * mPreviewHeight * 2;
                mIrData = new byte[mIrLength];
                // 信息行
                mInfoLength = mPreviewWidth * mInfoDataHeight * 2;
                mInfoData = new byte[mInfoLength];
                //
                mIrARGBLength = mPreviewWidth * mPreviewHeight * 2 * 2;
                mIrARGBData = new byte[mIrARGBLength];
                mIrYuvData = new byte[mIrLength / 2];
                mIrRotateData = new byte[mIrARGBLength];

                //zeta zoom code
                initZetaZoomData();

                break;
            case YUYV_AND_TEMP_OUTPUT:
                /**
                 * 图像 YUYV+信息行+温度Y16；分辨率 256*386； 每帧数据大小（字节）256*192*2+256*2*2+256*192*2=197632
                 */
                mFrameFormatType = UvcParams.FrameFormatType.FRAME_FORMAT_YUYV;

                // 预览画面处理，需要考虑信息行
                if (mShowDoubleImage) {
                    mPreviewWidth = mStreamWidth;
                    mPreviewHeight = mStreamHeight;
                } else {
                    mPreviewWidth = mStreamWidth;
                    mPreviewHeight = (mStreamHeight - mInfoDataHeight) / 2;
                }

                // 原始红外数据
                mIrLength = mPreviewWidth * mPreviewHeight * 2;
                mIrData = new byte[mIrLength];
                if (!mShowDoubleImage) {
                    // 信息行
                    mInfoLength = mPreviewWidth * mInfoDataHeight * 2;
                    mInfoData = new byte[mInfoLength];
                }

                mIrARGBLength = mPreviewWidth * mPreviewHeight * 2 * 2;
                mIrARGBData = new byte[mIrARGBLength];
                mTempLength = mPreviewWidth * mPreviewHeight * 2;
                mTempData = new byte[mTempLength];
                mIrRotateData = new byte[mIrARGBLength];
                mTempRotateData = new byte[mIrARGBLength];
                break;
            case NV12_IMAGE_OUTPUT:
                /**
                 * 图像 NV12
                 * 分辨率 640*512
                 * 每帧数据大小（字节）640*512*1.5=491520
                 */
                mFrameFormatType = UvcParams.FrameFormatType.FRAME_FORMAT_NV12;

                mPreviewWidth = 640;
                mPreviewHeight = 512;

                mStreamWidth = 640;
                mStreamHeight = 512;

                mIrLength = (int) (mPreviewWidth * mPreviewHeight * 1.5);
                mIrData = new byte[mIrLength];
                mIrARGBLength = mPreviewWidth * mPreviewHeight * 2 * 2;
                mIrARGBData = new byte[mIrARGBLength];

                break;
            case NV12_AND_TEMP_OUTPUT:
                /**
                 * 图像 NV12+信息行+温度Y16+Dummy
                 * 分辨率 640*900
                 * 每帧数据大小（字节）640*512*1.5+640*2*2+640*512*2+640*2*2=1152000
                 */
                mFrameFormatType = UvcParams.FrameFormatType.FRAME_FORMAT_NV12;

                mPreviewWidth = 640;
                mPreviewHeight = 512;

                mStreamWidth = 640;
                mStreamHeight = 1200;

                mIrLength = (int) (mPreviewWidth * mPreviewHeight * 1.5);
                mIrData = new byte[mIrLength];
                mIrARGBLength = mPreviewWidth * mPreviewHeight * 2 * 2;
                mIrARGBData = new byte[mIrARGBLength];
                mTempLength = mPreviewWidth * mPreviewHeight * 2;
                mTempData = new byte[mTempLength];
                break;
            default:
                break;
        }

        Log.i(TAG, "mPreviewWidth = " + mPreviewWidth + " mPreviewHeight = " + mPreviewHeight);
        mLibIRTemp = new LibIRTemp(mPreviewWidth, mPreviewHeight);
        mImageRes = new LibIRProcess.ImageRes_t();
        mImageRes.width = (char) mPreviewWidth;
        mImageRes.height = (char) mPreviewHeight;

        mAutoGainImageRes.width = 256;
        mAutoGainImageRes.height = 192;

        // 自动增益切换参数auto gain switch parameter
        mGainSwitchParam.above_pixel_prop = 0.1f;    //用于high -> low gain,图像像素总面积的百分比
        mGainSwitchParam.above_temp_data = (int) ((130 + 273.15) * 16 * 4); //用于high -> low gain,高增益向低增益切换的触发温度,130为摄氏度
        mGainSwitchParam.below_pixel_prop = 0.95f;   //用于low -> high gain,图像像素总面积的百分比
        mGainSwitchParam.below_temp_data = (int) ((150 + 273.15) * 16 * 4);//用于low -> high gain,低增益向高增益切换的触发温度，150为摄氏度
        mAutoGainSwitchInfo.switch_frame_cnt = 5 * 15; //连续满足触发条件帧数超过该阈值会触发自动增益切换(假设出图帧率为每秒15帧，则5 * 15大概为5秒)
        mAutoGainSwitchInfo.waiting_frame_cnt = 7 * 15;//触发自动增益切换之后，会间隔该阈值的帧数不进行增益切换监测(假设出图帧率为每秒15帧，则7 * 15大概为7秒)

    }

    public void handleUSBConnect(USBMonitor.UsbControlBlock ctrlBlock) {
        initHandleEngine(ctrlBlock, true);
    }

    public void handleUSBConnectNoPreview(USBMonitor.UsbControlBlock ctrlBlock) {
        initHandleEngine(ctrlBlock, false);
    }

    private void handleStartPreview() {
        startPreview();
        if (Const.DEVICE_TYPE == DeviceType.DEVICE_TYPE_WN2640) {
            //mimi640 模组, 大疆固件版本，上电后，需要一段的时间加载数据，此时无法进行命令发送，需等待10s以上
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    //mimi640 模组，大疆固件版本，需要先发送basicVideoStreamContinue命令，打开数据流
                    IrcmdError basicVideoStreamContinueResult = DeviceIrcmdControlManager.getInstance()
                            .getIrcmdEngine().basicVideoStreamContinue();
                    Log.d(TAG, "basicVideoStreamContinueResult=" + basicVideoStreamContinueResult);
                    mMainHandler.sendEmptyMessage(IrDisplayActivity.HIDE_LOADING);
                }
            }, 10000);
        } else {
            mMainHandler.sendEmptyMessage(IrDisplayActivity.HIDE_LOADING);
        }
    }

    public Bitmap scaledBitmap(){
        return scaledBitmap(false);
    }
    byte[] tmpData = null;
    public Bitmap scaledBitmap(Boolean isTakePhoto){
        if (tmpData == null){
            tmpData = new byte[mIrARGBLength];
        }
        System.arraycopy(mIrRotateData,0,tmpData,0,mIrARGBLength);
        mPhotoBitmap = Bitmap.createBitmap(mFinalImageWidth, mFinalImageHeight, Bitmap.Config.ARGB_8888);
        if (isTakePhoto){
            System.arraycopy(frameIrAndTempData, 0, takePhotoIrAndTempData, 0, takePhotoIrAndTempData.length);
        }
        mPhotoBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(tmpData));
        return mPhotoBitmap;
    }

    /**
     * 获取支持的设备信息列表
     *
     * @return
     */
    public List<CameraSize> getAllSupportedSize() {
        return mIrcamEngine.getUsbSupportInfo();
    }

    /**
     * 初始化命令交互类，出图交互类
     */
    private void initHandleEngine(USBMonitor.UsbControlBlock ctrlBlock, boolean isStartPreview) {
        UvcHandleParam uvcHandleParam = new UvcHandleParam();
        /**
         * 设置uvccamera出图需要的参数
         */
        uvcHandleParam.setCtrlBlock(ctrlBlock);

        int fps = IrConst.DEFAULT_STREAM_FPS;
        /**
         * 调整帧率，必须设备支持才可以，否则会出图失败
         */
        uvcHandleParam.setFps(fps);

        float bandwidth = SharedPreferencesUtils.getFloat(Utils.getApp(),
                IrConst.KEY_DEFAULT_STREAM_BANDWIDTH, IrConst.DEFAULT_STREAM_BANDWIDTH);

        /**
         * 调整带宽
         * 部分分辨率或在部分机型上，会出现无法出图，或出图一段时间后卡顿的问题，需要配置对应的带宽
         */
        uvcHandleParam.setBandwidth(bandwidth);

        Log.d(TAG, "initHandleEngine UvcHandleParam = " + uvcHandleParam.toString());

        LibIRProcess.irprocessLogRegister(LogLevel.SDK_LOG_NO_PRINT);
        LibIRProcess.getIRProcessVersion();
        LibIRParse.irparseLogRegister(LogLevel.SDK_LOG_NO_PRINT);
        LibIRParse.getIRParseVersion();
        LibIRTemp.irtempLogRegister(LogLevel.SDK_LOG_NO_PRINT);
        LibIRTemp.getIRTempVersion();

        mIrcamEngine = IrcamEngine.Builder()
                .setLogLevel(CommonParams.LogLevel.SDK_LOG_DEBUG)
                .setStreamWidth(mStreamWidth)
                .setStreamHeight(mStreamHeight)
                .setDriverType(CommonParams.DriverType.USB)
                /**
                 * 设置出图模式
                 */
                .setFrameOutputFormat(FRAME_OUT_PUT_FORMAT)
                .setUvcHandleParam(uvcHandleParam)
                .build();
        Log.d(TAG, "stopPreview onSuccess initHandle : ");
        mIrcamEngine.initHandle(new HandleInitCallback() {
            @Override
            public void onSuccess(IrcmdEngine ircmdEngine) {
                DeviceIrcmdControlManager.getInstance().setIrcamEngine(mIrcamEngine);
                DeviceIrcmdControlManager.getInstance().setIrcmdEngine(ircmdEngine);
                Log.d(TAG, "IrcamVersion : " + mIrcamEngine.ircamVersion());
                Log.d(TAG, "IrcmdVersion : " + ircmdEngine.ircmdVersion());
                Log.d(TAG, "IrcamVersion number: " + mIrcamEngine.ircamVersionNumber());
                Log.d(TAG, "IrcmdVersion number: " + ircmdEngine.ircmdVersionNumber());
                if (isStartPreview) {
                    handleStartPreview();
                }
            }

            @Override
            public void onFail(ErrorCode errorCode) {
                mMainHandler.sendEmptyMessage(IrDisplayActivity.HANDLE_INIT_FAIL);
            }
        });
    }

    /**
     * 开始出图
     */
    public void startPreview() {
        Log.d(TAG, "startPreview");
        if (mIrcamEngine != null) {
            mIrcamEngine.setIrFrameCallback(mIIrFrameCallback);
            int result = mIrcamEngine.startVideoStream();
            if (result != 0) {
                mMainHandler.sendEmptyMessage(IrDisplayActivity.PREVIEW_FAIL);
            }
            if (Const.DEVICE_TYPE == DeviceType.DEVICE_TYPE_X3) {
                DeviceIrcmdControlManager.getInstance().sendFPGAParam();
                DeviceIrcmdControlManager.getInstance().sendISPParam();
            }
        }
        TempCompensation.getInstance().startTempCompensation();
    }

    /**
     * 暂停出图
     */
    public void pausePreview() {
        if (mIrcamEngine != null) {
            mIrcamEngine.pauseVideoStream();
        }
    }

    /**
     * 恢复出图
     */
    public void resumePreview() {
        if (mIrcamEngine != null) {
            mIrcamEngine.resumeVideoStream();
        }
    }

    /**
     * 结束出图
     */
    public void closePreview() {
        if (mIrcamEngine != null) {
            mIrcamEngine.closeVideoStream();
            mIrcamEngine.releaseVideoStream();
            mIrcamEngine.destroyHandle();
            mIrcamEngine = null;
        }
    }

    /**
     * 停止出图
     */
    public void stopPreview() {
        Log.i(TAG, "stopPreview");
//        TempCompensation.getInstance().stopTempCompensation();
        if (Const.DEVICE_TYPE == DeviceType.DEVICE_TYPE_WN2640) {
            //WN2640固件版本，退出之前，需要发停止数据流的命令
            IrcmdError ircmdError = DeviceIrcmdControlManager.getInstance().getIrcmdEngine()
                    .basicVideoStreamPause();
            Log.d(TAG, "basicVideoStreamPause=" + ircmdError);
        }
        if (mIrcamEngine != null) {
            mIrcamEngine.setIrFrameCallback(null);
            mIrcamEngine.stopVideoStream();
        }
    }

    /**
     * 回收资源
     */
    public void releaseSource() {
        mIIrFrameCallback = null;
        mIrARGBData = null;
        mIrData = null;
        mOnTempDataChangeCallback = null;
        setAutoSwitchGainEnable(false);
        DeviceIrcmdControlManager.getInstance().setIrcmdEngine(null);
        DeviceIrcmdControlManager.getInstance().setIrcamEngine(null);
    }

    public void updateDevHandleParam(DevHandleParam devHandleParam) {
        if (mIrcamEngine != null) {
            mIrcamEngine.updateDevHandleParam(devHandleParam);
        }
    }

    public void setSaveData(boolean mSaveData) {
        this.mSaveData = mSaveData;
    }

    public void setTakePhoto(boolean takePhoto) {
        this.mTakePhoto = takePhoto;
    }

    public IrcamEngine getIrcamEngine() {
        return mIrcamEngine;
    }

    public void setImageRotate(RotateDegree imageRotate) {
        this.mImageRotate = imageRotate;
        mIrRotateData = null;
        mIrRotateData = new byte[mIrARGBLength];
        Log.d(TAG, "setImageRotate : " + imageRotate.getValue());
    }

    public RotateDegree getImageRotate() {
        return mImageRotate;
    }

    public void setFramePause(boolean framePause) {
        this.mFramePause = framePause;
    }

    public void setFrameOutPutFormat(CommonParams.FrameOutputFormat frameOutPutFormat) {
        FRAME_OUT_PUT_FORMAT = frameOutPutFormat;
    }

    public void setShowDoubleImage(boolean showDoubleImage) {
        this.mShowDoubleImage = showDoubleImage;
    }

    public void setAutoSwitchGainEnable(boolean mAutoSwitchGainEnable) {
        this.mAutoSwitchGainEnable = mAutoSwitchGainEnable;
    }

    public boolean getAutoSwitchGainEnable() {
        return mAutoSwitchGainEnable;
    }

    public void setOnTempDataChangeCallback(OnTempDataChangeCallback onTempDataChangeCallback) {
        this.mOnTempDataChangeCallback = onTempDataChangeCallback;
    }

    public interface OnTempDataChangeCallback {
        void onTempDataChange(byte[] data);
    }

    private void handleSurfaceDisplay() {

        switch (mImageRotate) {
            case DEGREE_0:
                mFinalImageWidth = mPreviewWidth;
                mFinalImageHeight = mPreviewHeight;
                System.arraycopy(mIrARGBData,0,mIrRotateData,0,mIrARGBData.length);
                break;
            case DEGREE_90:
                mFinalImageWidth = mPreviewHeight;
                mFinalImageHeight = mPreviewWidth;
                LibIRProcess.rotateRight90(mIrARGBData, mImageRes,
                        IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888, mIrRotateData);
                break;
            case DEGREE_180:
                mFinalImageWidth = mPreviewWidth;
                mFinalImageHeight = mPreviewHeight;
                LibIRProcess.rotate180(mIrARGBData, mImageRes,
                        IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888, mIrRotateData);
                break;
            case DEGREE_270:
                mFinalImageWidth = mPreviewHeight;
                mFinalImageHeight = mPreviewWidth;
                LibIRProcess.rotateLeft90(mIrARGBData, mImageRes,
                        IRPROCSRCFMTType.IRPROC_SRC_FMT_ARGB8888, mIrRotateData);
                break;
            default:
                break;
        }
        try {
            mSurfaceView.setMIrRotateData(mIrRotateData.clone());
            mSurfaceView.setMFinalImageWidth(mFinalImageWidth);
            mSurfaceView.setMFinalImageHeight(mFinalImageHeight);
            //通过NativeWindow绘制
            mSurface = mSurfaceView.getHolder().getSurface();
            if (mSurface != null) {
                mSurfaceNativeWindow.onDrawFrame(mSurface, mIrRotateData, mFinalImageWidth, mFinalImageHeight);
            }
        }catch (Exception e){
            XLog.e(TAG+":lite的图像渲染异常："+e.getMessage());
        }
    }

    //==============================Zeta Zoom start =======================================//
    private void initZetaZoomData() {
        //zeta zoom code
//        if (BuildConfig.zetazoomEnable) {
//            ZetaZoomHelper.getInstance().initData(mPreviewWidth, mPreviewHeight);
//            int imageWidth = ZetaZoomHelper.getInstance().getImageWidth();
//            int imageHeight = ZetaZoomHelper.getInstance().getImageHeight();
//            Log.d(TAG, "imageWidth" + imageWidth);
//            Log.d(TAG, "imageHeight" + imageHeight);
//            //zeta zoom code
//            mResultARBGDataForZetaZoom = new byte[imageWidth * imageHeight * 4];
//        }
    }

    private void handleSurfaceDisplayForZetaZoom() {
        //zeta zoom code
//        boolean isZetaZoom = ZetaZoomHelper.getInstance().isZetaZoomEnable();
//        if (isZetaZoom) {
//            Log.d(TAG, "isZetaZoom");
//            ZetaZoomHelper.getInstance().zetazoomRun(mIrData, mResultARBGDataForZetaZoom);
//            mFinalImageWidth = ZetaZoomHelper.getInstance().getImageWidth();
//            mFinalImageHeight = ZetaZoomHelper.getInstance().getImageHeight();
//            //通过NativeWindow绘制
//            mSurface = mSurfaceView.getHolder().getSurface();
//            if (mSurface != null) {
//                mSurfaceNativeWindow.onDrawFrame(mSurface, mResultARBGDataForZetaZoom, mFinalImageWidth, mFinalImageHeight);
//            }
//        } else {
//            handleSurfaceDisplay();
//        }
    }

    //==============================Zeta Zoom end =======================================//

    public AlarmBean getAlarmBean() {
        return alarmBean;
    }

    public void setAlarmBean(AlarmBean alarmBean) {
        this.alarmBean = alarmBean;
    }

    public void setLimit(float max, float min, int maxColor, int minColor) {
        this.max = max;
        this.min = min;
        this.maxColor = maxColor;
        this.minColor = minColor;
    }

    public void setColorList(int[] colorList, @Nullable float[] places, boolean isUseGray, float customMaxTemp, float customMinTemp) {
        irImageHelp.setColorList(colorList, places, isUseGray,customMaxTemp,customMinTemp);
    }
    public void setPseudocolorMode(int pseudocolorMode) {
        this.pseudocolorMode = pseudocolorMode;
    }
}
