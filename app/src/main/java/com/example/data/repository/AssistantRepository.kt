package com.example.data.repository

import com.example.data.db.AssistantDao
import com.example.data.model.ActionLog
import com.example.data.model.ConversationMessage
import kotlinx.coroutines.flow.Flow

class AssistantRepository(private val dao: AssistantDao) {
    val allMessages: Flow<List<ConversationMessage>> = dao.getAllMessages()
    val allLogs: Flow<List<ActionLog>> = dao.getAllLogs()

    suspend fun insertMessage(message: ConversationMessage): Long {
        return dao.insertMessage(message)
    }

    suspend fun updateMessage(message: ConversationMessage) {
        dao.updateMessage(message)
    }

    suspend fun clearMessages() {
        dao.clearMessages()
    }

    suspend fun logAction(
        command: String,
        actionType: String,
        details: String,
        isSuccess: Boolean,
        isRoot: Boolean = false
    ): Long {
        return dao.insertLog(
            ActionLog(
                command = command,
                actionType = actionType,
                details = details,
                isSuccess = isSuccess,
                isRoot = isRoot
            )
        )
    }

    suspend fun clearLogs() {
        dao.clearLogs()
    }
}
