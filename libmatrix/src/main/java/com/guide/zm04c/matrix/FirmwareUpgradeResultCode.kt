package com.guide.zm04c.matrix

enum class FirmwareUpgradeResultCode {

    SUCCESS("Success", 0),
    FILE_ERROR("File path is null", 1),
    FILE_NOT_EXISTS("File does not exists", 3),
    USB_DEVICE_ERROR("USB device is invalid", 4),
    FILE_READ_ERROR("Read upgrade file error", 5),
    PAGE_ERROR("Upgrade page error", 6),
    RENDER_DATA_ERROR("Render data is not available", 7),
    INVALID_FILE_ERROR("Upgrade file is invalid", 8),
    FILE_WRITE_ERROR("Write upgrade file error", 9);

    private var msg: String? = null
    private var code = 0

    constructor (msg: String, code: Int) {
        this.msg = msg
        this.code = code
    }

    open fun getMsg(): String? {
        return msg
    }

    open fun setMsg(msg: String?) {
        this.msg = msg
    }

    open fun getCode(): Int {
        return code
    }

    open fun setCode(code: Int) {
        this.code = code
    }
}