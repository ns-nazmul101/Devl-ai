package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_logs")
data class ActionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val command: String,
    val actionType: String,
    val details: String,
    val isSuccess: Boolean,
    val isRoot: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
