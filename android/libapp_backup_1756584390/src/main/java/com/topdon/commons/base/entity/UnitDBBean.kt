package com.topdon.commons.base.entity

import java.io.Serializable

data class UnitDBBean(
    var dbid: Long? = null,
    var loginName: String? = null,
    var unitType: Int = 0,
    var conversionRelation: String? = null,
    var preUnit: String? = null,
    var preName: String? = null,
    var afterUnit: String? = null,
    var afterName: String? = null,
    var conversionFormula: String? = null,
    var calcFactor: String? = null
) : Serializable {
    
    companion object {
        private const val serialVersionUID = -1L
    }
}