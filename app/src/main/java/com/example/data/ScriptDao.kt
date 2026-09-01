package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY lastRunAt DESC, createdAt DESC")
    fun getAllScripts(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun getScriptById(id: String): ScriptEntity?

    @Query("SELECT * FROM scripts WHERE isScheduled = 1")
    fun getScheduledScripts(): Flow<List<ScriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptEntity)

    @Update
    suspend fun updateScript(script: ScriptEntity)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteScriptById(id: String)

    @Query("UPDATE scripts SET runCount = runCount + 1, lastRunAt = :timestamp WHERE id = :id")
    suspend fun recordScriptRun(id: String, timestamp: Long)

    @Query("SELECT * FROM backup_snapshots ORDER BY timestamp DESC")
    fun getAllBackups(): Flow<List<BackupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupEntity)

    @Query("DELETE FROM backup_snapshots WHERE id = :id")
    suspend fun deleteBackupById(id: String)
}
