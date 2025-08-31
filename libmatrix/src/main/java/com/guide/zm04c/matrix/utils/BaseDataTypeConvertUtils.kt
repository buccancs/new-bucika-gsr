package com.guide.zm04c.matrix.utils

import com.guide.zm04c.matrix.Logger
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.experimental.and

/**
 * created by liuhongwei gd02527 on 2018年07月27日
 */
class BaseDataTypeConvertUtils private constructor() {

    companion object {
        private val TAG = BaseDataTypeConvertUtils::class.java.simpleName
        private var df: DecimalFormat ?= null

        fun convertShort2LittleEndianByteArr(value: Short): ByteArray {
            val shortByteArray = ByteArray(2)
            shortByteArray[0] = (value and 0xff).toByte()
            shortByteArray[1] = (value.toInt() ushr 8 and 0xff).toByte()
            return shortByteArray
        }

        fun convertShort2BigEndianByteArr(value: Short): ByteArray {
            val shortByteArray = ByteArray(2)
            shortByteArray[0] = (value.toInt() ushr 8 and 0xff).toByte()
            shortByteArray[1] = (value and 0xff).toByte()
            return shortByteArray
        }

        private fun convertShortArr2ByteArr(valueArr: ShortArray, convert: (Short) -> ByteArray): ByteArray {
            val valueByteArr = ByteArray(valueArr.size * 2)
            for (i in 0 until valueArr.size) {
                val dat = convert(valueArr[i])
                valueByteArr[2 * i] = dat[0]
                valueByteArr[2 * i + 1] = dat[1]
            }
            return valueByteArr
        }

        fun convertShortArr2LittleEndianByteArr(valueArr: ShortArray): ByteArray {
            return convertShortArr2ByteArr(valueArr, ::convertShort2LittleEndianByteArr)
        }

        fun convertShortArr2BigEndianByteArr(valueArr: ShortArray): ByteArray {
            return convertShortArr2ByteArr(valueArr, ::convertShort2BigEndianByteArr)
        }

        fun byteArr2HexString(src: ByteArray): String? {
            val stringBuilder = StringBuilder()
            for (i in src.indices) {
                val b = src[i].toInt() and 0xFF
                val h = Integer.toHexString(b)
                if (h.length < 2) {
                    stringBuilder.append(0)
                }
                stringBuilder.append(h)
            }
            return stringBuilder.toString()
        }

        fun convertFloatWith2Decimals(value: Float): Float {
            return (value * 100).toInt() / 100.0f
        }

        /**
         * 将float格式化为只带有一位小数的字符串
         *
         * @param number
         * @return
         */
        fun float2StrWithOneDecimal(number: Float): String {
            try {
                val pattern = "0.0"
                if(df == null) {
                    val enlocale = Locale("en", "US")
                    df = NumberFormat.getNumberInstance(enlocale) as DecimalFormat
                }
                df!!.applyPattern(pattern)
                return float2Str(number, df!!)
            } catch (e: Exception) {
                val newNumber = Math.round(number * 10) / 10f
                val str = newNumber.toString()
                Logger.e(TAG,"float2StrWithOneDecimal number = " + number + " str = " + str);
                return str;
            }
        }

        /**
         * 将float格式化为只带有一位小数的字符串
         *
         * @param number
         * @return
         */
        fun float2StrWithTwoDecimal(number: Float): String {
            try {
                val pattern = "0.00"
                if(df == null) {
                    val enlocale = Locale("en", "US")
                    df = NumberFormat.getNumberInstance(enlocale) as DecimalFormat
                }
                df!!.applyPattern(pattern)
                return float2Str(number, df!!)
            } catch (e: Exception) {
                val newNumber = Math.round(number * 100) / 100f
                val str = newNumber.toString()
                Logger.e(TAG,"float2StrWithTwoDecimal number = " + number + " str = " + str);
                return str;
            }
        }

        /**
         * 将float格式化为字符串
         *
         * @param number 需要格式化的float字符串
         * @param df     DecimalFormat
         * @return
         */
        fun float2Str(number: Float, df: DecimalFormat): String {
            return df.format(number.toDouble())
        }
    }

    init {
        throw AssertionError("cannot be instantiated")
    }

}