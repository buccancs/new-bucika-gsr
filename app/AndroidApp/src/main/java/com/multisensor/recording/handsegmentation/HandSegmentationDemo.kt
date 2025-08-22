package com.multisensor.recording.handsegmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.runBlocking

class HandSegmentationDemo(private val context: Context) {

    companion object {
        private const val TAG = "HandSegmentationDemo"
    }

    fun runDemo(): Boolean {
        Log.i(TAG, "Starting hand segmentation demo")

        return try {
            val engine = HandSegmentationEngine(context)

            if (!engine.initialize()) {
                Log.e(TAG, "Failed to initialize hand segmentation engine")
                return false
            }

            val testBitmap = createTestHandBitmap()

            val result = runBlocking {
                engine.processFrame(testBitmap)
            }

            Log.i(TAG, "Demo processing result:")
            Log.i(TAG, "- Detected hands: ${result.detectedHands.size}")
            Log.i(TAG, "- Processing time: ${result.processingTimeMs}ms")
            Log.i(TAG, "- Mask bitmap created: ${result.maskBitmap != null}")
            Log.i(TAG, "- Processed bitmap created: ${result.processedBitmap != null}")

            val stats = engine.getDatasetStats()
            Log.i(TAG, "Dataset stats: $stats")

            val datasetDir = engine.saveCroppedDataset("demo_test")
            if (datasetDir != null) {
                Log.i(TAG, "Dataset saved successfully to: ${datasetDir.absolutePath}")
            }

            engine.cleanup()

            Log.i(TAG, "Hand segmentation demo completed successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Hand segmentation demo failed", e)
            false
        }
    }

    private fun createTestHandBitmap(): Bitmap {
        val width = 640
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        bitmap.eraseColor(Color.rgb(200, 200, 200))

        val skinColor = Color.rgb(220, 180, 140)

        for (y in 150..350) {
            for (x in 200..400) {
                if (x in 200..350 && y in 200..350) {
                    bitmap.setPixel(x, y, skinColor)
                }
                if (x in 220..240 && y in 150..200) bitmap.setPixel(x, y, skinColor)
                if (x in 250..270 && y in 140..200) bitmap.setPixel(x, y, skinColor)
                if (x in 280..300 && y in 150..200) bitmap.setPixel(x, y, skinColor)
                if (x in 310..330 && y in 160..200) bitmap.setPixel(x, y, skinColor)
                if (x in 180..220 && y in 240..280) bitmap.setPixel(x, y, skinColor)
            }
        }

        return bitmap
    }
}
