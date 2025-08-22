package com.multisensor.recording.ui.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.multisensor.recording.util.Logger
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@ActivityScoped
class PreviewManager @Inject constructor(
    private val logger: Logger
) : LifecycleEventObserver {

    private var rgbPreviewJob: Job? = null
    private var thermalPreviewJob: Job? = null
    private var isRgbPreviewActive = false
    private var isThermalPreviewActive = false

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    companion object {
        private const val RGB_PREVIEW_DELAY = 100L
        private const val THERMAL_PREVIEW_DELAY = 200L
        private const val PREVIEW_WIDTH = 320
        private const val PREVIEW_HEIGHT = 240
    }

    fun startRgbPreview(
        scope: CoroutineScope,
        imageView: ImageView,
        onPreviewUpdate: (isActive: Boolean) -> Unit = {}
    ) {
        if (isRgbPreviewActive) return

        isRgbPreviewActive = true
        onPreviewUpdate(true)
        logger.info("Starting RGB preview")

        rgbPreviewJob = scope.launch {
            while (isActive && isRgbPreviewActive) {
                try {
                    val bitmap = generateRgbPreviewBitmap()
                    imageView.post { imageView.setImageBitmap(bitmap) }
                    delay(RGB_PREVIEW_DELAY)
                } catch (e: Exception) {
                    logger.error("Error updating RGB preview", e)
                    break
                }
            }
        }
    }

    fun stopRgbPreview(onPreviewUpdate: (isActive: Boolean) -> Unit = {}) {
        if (!isRgbPreviewActive) return

        isRgbPreviewActive = false
        rgbPreviewJob?.cancel()
        rgbPreviewJob = null
        onPreviewUpdate(false)
        logger.info("Stopped RGB preview")
    }

    fun startThermalPreview(
        scope: CoroutineScope,
        imageView: ImageView,
        onPreviewUpdate: (isActive: Boolean) -> Unit = {}
    ) {
        if (isThermalPreviewActive) return

        isThermalPreviewActive = true
        onPreviewUpdate(true)
        logger.info("Starting thermal preview")

        thermalPreviewJob = scope.launch {
            while (isActive && isThermalPreviewActive) {
                try {
                    val bitmap = generateThermalPreviewBitmap()
                    imageView.post { imageView.setImageBitmap(bitmap) }
                    delay(THERMAL_PREVIEW_DELAY)
                } catch (e: Exception) {
                    logger.error("Error updating thermal preview", e)
                    break
                }
            }
        }
    }

    fun stopThermalPreview(onPreviewUpdate: (isActive: Boolean) -> Unit = {}) {
        if (!isThermalPreviewActive) return

        isThermalPreviewActive = false
        thermalPreviewJob?.cancel()
        thermalPreviewJob = null
        onPreviewUpdate(false)
        logger.info("Stopped thermal preview")
    }

    fun stopAllPreviews() {
        stopRgbPreview()
        stopThermalPreview()
    }

    fun isRgbPreviewActive(): Boolean = isRgbPreviewActive

    fun isThermalPreviewActive(): Boolean = isThermalPreviewActive

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> onPause()
            else -> {}
        }
    }

    private fun onPause() {
        stopAllPreviews()
        logger.info("PreviewManager paused - stopped all previews")
    }

    private fun generateRgbPreviewBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val time = System.currentTimeMillis() / 100

        paint.color = Color.rgb(50, 50, 80)
        canvas.drawRect(0f, 0f, PREVIEW_WIDTH.toFloat(), PREVIEW_HEIGHT.toFloat(), paint)

        for (i in 0..10) {
            val x = ((time + i * 30) % (PREVIEW_WIDTH + 100)).toFloat() - 50
            val y = (PREVIEW_HEIGHT * 0.2f + i * PREVIEW_HEIGHT * 0.05f).toFloat()
            paint.color = Color.rgb(
                (100 + i * 15) % 255,
                (150 + i * 10) % 255,
                (200 + i * 5) % 255
            )
            canvas.drawCircle(x, y, 15f, paint)
        }

        paint.color = Color.RED
        paint.textSize = 24f
        canvas.drawText("● LIVE", 10f, 30f, paint)

        paint.color = Color.WHITE
        paint.textSize = 16f
        val timeStr = timeFormatter.format(Date())
        canvas.drawText(timeStr, 10f, PREVIEW_HEIGHT - 10f, paint)

        return bitmap
    }

    private fun generateThermalPreviewBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(PREVIEW_WIDTH, PREVIEW_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val time = System.currentTimeMillis() / 200.0

        for (y in 0 until PREVIEW_HEIGHT) {
            for (x in 0 until PREVIEW_WIDTH) {
                val centerX = PREVIEW_WIDTH / 2.0
                val centerY = PREVIEW_HEIGHT / 2.0
                val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
                val intensity = (127 + 127 * sin(distance * 0.1 + time)).toInt().coerceIn(0, 255)

                val color = when {
                    intensity < 85 -> Color.rgb(0, 0, intensity * 3)
                    intensity < 170 -> Color.rgb((intensity - 85) * 3, 0, 255 - (intensity - 85) * 2)
                    else -> Color.rgb(255, (intensity - 170) * 3, 0)
                }
                paint.color = color
                canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
            }
        }

        paint.color = Color.YELLOW
        for (i in 0..3) {
            val hotX = Random.nextInt(50, PREVIEW_WIDTH - 50).toFloat()
            val hotY = Random.nextInt(50, PREVIEW_HEIGHT - 50).toFloat()
            canvas.drawCircle(hotX, hotY, 20f, paint)
        }

        paint.color = Color.WHITE
        paint.textSize = 18f
        canvas.drawText("🌡️ THERMAL", 10f, 30f, paint)

        val temp = (20 + Random.nextFloat() * 15).toInt()
        paint.textSize = 14f
        canvas.drawText("${temp}°C", 10f, PREVIEW_HEIGHT - 10f, paint)

        return bitmap
    }
}