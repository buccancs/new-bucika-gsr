package com.topdon.thermal.video

abstract class VideoRecord {

    abstract fun startRecord()
    abstract fun startRecord(fileDir : String)

    abstract fun stopRecord()

    abstract fun updateAudioState(audioRecord: Boolean);
