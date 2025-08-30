package com.example.thermal_lite.camera.task;

import android.os.SystemClock;
import android.util.Log;

import com.example.thermal_lite.camera.CameraPreviewManager;

public class StopPreviewTask extends BaseTask {

    public StopPreviewTask(DeviceState deviceState) {
        this.mDeviceState = deviceState;
    }

    @Override
    public void run() {
        if (mDeviceState != DeviceState.CLOSED) {
            Log.d(TAG, "stopPreview start");

            CameraPreviewManager.getInstance().stopPreview();
            SystemClock.sleep(100);
            CameraPreviewManager.getInstance().closePreview();
            //todo 这里的200ms是为了usb能完全停图，因为如果用户操作过快，触发停图出图，机芯还停止，就又出图会导致程序卡死
            SystemClock.sleep(200);
            mDeviceState = DeviceState.CLOSED;
            Log.d(TAG, "stopPreview end33");
        }
    }
}
