package com.example.thermal_lite.camera;

import android.util.Log;

import com.energy.iruvccamera.usb.USBMonitor;
import com.example.thermal_lite.camera.task.DeviceControlWorker;
import com.example.thermal_lite.camera.task.IDeviceConnectListener;
import com.example.thermal_lite.camera.task.PausePreviewTask;
import com.example.thermal_lite.camera.task.ResumePreviewTask;
import com.example.thermal_lite.camera.task.StartPreviewTask;
import com.example.thermal_lite.camera.task.StopPreviewTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fengjibo on 2024/5/17.
 */
public class DeviceControlManager implements IDeviceConnectListener {

    private static final String TAG = "DualDeviceControlManager";

    private DeviceControlWorker mDeviceControlWorker;

    private HashMap<String, IDeviceConnectListener> mIDeviceConnectListeners;

    private DeviceControlManager() {

    }

    private static DeviceControlManager mInstance;

    public static synchronized DeviceControlManager getInstance() {
        if (mInstance == null) {
            mInstance = new DeviceControlManager();
        }
        return mInstance;
    }

    /**
     * 初始化
     */
    public void init() {
        mDeviceControlWorker = new DeviceControlWorker();
        mDeviceControlWorker.setDeviceControlCallback(this);
        mDeviceControlWorker.startWork();
        mIDeviceConnectListeners = new HashMap<>();
    }

    /**
     * 注册设备状态回调，可在activity或fragment中注册，用于UI的改变
     * @param key 唯一标识
     * @param iDeviceConnectListener
     */
    public void addDeviceConnectListener(String key, IDeviceConnectListener iDeviceConnectListener) {
        if (mIDeviceConnectListeners != null) {
            mIDeviceConnectListeners.put(key, iDeviceConnectListener);
        }
    }

    /**
     * 取消注册设备状态回调
     * @param key
     */
    public void removeDeviceConnectListener(String key) {
        if (mIDeviceConnectListeners != null) {
            mIDeviceConnectListeners.remove(key);
        }
    }

    /**
     * 回收资源
     */
    public void release() {
        if (mDeviceControlWorker != null) {
            mDeviceControlWorker.release();
            mDeviceControlWorker = null;
        }
        if (mIDeviceConnectListeners != null) {
            mIDeviceConnectListeners.clear();
            mIDeviceConnectListeners = null;
        }
    }

    /**
     * 双光数据流出图
     * @param ctrlBlock
     */
    public void handleStartPreview(USBMonitor.UsbControlBlock ctrlBlock) {
        if (mDeviceControlWorker != null) {
            Log.d(TAG, "handleStartPreview");
            mDeviceControlWorker.addTask(new StartPreviewTask(ctrlBlock, mDeviceControlWorker.getDeviceState()));
        }
    }

    /**
     * 双光数据流停图
     */
    public void handleStopPreview() {
        if (mDeviceControlWorker != null) {
            Log.d(TAG, "handleStopPreview");
            mDeviceControlWorker.addTask(new StopPreviewTask(mDeviceControlWorker.getDeviceState()));
        }
    }

    /**
     * 双光数据流暂停
     */
    public void handlePauseDualPreview() {
        if (mDeviceControlWorker != null) {
            Log.d(TAG, "handlePausePreview");
            mDeviceControlWorker.addTask(new PausePreviewTask(mDeviceControlWorker.getDeviceState()));
        }
    }

    /**
     * 双光数据流恢复
     */
    public void handleResumeDualPreview() {
        if (mDeviceControlWorker != null) {
            Log.d(TAG, "handleResumePreview");
            mDeviceControlWorker.addTask(new ResumePreviewTask(mDeviceControlWorker.getDeviceState()));
        }
    }

    @Override
    public void onPrepareConnect() {
        //StartPreview前回调
        for (Map.Entry<String, IDeviceConnectListener> entry: mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onPrepareConnect();
        }
    }

    @Override
    public void onConnected() {
        //StartPreview成功前后回调，注意是子线程
        for (Map.Entry<String, IDeviceConnectListener> entry: mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onConnected();
        }
    }

    @Override
    public void onDisconnected() {
        //StopPreview成功前后回调，注意是子线程
        for (Map.Entry<String, IDeviceConnectListener> entry: mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onDisconnected();
        }
    }

    @Override
    public void onPaused() {
        //todo 自行定义Paused Task来实现
        for (Map.Entry<String, IDeviceConnectListener> entry: mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onPaused();
        }
    }

    @Override
    public void onResumed() {
        //todo 自行定义Resumed Task来实现
        for (Map.Entry<String, IDeviceConnectListener> entry: mIDeviceConnectListeners.entrySet()) {
            entry.getValue().onResumed();
        }
    }
}