package com.topdon.commons.base

data class SaveBean(
    var type: String? = null,
    var mac: String? = null,
    var name: String? = null
) {
    constructor(type: String, mac: String, name: String) : this() {
        this.type = type
        this.mac = mac
        this.name = name
    }
}
