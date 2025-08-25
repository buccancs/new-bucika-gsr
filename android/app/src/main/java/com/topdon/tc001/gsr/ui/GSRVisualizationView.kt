package com.topdon.tc001.gsr.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.shimmerresearch.driver.ProcessedGSRData
import kotlin.math.max
import kotlin.math.min

/**
 * Real-time GSR data visualization component
 * Displays GSR conductance, temperature, and signal quality in real-time
 */
class GSRVisualizationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        private const val MAX_DATA_POINTS = 512 // 4 seconds at 128 Hz
        private const val GRID_LINES = 5
    }
    
    // Data storage
    private val gsrDataPoints = mutableListOf<Float>()
    private val temperatureDataPoints = mutableListOf<Float>()
    private val qualityDataPoints = mutableListOf<Float>()
    private val timestamps = mutableListOf<Long>()
    
    // Paint objects for rendering
    private val gsrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }
    
    private val temperaturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    
    private val qualityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 32f
    }
    
    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    
    // Paths for drawing lines
    private val gsrPath = Path()
    private val temperaturePath = Path()
    private val qualityPath = Path()
    
    // Value ranges
    private var gsrMin = 0f
    private var gsrMax = 20f
    private var tempMin = 30f
    private var tempMax = 35f
    private var currentGSR = 0f
    private var currentTemp = 0f
    private var currentQuality = 0f
    
    /**
     * Add new GSR data point
     */
    fun addDataPoint(processedData: ProcessedGSRData) {
        synchronized(this) {
            val gsrValue = processedData.filteredGSR.toFloat()
            val tempValue = processedData.filteredTemperature.toFloat()
            val qualityValue = processedData.signalQuality.toFloat()
            
            // Add data points
            gsrDataPoints.add(gsrValue)
            temperatureDataPoints.add(tempValue)
            qualityDataPoints.add(qualityValue)
            timestamps.add(processedData.timestamp)
            
            // Update current values
            currentGSR = gsrValue
            currentTemp = tempValue
            currentQuality = qualityValue
            
            // Update ranges dynamically
            updateRanges(gsrValue, tempValue)
            
            // Maintain buffer size
            if (gsrDataPoints.size > MAX_DATA_POINTS) {
                gsrDataPoints.removeAt(0)
                temperatureDataPoints.removeAt(0)
                qualityDataPoints.removeAt(0)
                timestamps.removeAt(0)
            }
        }
        
        // Trigger redraw
        post { invalidate() }
    }
    
    /**
     * Update value ranges for auto-scaling
     */
    private fun updateRanges(gsrValue: Float, tempValue: Float) {
        // Update GSR range with some padding
        gsrMin = min(gsrMin, gsrValue - 1f)
        gsrMax = max(gsrMax, gsrValue + 1f)
        
        // Update temperature range with some padding
        tempMin = min(tempMin, tempValue - 0.5f)
        tempMax = max(tempMax, tempValue + 0.5f)
        
        // Ensure minimum range
        if (gsrMax - gsrMin < 5f) {
            val center = (gsrMax + gsrMin) / 2f
            gsrMin = center - 2.5f
            gsrMax = center + 2.5f
        }
        
        if (tempMax - tempMin < 2f) {
            val center = (tempMax + tempMin) / 2f
            tempMin = center - 1f
            tempMax = center + 1f
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (gsrDataPoints.isEmpty()) {
            drawEmptyState(canvas)
            return
        }
        
        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // Calculate drawing dimensions
        val chartHeight = height * 0.75f
        val infoHeight = height * 0.25f
        val padding = 40f
        val chartWidth = width - 2 * padding
        
        // Draw grid
        drawGrid(canvas, padding, chartHeight, chartWidth)
        
        // Draw data lines
        drawDataLines(canvas, padding, chartHeight, chartWidth)
        
        // Draw current values
        drawCurrentValues(canvas, chartHeight, infoHeight)
        
        // Draw legend
        drawLegend(canvas, padding)
    }
    
    /**
     * Draw empty state message
     */
    private fun drawEmptyState(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        val message = "Waiting for GSR data..."
        val textWidth = textPaint.measureText(message)
        val x = (width - textWidth) / 2f
        val y = height / 2f
        
        canvas.drawText(message, x, y, textPaint)
    }
    
    /**
     * Draw background grid
     */
    private fun drawGrid(canvas: Canvas, padding: Float, chartHeight: Float, chartWidth: Float) {
        // Horizontal grid lines
        for (i in 0..GRID_LINES) {
            val y = padding + (chartHeight - 2 * padding) * i / GRID_LINES
            canvas.drawLine(padding, y, padding + chartWidth, y, gridPaint)
        }
        
        // Vertical grid lines
        for (i in 0..GRID_LINES) {
            val x = padding + chartWidth * i / GRID_LINES
            canvas.drawLine(x, padding, x, chartHeight - padding, gridPaint)
        }
    }
    
    /**
     * Draw GSR and temperature data lines
     */
    private fun drawDataLines(canvas: Canvas, padding: Float, chartHeight: Float, chartWidth: Float) {
        if (gsrDataPoints.size < 2) return
        
        val drawingHeight = chartHeight - 2 * padding
        val pointSpacing = chartWidth / (MAX_DATA_POINTS - 1)
        
        // Reset paths
        gsrPath.reset()
        temperaturePath.reset()
        qualityPath.reset()
        
        synchronized(this) {
            // Draw GSR data (primary chart)
            for (i in gsrDataPoints.indices) {
                val x = padding + i * pointSpacing
                val gsrNormalized = (gsrDataPoints[i] - gsrMin) / (gsrMax - gsrMin)
                val gsrY = padding + drawingHeight * (1f - gsrNormalized)
                
                if (i == 0) {
                    gsrPath.moveTo(x, gsrY)
                } else {
                    gsrPath.lineTo(x, gsrY)
                }
            }
            
            // Draw temperature data (secondary chart, scaled differently)
            for (i in temperatureDataPoints.indices) {
                val x = padding + i * pointSpacing
                val tempNormalized = (temperatureDataPoints[i] - tempMin) / (tempMax - tempMin)
                val tempY = padding + drawingHeight * 0.5f + drawingHeight * 0.3f * (1f - tempNormalized)
                
                if (i == 0) {
                    temperaturePath.moveTo(x, tempY)
                } else {
                    temperaturePath.lineTo(x, tempY)
                }
            }
            
            // Draw quality indicator (bottom section)
            for (i in qualityDataPoints.indices) {
                val x = padding + i * pointSpacing
                val qualityNormalized = qualityDataPoints[i] / 100f
                val qualityY = chartHeight - padding - drawingHeight * 0.1f * qualityNormalized
                
                if (i == 0) {
                    qualityPath.moveTo(x, qualityY)
                } else {
                    qualityPath.lineTo(x, qualityY)
                }
            }
        }
        
        // Draw the paths
        canvas.drawPath(gsrPath, gsrPaint)
        canvas.drawPath(temperaturePath, temperaturePaint)
        canvas.drawPath(qualityPath, qualityPaint)
    }
    
    /**
     * Draw current values display
     */
    private fun drawCurrentValues(canvas: Canvas, chartHeight: Float, infoHeight: Float) {
        val y = chartHeight + 30f
        val textSize = 28f
        textPaint.textSize = textSize
        
        // GSR value
        textPaint.color = Color.BLUE
        val gsrText = "GSR: %.2f µS".format(currentGSR)
        canvas.drawText(gsrText, 40f, y, textPaint)
        
        // Temperature value
        textPaint.color = Color.RED
        val tempText = "Temp: %.2f °C".format(currentTemp)
        canvas.drawText(tempText, 40f, y + textSize + 10f, textPaint)
        
        // Signal quality
        textPaint.color = Color.GREEN
        val qualityText = "Quality: %.1f%%".format(currentQuality)
        canvas.drawText(qualityText, 40f, y + 2 * (textSize + 10f), textPaint)
        
        // Data rate
        textPaint.color = Color.BLACK
        val rateText = "128 Hz"
        canvas.drawText(rateText, width - 120f, y, textPaint)
    }
    
    /**
     * Draw legend
     */
    private fun drawLegend(canvas: Canvas, padding: Float) {
        val legendY = 20f
        val lineLength = 30f
        
        // GSR legend
        canvas.drawLine(padding, legendY, padding + lineLength, legendY, gsrPaint)
        textPaint.color = Color.BLUE
        textPaint.textSize = 24f
        canvas.drawText("GSR", padding + lineLength + 10f, legendY + 8f, textPaint)
        
        // Temperature legend
        val tempX = padding + 100f
        canvas.drawLine(tempX, legendY, tempX + lineLength, legendY, temperaturePaint)
        textPaint.color = Color.RED
        canvas.drawText("Temp", tempX + lineLength + 10f, legendY + 8f, textPaint)
        
        // Quality legend
        val qualityX = padding + 200f
        canvas.drawLine(qualityX, legendY, qualityX + lineLength, legendY, qualityPaint)
        textPaint.color = Color.GREEN
        canvas.drawText("Quality", qualityX + lineLength + 10f, legendY + 8f, textPaint)
    }
    
    /**
     * Clear all data points
     */
    fun clearData() {
        synchronized(this) {
            gsrDataPoints.clear()
            temperatureDataPoints.clear()
            qualityDataPoints.clear()
            timestamps.clear()
        }
        
        post { invalidate() }
    }
    
    /**
     * Get current data point count
     */
    fun getDataPointCount(): Int = gsrDataPoints.size
    
    /**
     * Export current data as CSV string
     */
    fun exportDataAsCSV(): String {
        val sb = StringBuilder()
        sb.appendLine("Timestamp,GSR_µS,Temperature_°C,Quality_%")
        
        synchronized(this) {
            for (i in gsrDataPoints.indices) {
                if (i < timestamps.size) {
                    sb.appendLine("${timestamps[i]},${gsrDataPoints[i]},${temperatureDataPoints[i]},${qualityDataPoints[i]}")
                }
            }
        }
        
        return sb.toString()
    }
