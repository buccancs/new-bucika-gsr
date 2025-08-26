package com.topdon.thermal.video.media

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodec.*
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaFormat.*
import android.media.MediaMuxer
import android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
import android.os.Build
import android.util.Log
import java.io.IOException
import java.nio.ByteBuffer

class MP4Encoder : Encoder() {
    
    companion object {
        private const val BIT_RATE = 600000
        private const val FRAME_RATE = 20
        private const val I_FRAME_INTERVAL = 5
        private const val ONE_SEC = 1000000L
        private val TAG = MP4Encoder::class.java.simpleName
        private const val TIMEOUT_US = 10000
        
        private fun getPresentationTimeUsec(frameIndex: Int): Long {
            return ((frameIndex.toLong()) * ONE_SEC) / 20
        }
    }
    
    private var addedFrameCount = 0
    private var audioCodec: MediaCodec? = null
    private var audioTrackIndex = 0
    private var bufferInfo: BufferInfo? = null
    private var encodedFrameCount = 0
    private var isMuxerStarted = false
    private var isStarted = false
    private var mediaMuxer: MediaMuxer? = null
    private var trackCount = 0
    private var videoCodec: MediaCodec? = null
    private var videoTrackIndex = 0
    
    override fun onInit() {
        // Empty implementation
    }
    
    override fun onStart() {
        isStarted = true
        addedFrameCount = 0
        encodedFrameCount = 0
        val width = getWidth()
        val height = getHeight()
        
        try {
            bufferInfo = BufferInfo()
            
            // Setup video codec
            videoCodec = MediaCodec.createEncoderByType(MIMETYPE_VIDEO_AVC).apply {
                val videoFormat = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, width, height).apply {
                    setInteger(KEY_BIT_RATE, BIT_RATE)
                    setInteger(KEY_FRAME_RATE, FRAME_RATE)
                    setInteger(KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                    setInteger(KEY_COLOR_FORMAT, getColorFormat())
                }
                configure(videoFormat, null, null, CONFIGURE_FLAG_ENCODE)
                start()
            }
            
            // Setup audio codec
            audioCodec = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC).apply {
                val audioFormat = MediaFormat.createAudioFormat(MIMETYPE_AUDIO_AAC, 44100, 1).apply {
                    val profile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        MediaCodecInfo.CodecProfileLevel.MPEG2ProfileHigh
                    } else {
                        5
                    }
                    setInteger(KEY_AAC_PROFILE, profile)
                    setInteger(KEY_BIT_RATE, 65536)
                }
                configure(audioFormat, null, null, CONFIGURE_FLAG_ENCODE)
                start()
            }
            
            mediaMuxer = MediaMuxer(outputFilePath!!, MUXER_OUTPUT_MPEG_4)
        } catch (ioe: IOException) {
            throw RuntimeException("MediaMuxer creation failed", ioe)
        }
    }
    
    override fun onStop() {
        if (isStarted) {
            encode()
            if (addedFrameCount > 0) {
                Log.i(TAG, "Total frame count = $addedFrameCount")
                
                videoCodec?.let {
                    it.stop()
                    it.release()
                    videoCodec = null
                    Log.i(TAG, "RELEASE VIDEO CODEC")
                }
                
                audioCodec?.let {
                    it.stop()
                    it.release()
                    audioCodec = null
                    Log.i(TAG, "RELEASE AUDIO CODEC")
                }
                
                mediaMuxer?.let {
                    it.stop()
                    it.release()
                    mediaMuxer = null
                    Log.i(TAG, "RELEASE MUXER")
                }
            } else {
                Log.e(TAG, "not added any frame")
            }
            isStarted = false
        }
    }
    
    override fun onAddFrame(bitmap: Bitmap) {
        if (!isStarted) {
            Log.d(TAG, "already finished. can't add Frame ")
            return
        }
        
        val codec = videoCodec ?: run {
            Log.e(TAG, "Video codec is null")
            return
        }
        
        val inputBufIndex = codec.dequeueInputBuffer(TIMEOUT_US.toLong())
        if (inputBufIndex >= 0) {
            val input = EncodeYuvTools.getNV12(bitmap.width, bitmap.height, bitmap, getColorFormat())
            val inputBuffer = codec.getInputBuffer(inputBufIndex)
            inputBuffer?.let {
                it.clear()
                it.put(input)
                codec.queueInputBuffer(
                    inputBufIndex, 0, input.size,
                    getPresentationTimeUsec(addedFrameCount), 0
                )
            }
        }
        
        val audioInputBufferIndex = audioCodec?.dequeueInputBuffer(TIMEOUT_US.toLong()) ?: -1
        if (audioInputBufferIndex >= -1) {
            // Empty implementation for audio
        }
        
        addedFrameCount++
        while (addedFrameCount > encodedFrameCount) {
            encode()
        }
    }
    
    private fun encode() {
        encodeVideo()
        encodeAudio()
    }
    
    private fun encodeAudio() {
        val codec = audioCodec ?: return
        val info = bufferInfo ?: return
        
        val audioStatus = codec.dequeueOutputBuffer(info, TIMEOUT_US.toLong())
        Log.i(TAG, "Audio encoderStatus = $audioStatus, presentationTimeUs = ${info.presentationTimeUs}")
        
        when (audioStatus) {
            INFO_OUTPUT_FORMAT_CHANGED -> {
                val audioFormat = codec.outputFormat
                Log.i(TAG, "output format changed. audio format: $audioFormat")
                audioTrackIndex = mediaMuxer?.addTrack(audioFormat) ?: 0
                trackCount++
                if (trackCount == 2) {
                    Log.i(TAG, "started media muxer.")
                    mediaMuxer?.start()
                    isMuxerStarted = true
                }
            }
            INFO_TRY_AGAIN_LATER -> {
                Log.d(TAG, "no output from audio encoder available")
            }
            else -> {
                val audioData = codec.getOutputBuffer(audioStatus)
                audioData?.let { data ->
                    data.position(info.offset)
                    data.limit(info.offset + info.size)
                    if (isMuxerStarted) {
                        mediaMuxer?.writeSampleData(audioTrackIndex, data, info)
                    }
                    codec.releaseOutputBuffer(audioStatus, false)
                }
            }
        }
    }
    
    private fun encodeVideo() {
        val codec = videoCodec ?: return
        val info = bufferInfo ?: return
        
        val encoderStatus = codec.dequeueOutputBuffer(info, TIMEOUT_US.toLong())
        Log.i(TAG, "Video encoderStatus = $encoderStatus, presentationTimeUs = ${info.presentationTimeUs}")
        
        when (encoderStatus) {
            INFO_OUTPUT_FORMAT_CHANGED -> {
                val videoFormat = codec.outputFormat
                Log.i(TAG, "output format changed. video format: $videoFormat")
                videoTrackIndex = mediaMuxer?.addTrack(videoFormat) ?: 0
                trackCount++
                if (trackCount == 2) {
                    Log.i(TAG, "started media muxer.")
                    mediaMuxer?.start()
                    isMuxerStarted = true
                }
            }
            INFO_TRY_AGAIN_LATER -> {
                Log.d(TAG, "no output from video encoder available")
            }
            else -> {
                val encodedData = codec.getOutputBuffer(encoderStatus)
                if (encodedData != null) {
                    encodedData.position(info.offset)
                    encodedData.limit(info.offset + info.size)
                    if (isMuxerStarted) {
                        mediaMuxer?.writeSampleData(videoTrackIndex, encodedData, info)
                    }
                    codec.releaseOutputBuffer(encoderStatus, false)
                    encodedFrameCount++
                } else {
                    Log.i(TAG, "encoderOutputBuffer $encoderStatus was null")
                }
            }
        }
    }
    
    private fun getColorFormat(): Int {
        return if ("GOOGLE".equals(Build.BRAND, ignoreCase = true) && 
                   "PIXEL 4".equals(Build.MODEL, ignoreCase = true)) {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
        } else {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        }
    }
}