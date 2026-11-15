package com.smsforwarder.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LogDao {

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): LiveData<List<LogEntry>>

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC LIMIT 200")
    suspend fun getRecentLogsSuspend(): List<LogEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: LogEntry)

    @Query("SELECT COUNT(*) FROM log_entries")
    suspend fun getCount(): Int

    /**
     * Delete the oldest entries so that only [keepCount] rows remain.
     */
    @Query(
        "DELETE FROM log_entries WHERE id IN " +
        "(SELECT id FROM log_entries ORDER BY timestamp ASC LIMIT MAX(0, (SELECT COUNT(*) FROM log_entries) - :keepCount))"
    )
    suspend fun trimToLimit(keepCount: Int = 200)

    @Query("DELETE FROM log_entries")
    suspend fun clearAll()
}
