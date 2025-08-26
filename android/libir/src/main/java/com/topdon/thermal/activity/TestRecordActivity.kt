package com.topdon.thermal.activity

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Camera
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.topdon.thermal.R
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.*

class TestRecordActivity : Activity() {
    
    companion object {
        private const val CLASS_LABEL = "RecordActivity"
        private const val LOG_TAG = CLASS_LABEL
        private const val RECORD_LENGTH = 60 * 60
    }
    
    private var mWakeLock: PowerManager.WakeLock? = null
    private lateinit var ffmpegLink: String
    private var startTime: Long = 0
    private var recording = false
    private var recorder: FFmpegFrameRecorder? = null
    private var isPreviewOn = false
    
    private val sampleAudioRateInHz = 44100
    private var imageWidth = 320
    private var imageHeight = 240
    private val frameRate = 30
    
    private var audioRecord: AudioRecord? = null
    private var audioRecordRunnable: AudioRecordRunnable? = null
    private var audioThread: Thread? = null
    @Volatile private var runAudioThread = true
    
    private var cameraDevice: Camera? = null
    private var cameraView: CameraView? = null
    private var yuvImage: Frame? = null
    
    private val bgScreenBx = 232
    private val bgScreenBy = 128
    private val bgScreenWidth = 700
    private val bgScreenHeight = 500
    private val bgWidth = 1123
    private val bgHeight = 715
    private val liveWidth = 640
    private val liveHeight = 480
    
    private var screenWidth = 0
    private var screenHeight = 0
    private lateinit var btnRecorderControl: Button
    
    private lateinit var images: Array<Frame>
    private lateinit var timestamps: LongArray
    private lateinit var samples: Array<ShortBuffer>
    private var imagesIndex = 0
    private var samplesIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_record_test)
        
        ffmpegLink = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).absolutePath}/${Date().time}.mp4"
        
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, CLASS_LABEL)
        mWakeLock?.acquire()
        
        initLayout()
    }
    
    override fun onResume() {
        super.onResume()
        
        if (mWakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, CLASS_LABEL)
            mWakeLock?.acquire()
        }
    }
    
    override fun onPause() {
        super.onPause()
        mWakeLock?.release()
        mWakeLock = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        recording = false
        
        cameraView?.stopPreview()
        
        cameraDevice?.apply {
            stopPreview()
            release()
        }
        cameraDevice = null
        
        mWakeLock?.release()
        mWakeLock = null
    }
    
    private fun initLayout() {
        val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        screenWidth = display.width
        screenHeight = display.height
        
        val myInflate = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val topLayout = RelativeLayout(this)
        setContentView(topLayout)
        val preViewLayout = myInflate.inflate(R.layout.activity_record_test, null) as LinearLayout
        val layoutParam = RelativeLayout.LayoutParams(screenWidth, screenHeight)
        topLayout.addView(preViewLayout, layoutParam)
        
        btnRecorderControl = findViewById(R.id.recorder_control)
        btnRecorderControl.text = "Start"
        btnRecorderControl.setOnClickListener {
            if (!recording) {
                startRecording()
                Log.w(LOG_TAG, "Start Button Pushed")
                btnRecorderControl.text = "Stop"
            } else {
                stopRecording()
                Log.w(LOG_TAG, "Stop Button Pushed")
                btnRecorderControl.text = "Start"
            }
        }
        
        val displayWidthD = (1.0 * bgScreenWidth * screenWidth / bgWidth).toInt()
        val displayHeightD = (1.0 * bgScreenHeight * screenHeight / bgHeight).toInt()
        
        val (prevRw, prevRh) = if (1.0 * displayWidthD / displayHeightD > 1.0 * liveWidth / liveHeight) {
            val rh = displayHeightD
            val rw = (1.0 * displayHeightD * liveWidth / liveHeight).toInt()
            Pair(rw, rh)
        } else {
            val rw = displayWidthD
            val rh = (1.0 * displayWidthD * liveHeight / liveWidth).toInt()
            Pair(rw, rh)
        }
        
        val cameraLayoutParam = RelativeLayout.LayoutParams(prevRw, prevRh).apply {
            topMargin = (1.0 * bgScreenBy * screenHeight / bgHeight).toInt()
            leftMargin = (1.0 * bgScreenBx * screenWidth / bgWidth).toInt()
        }
        
        cameraDevice = Camera.open()
        Log.i(LOG_TAG, "camera open")
        cameraView = CameraView(this, cameraDevice!!)
        topLayout.addView(cameraView, cameraLayoutParam)
        Log.i(LOG_TAG, "camera preview start: OK")
    }
    
    private fun initRecorder() {
        Log.w(LOG_TAG, "init recorder")
        
        if (RECORD_LENGTH > 0) {
            imagesIndex = 0
            images = Array(RECORD_LENGTH * frameRate) { Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2) }
            timestamps = LongArray(images.size) { -1 }
        } else if (yuvImage == null) {
            yuvImage = Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2)
            Log.i(LOG_TAG, "create yuvImage")
        }
        
        Log.i(LOG_TAG, "ffmpeg_url: $ffmpegLink")
        recorder = FFmpegFrameRecorder(ffmpegLink, imageWidth, imageHeight, 1).apply {
            format = "mp4"
            sampleRate = sampleAudioRateInHz
            setFrameRate(frameRate)
        }
        
        Log.i(LOG_TAG, "recorder initialize success")
        
        audioRecordRunnable = AudioRecordRunnable()
        audioThread = Thread(audioRecordRunnable)
        runAudioThread = true
    }
    
    private fun startRecording() {
        initRecorder()
        
        try {
            recorder?.start()
            startTime = System.currentTimeMillis()
            recording = true
            audioThread?.start()
        } catch (e: FFmpegFrameRecorder.Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopRecording() {
        runAudioThread = false
        try {
            audioThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        audioRecordRunnable = null
        audioThread = null
        
        recorder?.let { rec ->
            if (recording) {
                if (RECORD_LENGTH > 0) {
                    Log.v(LOG_TAG, "Writing frames")
                    try {
                        var firstIndex = imagesIndex % samples.size
                        var lastIndex = (imagesIndex - 1) % images.size
                        
                        if (imagesIndex <= images.size) {
                            firstIndex = 0
                            lastIndex = imagesIndex - 1
                        }
                        
                        val recordStartTime = timestamps[lastIndex] - RECORD_LENGTH * 1000000L
                        startTime = if (recordStartTime < 0) 0 else recordStartTime
                        
                        if (lastIndex < firstIndex) {
                            lastIndex += images.size
                        }
                        
                        for (i in firstIndex..lastIndex) {
                            val t = timestamps[i % timestamps.size] - startTime
                            if (t >= 0) {
                                if (t > rec.timestamp) {
                                    rec.timestamp = t
                                }
                                rec.record(images[i % images.size])
                            }
                        }
                        
                        firstIndex = samplesIndex % samples.size
                        lastIndex = (samplesIndex - 1) % samples.size
                        if (samplesIndex <= samples.size) {
                            firstIndex = 0
                            lastIndex = samplesIndex - 1
                        }
                        if (lastIndex < firstIndex) {
                            lastIndex += samples.size
                        }
                        for (i in firstIndex..lastIndex) {
                            rec.recordSamples(samples[i % samples.size])
                        }
                    } catch (e: FFmpegFrameRecorder.Exception) {
                        Log.v(LOG_TAG, e.message ?: "Unknown error")
                        e.printStackTrace()
                    }
                }
                
                recording = false
                Log.v(LOG_TAG, "Finishing recording, calling stop and release on recorder")
                try {
                    rec.stop()
                    rec.release()
                } catch (e: FFmpegFrameRecorder.Exception) {
                    e.printStackTrace()
                }
                recorder = null
            }
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (recording) {
                stopRecording()
            }
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    
    inner class AudioRecordRunnable : Runnable {
        override fun run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleAudioRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleAudioRateInHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            val audioData: ShortBuffer = if (RECORD_LENGTH > 0) {
                samplesIndex = 0
                samples = Array(RECORD_LENGTH * sampleAudioRateInHz * 2 / bufferSize + 1) {
                    ShortBuffer.allocate(bufferSize)
                }
                samples[0] // Will be reassigned in loop
            } else {
                ShortBuffer.allocate(bufferSize)
            }
            
            Log.d(LOG_TAG, "audioRecord.startRecording()")
            audioRecord?.startRecording()
            
            var currentAudioData = audioData
            
            while (runAudioThread) {
                if (RECORD_LENGTH > 0) {
                    currentAudioData = samples[samplesIndex++ % samples.size]
                    currentAudioData.position(0).limit(0)
                }
                
                val bufferReadResult = audioRecord?.read(
                    currentAudioData.array(),
                    0,
                    currentAudioData.capacity()
                ) ?: 0
                
                currentAudioData.limit(bufferReadResult)
                if (bufferReadResult > 0) {
                    Log.v(LOG_TAG, "bufferReadResult: $bufferReadResult")
                    
                    if (recording && RECORD_LENGTH <= 0) {
                        try {
                            recorder?.recordSamples(currentAudioData)
                        } catch (e: FFmpegFrameRecorder.Exception) {
                            Log.v(LOG_TAG, e.message ?: "Unknown error")
                            e.printStackTrace()
                        }
                    }
                }
            }
            
            Log.v(LOG_TAG, "AudioThread Finished, release audioRecord")
            audioRecord?.apply {
                stop()
                release()
            }
            audioRecord = null
            Log.v(LOG_TAG, "audioRecord released")
        }
    }
    
    inner class CameraView(context: Context, private val mCamera: Camera) : 
        SurfaceView(context), SurfaceHolder.Callback, Camera.PreviewCallback {
        
        private val mHolder: SurfaceHolder = holder
        
        init {
            Log.w("camera", "camera view")
            mHolder.addCallback(this)
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
            mCamera.setPreviewCallback(this)
        }
        
        override fun surfaceCreated(holder: SurfaceHolder) {
            try {
                stopPreview()
                mCamera.setPreviewDisplay(holder)
            } catch (exception: IOException) {
                mCamera.release()
            }
        }
        
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            val camParams = mCamera.parameters
            val sizes = camParams.supportedPreviewSizes
            
            sizes.sortWith { a, b -> a.width * a.height - b.width * b.height }
            
            sizes.forEachIndexed { i, size ->
                if ((size.width >= imageWidth && size.height >= imageHeight) || i == sizes.size - 1) {
                    imageWidth = size.width
                    imageHeight = size.height
                    Log.v(LOG_TAG, "Changed to supported resolution: ${imageWidth}x$imageHeight")
                    return@forEachIndexed
                }
            }
            
            camParams.setPreviewSize(imageWidth, imageHeight)
            Log.v(LOG_TAG, "Setting imageWidth: $imageWidth imageHeight: $imageHeight frameRate: $frameRate")
            
            camParams.previewFrameRate = frameRate
            Log.v(LOG_TAG, "Preview Framerate: ${camParams.previewFrameRate}")
            
            mCamera.parameters = camParams
            startPreview()
        }
        
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            try {
                mHolder.addCallback(null)
                mCamera.setPreviewCallback(null)
            } catch (e: RuntimeException) {
                // Ignore
            }
        }
        
        fun startPreview() {
            if (!isPreviewOn) {
                isPreviewOn = true
                mCamera.startPreview()
            }
        }
        
        fun stopPreview() {
            if (isPreviewOn) {
                isPreviewOn = false
                mCamera.stopPreview()
            }
        }
        
        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                startTime = System.currentTimeMillis()
                return
            }
            
            if (RECORD_LENGTH > 0) {
                val i = imagesIndex++ % images.size
                yuvImage = images[i]
                timestamps[i] = 1000 * (System.currentTimeMillis() - startTime)
            }
            
            yuvImage?.let { yuv ->
                if (recording) {
                    (yuv.image[0] as ByteBuffer).position(0).put(data)
                    
                    if (RECORD_LENGTH <= 0) {
                        try {
                            Log.v(LOG_TAG, "Writing Frame")
                            val t = 1000 * (System.currentTimeMillis() - startTime)
                            recorder?.let { rec ->
                                if (t > rec.timestamp) {
                                    rec.timestamp = t
                                }
                                rec.record(yuv)
                            }
                        } catch (e: FFmpegFrameRecorder.Exception) {
                            Log.v(LOG_TAG, e.message ?: "Unknown error")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}