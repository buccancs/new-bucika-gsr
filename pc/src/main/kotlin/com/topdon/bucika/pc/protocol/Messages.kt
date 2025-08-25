package com.topdon.bucika.pc.protocol

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Standard message envelope for WebSocket communication
 */
data class MessageEnvelope(
    val id: String,
    val type: MessageType,
    val ts: Long, // nanoseconds since Unix epoch
    val sessionId: String?,
    val deviceId: String,
    val payload: MessagePayload
)

enum class MessageType {
    HELLO, REGISTER, PING, PONG, START, STOP, SYNC_MARK, 
    ACK, ERROR, GSR_SAMPLE, UPLOAD_BEGIN, UPLOAD_CHUNK, UPLOAD_END
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(HelloPayload::class, name = "HELLO"),
    JsonSubTypes.Type(RegisterPayload::class, name = "REGISTER"),
    JsonSubTypes.Type(EmptyPayload::class, name = "PING"),
    JsonSubTypes.Type(EmptyPayload::class, name = "PONG"),
    JsonSubTypes.Type(StartPayload::class, name = "START"),
    JsonSubTypes.Type(EmptyPayload::class, name = "STOP"),
    JsonSubTypes.Type(SyncMarkPayload::class, name = "SYNC_MARK"),
    JsonSubTypes.Type(AckPayload::class, name = "ACK"),
    JsonSubTypes.Type(ErrorPayload::class, name = "ERROR"),
    JsonSubTypes.Type(GSRSamplePayload::class, name = "GSR_SAMPLE"),
    JsonSubTypes.Type(UploadBeginPayload::class, name = "UPLOAD_BEGIN"),
    JsonSubTypes.Type(UploadChunkPayload::class, name = "UPLOAD_CHUNK"),
    JsonSubTypes.Type(UploadEndPayload::class, name = "UPLOAD_END")
)
abstract class MessagePayload

// Payload implementations
object EmptyPayload : MessagePayload()

data class HelloPayload(
    val deviceName: String,
    val capabilities: List<String>,
    val batteryLevel: Int,
    val version: String
) : MessagePayload()

data class RegisterPayload(
    val registered: Boolean,
    val role: String,
    val syncConfig: SyncConfig
) : MessagePayload()

data class SyncConfig(
    val syncInterval: Long, // milliseconds
    val offsetThreshold: Long // nanoseconds
)

data class StartPayload(
    val sessionConfig: SessionConfig
) : MessagePayload()

data class SessionConfig(
    val videoConfig: VideoConfig,
    val gsrConfig: GSRConfig
)

data class VideoConfig(
    val resolution: String,
    val fps: Int,
    val bitrate: Int
)

data class GSRConfig(
    val sampleRate: Int,
    val channels: List<String>
)

data class SyncMarkPayload(
    val markerId: String,
    val referenceTime: Long
) : MessagePayload()

data class GSRSamplePayload(
    val samples: List<GSRSample>
) : MessagePayload()

data class GSRSample(
    val t_mono_ns: Long,
    val t_utc_ns: Long,
    val offset_ms: Double,
    val seq: Long,
    val gsr_raw_uS: Double,
    val gsr_filt_uS: Double,
    val temp_C: Double,
    val flag_spike: Boolean,
    val flag_sat: Boolean,
    val flag_dropout: Boolean
)

data class UploadBeginPayload(
    val fileName: String,
    val fileSize: Long,
    val checksum: String,
    val chunkSize: Int
) : MessagePayload()

data class UploadChunkPayload(
    val fileName: String,
    val chunkIndex: Int,
    val data: String, // base64 encoded
    val checksum: String
) : MessagePayload()

data class UploadEndPayload(
    val fileName: String,
    val totalChunks: Int,
    val finalChecksum: String
) : MessagePayload()

data class AckPayload(
    val ackId: String,
    val status: String
) : MessagePayload()

data class ErrorPayload(
    val errorCode: String,
    val message: String,
    val details: Map<String, Any>? = null
) : MessagePayload()