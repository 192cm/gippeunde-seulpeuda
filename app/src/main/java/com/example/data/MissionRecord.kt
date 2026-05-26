package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mission_records")
data class MissionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val photoPath: String, // Local URI or File path
    val targetEmotion: String, // JSON block
    val resultEmotion: String, // JSON block
    val score: Float, // 0..100
    val latitude: Double,
    val longitude: Double
)
