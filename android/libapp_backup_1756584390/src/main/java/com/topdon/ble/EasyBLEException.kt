package com.topdon.ble

class EasyBLEException : RuntimeException {
    constructor(message: String) : super(message)
    
    constructor(message: String, cause: Throwable) : super(message, cause)
    
    constructor(cause: Throwable) : super(cause)
    
    companion object {
        private const val serialVersionUID = -7775315841108791634L
    }
}
