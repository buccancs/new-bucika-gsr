package com.example.thermal_lite.camera;

import android.hardware.usb.UsbDevice;

import com.energy.iruvccamera.usb.USBMonitor;

/**
 * @Package: com.energy.ac020.usbir.camera
 * @ClassName: OnUSBConnectListener
 * @Description:
 * @Author: brilliantzhao
 * @CreateDate: 3/29/2023 3:26 PM
 * @UpdateUser:
 * @UpdateDate: 3/29/2023 3:26 PM
 * @UpdateRemark:
 * @Version: 1.0.0
 */
public interface OnUSBConnectListener {

    void onAttach(UsbDevice device);

    void onGranted(UsbDevice usbDevice, boolean granted);

    void onDetach(UsbDevice device);

    void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew);

    void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock);

    void onCancel(UsbDevice device);

    void onCompleteInit();

}
