package com.example.thermal_lite.camera.task;

import android.util.Log;

import com.example.thermal_lite.camera.CameraPreviewManager;

/**
 * Created by fengjibo on 2024/4/3.
 */
public class ResumePreviewTask extends BaseTask {
    public ResumePreviewTask(DeviceState deviceState) {
        this.mDeviceState = deviceState;
    }

    @Override
    public void run() {
        if (mDeviceState != DeviceState.RESUMED) {
            Log.d(TAG, "resumePreview start");
            CameraPreviewManager.getInstance().resumePreview();
            mDeviceState = DeviceState.RESUMED;
        }
    }
}