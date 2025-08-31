package com.example.thermal_lite.camera.task;

/**
 * Created by fengjibo on 2022/4/19.
 */
public interface IDeviceConnectListener {
    void onPrepareConnect();
    void onConnected();
    void onDisconnected();
    void onPaused();
    void onResumed();
}
