package com.multisensor.recording.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Storage service for uploading and managing sensor data files
 */
@Singleton
class FirebaseStorageService @Inject constructor(
    private val storage: FirebaseStorage
) {

    /**
     * Upload recording file to Firebase Storage
     */
    suspend fun uploadRecordingFile(
        sessionId: String,
        fileType: String, // "rgb_video", "thermal_video", "gsr_data", "calibration"
        localFile: File
    ): Result<String> {
        return try {
            val filename = "${fileType}_${System.currentTimeMillis()}_${localFile.name}"
            val storageRef = storage.reference
                .child("recording_sessions")
                .child(sessionId)
                .child(filename)

            val uploadTask = storageRef.putFile(Uri.fromFile(localFile)).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload multiple files for a session
     */
    suspend fun uploadSessionFiles(
        sessionId: String,
        files: Map<String, File> // fileType to File mapping
    ): Result<Map<String, String>> {
        return try {
            val uploadResults = mutableMapOf<String, String>()
            
            for ((fileType, file) in files) {
                val uploadResult = uploadRecordingFile(sessionId, fileType, file)
                if (uploadResult.isSuccess) {
                    uploadResults[fileType] = uploadResult.getOrThrow()
                } else {
                    return Result.failure(uploadResult.exceptionOrNull()!!)
                }
            }
            
            Result.success(uploadResults)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download file from Firebase Storage
     */
    suspend fun downloadFile(downloadUrl: String, localFile: File): Result<File> {
        return try {
            val httpsReference = storage.getReferenceFromUrl(downloadUrl)
            httpsReference.getFile(localFile).await()
            Result.success(localFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete files for a session
     */
    suspend fun deleteSessionFiles(sessionId: String): Result<Unit> {
        return try {
            val sessionRef = storage.reference
                .child("recording_sessions")
                .child(sessionId)
            
            val listResult = sessionRef.listAll().await()
            for (item in listResult.items) {
                item.delete().await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get file metadata
     */
    suspend fun getFileMetadata(downloadUrl: String): Result<Map<String, Any>> {
        return try {
            val httpsReference = storage.getReferenceFromUrl(downloadUrl)
            val metadata = httpsReference.metadata.await()
            
            val metadataMap = mapOf(
                "name" to (metadata.name ?: ""),
                "size" to metadata.sizeBytes,
                "contentType" to (metadata.contentType ?: ""),
                "timeCreated" to (metadata.creationTimeMillis),
                "updated" to (metadata.updatedTimeMillis)
            )
            
            Result.success(metadataMap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List all files for a session
     */
    suspend fun listSessionFiles(sessionId: String): Result<List<StorageReference>> {
        return try {
            val sessionRef = storage.reference
                .child("recording_sessions")
                .child(sessionId)
            
            val listResult = sessionRef.listAll().await()
            Result.success(listResult.items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get storage usage for a session
     */
    suspend fun getSessionStorageUsage(sessionId: String): Result<Long> {
        return try {
            val files = listSessionFiles(sessionId).getOrThrow()
            var totalSize = 0L
            
            for (file in files) {
                val metadata = file.metadata.await()
                totalSize += metadata.sizeBytes
            }
            
            Result.success(totalSize)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}