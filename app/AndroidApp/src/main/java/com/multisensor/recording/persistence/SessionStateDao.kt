package com.multisensor.recording.persistence

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionState(sessionState: SessionState)

    @Update
    suspend fun updateSessionState(sessionState: SessionState)

    @Query("SELECT * FROM session_state WHERE sessionId = :sessionId")
    suspend fun getSessionState(sessionId: String): SessionState?

    @Query("SELECT * FROM session_state ORDER BY timestamp DESC")
    suspend fun getAllSessionStates(): List<SessionState>

    @Query("SELECT * FROM session_state WHERE recordingState IN ('STARTING', 'RECORDING') ORDER BY timestamp DESC")
    suspend fun getActiveSessions(): List<SessionState>

    @Query("SELECT * FROM session_state ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSession(): SessionState?

    @Query("DELETE FROM session_state WHERE sessionId = :sessionId")
    suspend fun deleteSessionState(sessionId: String)

    @Query("DELETE FROM session_state WHERE recordingState IN ('COMPLETED', 'FAILED') AND timestamp < :cutoffTime")
    suspend fun deleteOldSessions(cutoffTime: Long)

    @Query("SELECT * FROM session_state WHERE errorOccurred = 1 ORDER BY timestamp DESC")
    suspend fun getFailedSessions(): List<SessionState>

    @Query("SELECT * FROM session_state WHERE recordingState IN ('STARTING', 'RECORDING') ORDER BY timestamp DESC")
    fun observeActiveSessions(): Flow<List<SessionState>>
}
