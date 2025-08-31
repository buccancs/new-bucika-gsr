package com.topdon.libcom

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.UriUtils
import com.blankj.utilcode.util.Utils
import com.topdon.lib.core.R
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.config.FileConfig
import com.topdon.lib.core.db.entity.ThermalEntity
import com.topdon.lib.core.tools.TimeTool
import com.topdon.lib.core.tools.UnitTools
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ExcelUtil {

    @JvmStatic
    @NonNull
    private fun getTemperature(index: Int, @NonNull norTempData: ByteArray, isShowC: Boolean): String {
        val tempValue = (norTempData[2 * index + 1].toInt() shl 8 and 0xff00) or (norTempData[2 * index].toInt() and 0xff)
        val value = tempValue / 64f - 273.15f
        return UnitTools.showC(value, isShowC)
    }

    @JvmStatic
    @Nullable
    fun exportExcel(@NonNull name: String, width: Int, height: Int, @NonNull norTempData: ByteArray, @Nullable callback: Callback?): String? {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet()
        val isShowC = SharedManager.getTemperature() == 1
        val cellStyle = workbook.createCellStyle().apply {
            setAlignment(HorizontalAlignment.CENTER)
            setVerticalAlignment(VerticalAlignment.CENTER)
        }

        for (i in 0 until height) {
            val row = sheet.createRow(i)
            for (j in 0 until width) {
                val index = i * width + j
                sheet.setColumnWidth(j, 9 * width)
                val cell = row.createCell(j)
                cell.cellStyle = cellStyle
                cell.setCellValue(getTemperature(index, norTempData, isShowC))
                if (index % 100 == 0 && callback != null) {
                    callback.onOneCell(index / 100, width * height / 100)
                }
            }
        }

        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val excel = File(FileConfig.excelDir, "$name.xlsx")
                FileOutputStream(excel).use { fos ->
                    workbook.write(fos)
                    fos.flush()
                }
                excel.absolutePath
            } else {
                val fileName = "$name.xlsx"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, FileConfig.excelDir)
                }
                val contentUri = MediaStore.Files.getContentUri("external")
                val uri = Utils.getApp().contentResolver.insert(contentUri, values)
                uri?.let {
                    Utils.getApp().contentResolver.openOutputStream(it)?.use { outputStream ->
                        BufferedOutputStream(outputStream).use { bos ->
                            workbook.write(bos)
                            bos.flush()
                        }
                    }
                    Log.w("导出", UriUtils.uri2File(it).absolutePath)
                    UriUtils.uri2File(it).absolutePath
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun interface Callback {
        fun onOneCell(current: Int, total: Int)
    }

    @JvmStatic
    fun exportExcel(listData: ArrayList<ThermalEntity>, isPoint: Boolean): String? {
        val isShowC = SharedManager.getTemperature() == 1
        
        return try {
            val wb = XSSFWorkbook()
            val sheet = wb.createSheet()
            
            val title = if (isPoint) {
                arrayOf(
                    Utils.getApp().getString(R.string.detail_date),
                    Utils.getApp().getString(R.string.chart_temperature)
                )
            } else {
                arrayOf(
                    Utils.getApp().getString(R.string.detail_date),
                    Utils.getApp().getString(R.string.chart_temperature_low),
                    Utils.getApp().getString(R.string.chart_temperature_high)
                )
            }

            val row = sheet.createRow(0)
            val colNum = title.size

            val titleStyle = wb.createCellStyle().apply {
                fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                setFillPattern(FillPatternType.SOLID_FOREGROUND)
                setAlignment(HorizontalAlignment.CENTER)
                setVerticalAlignment(VerticalAlignment.CENTER)
                val font = wb.createFont().apply { bold = true }
                setFont(font)
            }

            val contentStyle = wb.createCellStyle().apply {
                setAlignment(HorizontalAlignment.CENTER)
                setVerticalAlignment(VerticalAlignment.CENTER)
            }

            for (i in 0 until colNum) {
                sheet.setColumnWidth(i, 20 * 256)
                val cell = row.createCell(i)
                cell.cellStyle = titleStyle
                cell.setCellValue(title[i])
            }

            listData.forEachIndexed { rowNum, bean ->
                val dataRow = sheet.createRow(rowNum + 1)
                dataRow.heightInPoints = 28f

                for (j in title.indices) {
                    val cell = dataRow.createCell(j)
                    
                    if (isPoint) {
                        when (j) {
                            0 -> cell.setCellValue(bean.getTime())
                            1 -> {
                                cell.cellStyle = contentStyle
                                cell.setCellValue(UnitTools.showC(bean.getMinTemp()))
                            }
                        }
                    } else {
                        when (j) {
                            0 -> cell.setCellValue(bean.getTime())
                            1 -> {
                                cell.cellStyle = contentStyle
                                cell.setCellValue(UnitTools.showC(bean.getMinTemp()))
                            }
                            2 -> {
                                cell.cellStyle = contentStyle
                                cell.setCellValue(UnitTools.showC(bean.getMaxTemp(), isShowC))
                            }
                        }
                    }
                }
            }

            val timeStr = if (listData.isEmpty()) {
                TimeTool.showDateSecond()
            } else {
                TimeUtils.millis2String(listData[0].startTime, "yyyyMMddHHmmss")
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val excel = File(FileConfig.excelDir, "TCView_$timeStr.xlsx")
                FileOutputStream(excel).use { fos ->
                    wb.write(fos)
                    fos.flush()
                }
                excel.absolutePath
            } else {
                val fileName = "TCView_$timeStr.xlsx"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, FileConfig.excelDir)
                }
                val contentUri = MediaStore.Files.getContentUri("external")
                val uri = Utils.getApp().contentResolver.insert(contentUri, values)
                uri?.let {
                    Utils.getApp().contentResolver.openOutputStream(it)?.use { outputStream ->
                        BufferedOutputStream(outputStream).use { bos ->
                            wb.write(bos)
                            bos.flush()
                        }
                    }
                    Log.w("导出", UriUtils.uri2File(it).absolutePath)
                    UriUtils.uri2File(it).absolutePath
                }
            }
        } catch (e: IOException) {
            Log.e("ExpressExcle", "exportExcel", e)
            null
        }
    }
}