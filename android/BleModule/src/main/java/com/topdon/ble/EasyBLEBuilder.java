package com.topdon.ble;

import com.topdon.ble.util.Logger;
import com.topdon.commons.observer.Observable;
import com.topdon.commons.poster.ThreadMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * date: 2021/8/12 12:02
 * author: bichuanfeng
 */
public class EasyBLEBuilder {
    private final static ExecutorService DEFAULT_EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    BondController bondController;
    DeviceCreator deviceCreator;
    ThreadMode methodDefaultThreadMode = ThreadMode.MAIN;
    ExecutorService executorService = DEFAULT_EXECUTOR_SERVICE;
    ScanConfiguration scanConfiguration;
    Observable observable;
    Logger logger;
    boolean isObserveAnnotationRequired = false;
    ScannerType scannerType;

    EasyBLEBuilder() {
    }

    /**
     * 指定蓝牙扫描器，默认为系统Android5.0以上使用{@link ScannerType#LE}，否则使用{@link ScannerType#LEGACY}。
     * 系统小于Android5.0时，指定{@link ScannerType#LE}无效
     */
    public EasyBLEBuilder setScannerType(ScannerType scannerType) {
        Inspector.requireNonNull(scannerType, "scannerType can't be null");
        this.scannerType = scannerType;
        return this;
    }

    /**
     * 自定义线程池用来执行后台任务
     */
    public EasyBLEBuilder setExecutorService(ExecutorService executorService) {
        Inspector.requireNonNull(executorService, "executorService can't be null");
        this.executorService = executorService;
        return this;
    }

    /**
     * 设备实例构建器
     */
    public EasyBLEBuilder setDeviceCreator(DeviceCreator deviceCreator) {
        Inspector.requireNonNull(deviceCreator, "deviceCreator can't be null");
        this.deviceCreator = deviceCreator;
        return this;
    }

    /**
     * 配对控制器。如果设置了控制器，则会在连接时，尝试配对
     */
    public EasyBLEBuilder setBondController(BondController bondController) {
        Inspector.requireNonNull(bondController, "bondController can't be null");
        this.bondController = bondController;
        return this;
    }

    /**
     * 观察者或者回调的方法在没有使用注解指定调用线程时，默认被调用的线程
     */
    public EasyBLEBuilder setMethodDefaultThreadMode(ThreadMode mode) {
        Inspector.requireNonNull(mode, "mode can't be null");
        methodDefaultThreadMode = mode;
        return this;
    }

    /**
     * 搜索配置
     */
    public EasyBLEBuilder setScanConfiguration(ScanConfiguration scanConfiguration) {
        Inspector.requireNonNull(scanConfiguration, "scanConfiguration can't be null");
        this.scanConfiguration = scanConfiguration;
        return this;
    }

    /**
     * 日志打印
     */
    public EasyBLEBuilder setLogger(Logger logger) {
        Inspector.requireNonNull(logger, "logger can't be null");
        this.logger = logger;
        return this;
    }

    /**
     * 被观察者，消息发布者。
     * <br>如果观察者被设置，{@link #setMethodDefaultThreadMode(ThreadMode)}、
     * {@link #setObserveAnnotationRequired(boolean)}、{@link #setExecutorService(ExecutorService)}将不起作用
     */
    public EasyBLEBuilder setObservable(Observable observable) {
        Inspector.requireNonNull(observable, "observable can't be null");
        this.observable = observable;
        return this;
    }

    /**
     * 是否强制使用{@link Observe}注解才会收到被观察者的消息
     * 
     * @param observeAnnotationRequired true：只有方法上加{@link Observe}注解的才会收到消息。false：加不加注解都会收到消息
     */
    public EasyBLEBuilder setObserveAnnotationRequired(boolean observeAnnotationRequired) {
        isObserveAnnotationRequired = observeAnnotationRequired;
        return this;
    }

    /**
     * 根据当前配置构建EasyBLE实例
     */
    public EasyBLE build() {
        synchronized (EasyBLE.class) {
            if (EasyBLE.instance != null) {
                throw new EasyBLEException("EasyBLE instance already exists. It can only be instantiated once.");
            }
            EasyBLE.instance = new EasyBLE(this);
            return EasyBLE.instance;
        }
    }
}
