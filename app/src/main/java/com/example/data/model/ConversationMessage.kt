package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_messages")
data class ConversationMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user", "assistant", "system"
    val text: String,
    val actionTag: String? = null, // e.g. "OPEN_APP", "VOLUME", "BRIGHTNESS", "FILE_OP", "ROOT_SHELL", "SCREENSHOT", "DEVICE_INFO", "SETTINGS"
    val actionStatus: String = "completed", // "executing", "completed", "failed", "pending_confirmation"
    val isVoice: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
