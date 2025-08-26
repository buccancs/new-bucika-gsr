package com.topdon.commons.util

import android.text.TextUtils
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.reflect.TypeToken
import com.topdon.commons.base.entity.UnitDBBean
import com.topdon.lms.sdk.utils.SPUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object UnitUtils {
    
    @JvmStatic
    fun getUnitDBBeanList(unitType: Int): List<UnitDBBean> {
        return try {
            val app = Topdon.getApp() ?: return emptyList()
            val jsonStr = if (unitType == 0) {
                PreUtil.getInstance(app).get(SPKeyUtils.UNIT_METRIC)
            } else {
                PreUtil.getInstance(app).get(SPKeyUtils.UNIT_BRITISH)
            }
            LLog.w("bcf--jsonStr", jsonStr)
            if (TextUtils.isEmpty(jsonStr)) {
                emptyList()
            } else {
                GsonUtils.fromJson(jsonStr, object : TypeToken<List<UnitDBBean>>() {}.type) ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    @JvmStatic
    fun getUnitDBBeanHashMap(): HashMap<String, UnitDBBean> {
        val unit = SPUtils.getInstance(Topdon.getApp()).get("unit", "0") as String
        val unitType = if ("0" == unit) 0 else 1
        return getUnitDBBeanHashMap(unitType)
    }
    
    @JvmStatic
    fun getUnitDBBeanHashMap(unitType: Int): HashMap<String, UnitDBBean> {
        val hashMap = HashMap<String, UnitDBBean>()
        try {
            val unitDBBeanList = getUnitDBBeanList(unitType)
            for (unitDBBean in unitDBBeanList) {
                hashMap[unitDBBean.preUnit ?: ""?.lowercase() ?: ""] = unitDBBean
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return hashMap
    }
    
    @JvmStatic
    fun getCalcResult(hashMap: HashMap<String, UnitDBBean>, preUnit: String, numericalValue: String): Array<String> {
        val app = Topdon.getApp() ?: return arrayOf(numericalValue, preUnit)
        val unit = SPUtils.getInstance(app).get("unit", "0") as String
        val unitType = if ("0" == unit) 0 else 1
        return getCalcResult(unitType, hashMap, preUnit, numericalValue)
    }
    
    @JvmStatic
    fun getCalcResult(unitType: Int, hashMap: HashMap<String, UnitDBBean>, preUnit: String, numericalValue: String): Array<String> {
        var unitDBBean: UnitDBBean? = null
        try {
            if (TextUtils.isEmpty(preUnit)) {
                return arrayOf(numericalValue, preUnit)
            }
            
            unitDBBean = hashMap[preUnit.lowercase()]
            if (unitDBBean == null) {
                return arrayOf(numericalValue, preUnit)
            }
            
            if (unitType == 0) {
                if (preUnit.equals(unitDBBean.afterUnit ?: "", ignoreCase = true)) {
                    return arrayOf(numericalValue, unitDBBean.afterUnit ?: "")
                }
                
                when {
                    preUnit.equals("K", ignoreCase = true) -> {
                        return try {
                            arrayOf(getResult(numericalValue.toDouble() - 273.15).toString(), unitDBBean.afterUnit ?: "")
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                            arrayOf(numericalValue, unitDBBean.afterUnit ?: "")
                        }
                    }
                    preUnit == "deg.F" -> {
                        return try {
                            arrayOf(getResult((numericalValue.toDouble() - 32) / 1.8).toString(), "°C")
                        } catch (e: Exception) {
                            e.printStackTrace()
                            arrayOf(numericalValue, unitDBBean.afterUnit ?: "")
                        }
                    }
                    else -> {
                        return arrayOf(
                            getResult(numericalValue.toDouble() * (unitDBBean.calcFactor?.toDoubleOrNull() ?: 1.0)).toString(),
                            unitDBBean.afterUnit ?: ""
                        )
                    }
                }
            } else {
                if (preUnit.equals(unitDBBean.afterUnit ?: "", ignoreCase = true)) {
                    return arrayOf(numericalValue, unitDBBean.afterUnit ?: "")
                }
                
                when {
                    preUnit.equals("K", ignoreCase = true) -> {
                        return try {
                            arrayOf(
                                getResult(32 + (numericalValue.toDouble() - 273.15) * 1.8).toString(),
                                unitDBBean.afterUnit ?: ""
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            arrayOf(numericalValue, unitDBBean.afterUnit ?: "")
                        }
                    }
                    preUnit.equals("deg.C", ignoreCase = true) -> {
                        return try {
                            arrayOf(
                                getResult(32 + numericalValue.toDouble() * 1.8).toString(),
                                "°F"
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            arrayOf(numericalValue, unitDBBean.afterUnit ?: "")
                        }
                    }
                    else -> {
                        return arrayOf(
                            getResult(numericalValue.toDouble() * (unitDBBean.calcFactor?.toDoubleOrNull() ?: 1.0)).toString(),
                            unitDBBean.afterUnit ?: ""
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return if (unitDBBean == null) {
            arrayOf(numericalValue, preUnit)
        } else {
            arrayOf(numericalValue, unitDBBean.afterUnit ?: "")
        }
    }
    
    @JvmStatic
    fun getResult(dou: Double): Double {
        val bigDecimal = BigDecimal(dou).setScale(2, RoundingMode.HALF_UP)
        return bigDecimal.toDouble()
    }
    
    @JvmStatic
    fun getDecimalFormatByDouble(score: Double): String {
        val decimalFormat = DecimalFormat("0.00#")
        return decimalFormat.format(score)
    }
}