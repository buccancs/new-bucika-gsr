package com.example.thermal_lite.camera.task;

import android.util.Log;

import com.example.thermal_lite.camera.CameraPreviewManager;


/**
 * Created by fengjibo on 2024/4/3.
 */
public class PausePreviewTask extends BaseTask {
    public PausePreviewTask(DeviceState deviceState) {
        this.mDeviceState = deviceState;
    }

    @Override
    public void run() {
        if (mDeviceState != DeviceState.PAUSED) {
            Log.d(TAG, "pausePreview start");
            CameraPreviewManager.getInstance().pausePreview();
            mDeviceState = DeviceState.PAUSED;
        }
    }
}
