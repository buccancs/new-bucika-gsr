package com.infisense.usbdual.camera;

import com.energy.iruvc.dual.DualUVCCamera;
import com.infisense.usbdual.Const;

import java.util.ArrayList;

public abstract class BaseDualView {

    protected ArrayList<OnFrameCallback> onFrameCallbacks;
    public DualUVCCamera dualUVCCamera;

    protected int fusionLength;
    protected int irSize;
    protected int vlSize;
    protected int remapTempSize;
    protected byte[] remapTempData;
    protected byte[] mixData;
    protected byte[] normalTempData;
    protected byte[] mixDataRotate;
    protected byte[] irData;
    public byte[] vlData;
    public byte[] vlARGBData;

    public BaseDualView() {
        onFrameCallbacks = new ArrayList<>();
        fusionLength = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4;
        irSize = Const.IR_WIDTH * Const.IR_HEIGHT;
        vlSize = Const.VL_WIDTH * Const.VL_HEIGHT * 3;
        remapTempSize = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2;
        remapTempData = new byte[Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2];
        mixData = new byte[fusionLength];
        normalTempData = new byte[irSize * 2];
        irData = new byte[irSize * 2];
        vlData = new byte[vlSize];
        vlARGBData = new byte[fusionLength];
    }

    public interface OnFrameCallback {
        void onFame(byte[] mixData, byte[] remapTempData, double fpsText);
    }

    public void addFrameCallback(OnFrameCallback onFrameCallback) {
        onFrameCallbacks.add(onFrameCallback);
    }

    public void removeFrameCallback(OnFrameCallback onFrameCallback) {
        onFrameCallbacks.remove(onFrameCallback);
    }
}
