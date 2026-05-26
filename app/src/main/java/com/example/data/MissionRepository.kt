package com.example.data

import kotlinx.coroutines.flow.Flow

class MissionRepository(private val dao: MissionRecordDao) {
    val allRecords: Flow<List<MissionRecord>> = dao.getAllRecords()

    suspend fun insert(record: MissionRecord) {
        dao.insertRecord(record)
    }

    suspend fun delete(record: MissionRecord) {
        dao.deleteRecord(record)
    }

    suspend fun getRecordByDate(date: String): MissionRecord? {
        return dao.getRecordByDate(date)
    }
}
