package com.topdon.ble

import com.topdon.ble.util.Logger
import com.topdon.commons.observer.Observable
import com.topdon.commons.poster.ThreadMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EasyBLEBuilder internal constructor() {
    
    companion object {
        private val DEFAULT_EXECUTOR_SERVICE: ExecutorService = Executors.newCachedThreadPool()
    }
    
    var bondController: BondController? = null
        private set
    var deviceCreator: DeviceCreator? = null
        private set
    var methodDefaultThreadMode: ThreadMode = ThreadMode.MAIN
        private set
    var executorService: ExecutorService = DEFAULT_EXECUTOR_SERVICE
        private set
    var scanConfiguration: ScanConfiguration? = null
        private set
    var observable: Observable? = null
        private set
    var logger: Logger? = null
        private set
    var isObserveAnnotationRequired: Boolean = false
        private set
    var scannerType: ScannerType? = null
        private set

    fun setScannerType(scannerType: ScannerType): EasyBLEBuilder {
        Inspector.requireNonNull(scannerType, "scannerType can't be null")
        this.scannerType = scannerType
        return this
    }

    fun setExecutorService(executorService: ExecutorService): EasyBLEBuilder {
        Inspector.requireNonNull(executorService, "executorService can't be null")
        this.executorService = executorService
        return this
    }

    fun setDeviceCreator(deviceCreator: DeviceCreator): EasyBLEBuilder {
        Inspector.requireNonNull(deviceCreator, "deviceCreator can't be null")
        this.deviceCreator = deviceCreator
        return this
    }

    fun setBondController(bondController: BondController): EasyBLEBuilder {
        Inspector.requireNonNull(bondController, "bondController can't be null")
        this.bondController = bondController
        return this
    }

    fun setMethodDefaultThreadMode(mode: ThreadMode): EasyBLEBuilder {
        Inspector.requireNonNull(mode, "mode can't be null")
        methodDefaultThreadMode = mode
        return this
    }

    fun setScanConfiguration(scanConfiguration: ScanConfiguration): EasyBLEBuilder {
        Inspector.requireNonNull(scanConfiguration, "scanConfiguration can't be null")
        this.scanConfiguration = scanConfiguration
        return this
    }

    fun setLogger(logger: Logger): EasyBLEBuilder {
        Inspector.requireNonNull(logger, "logger can't be null")
        this.logger = logger
        return this
    }

    fun setObservable(observable: Observable): EasyBLEBuilder {
        Inspector.requireNonNull(observable, "observable can't be null")
        this.observable = observable
        return this
    }

    fun setObserveAnnotationRequired(observeAnnotationRequired: Boolean): EasyBLEBuilder {
        isObserveAnnotationRequired = observeAnnotationRequired
        return this
    }

    fun build(): EasyBLE {
        synchronized(EasyBLE::class.java) {
            if (EasyBLE.instance != null) {
                throw EasyBLEException("EasyBLE instance already exists. It can only be instantiated once.")
            }
            EasyBLE.instance = EasyBLE(this)
            return EasyBLE.instance!!
        }
    }
}