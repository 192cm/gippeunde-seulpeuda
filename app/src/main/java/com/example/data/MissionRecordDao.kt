package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionRecordDao {
    @Query("SELECT * FROM mission_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<MissionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MissionRecord)

    @Delete
    suspend fun deleteRecord(record: MissionRecord)

    @Query("SELECT * FROM mission_records WHERE date = :date LIMIT 1")
    suspend fun getRecordByDate(date: String): MissionRecord?
}
