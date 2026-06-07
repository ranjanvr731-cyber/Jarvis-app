package com.example.data

import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {

    val allMessages: Flow<List<ChatMessage>> = db.chatMessageDao().getAllMessages()
    val allMemories: Flow<List<Memory>> = db.memoryDao().getAllMemories()
    val allTasks: Flow<List<Task>> = db.taskDao().getAllTasks()

    suspend fun insertMessage(message: ChatMessage) {
        db.chatMessageDao().insertMessage(message)
    }

    suspend fun clearAllMessages() {
        db.chatMessageDao().deleteAllMessages()
    }

    suspend fun getMemory(key: String): String? {
        return db.memoryDao().getMemoryByKey(key)?.value
    }

    suspend fun saveMemory(key: String, value: String) {
        db.memoryDao().insertMemory(Memory(key, value))
    }

    suspend fun deleteMemory(key: String) {
        db.memoryDao().deleteMemory(key)
    }

    suspend fun insertTask(title: String, details: String = "", isCompleted: Boolean = false) {
        db.taskDao().insertTask(Task(title = title, details = details, isCompleted = isCompleted))
    }

    suspend fun updateTaskStatus(id: Int, completed: Boolean) {
        db.taskDao().updateTaskStatus(id, completed)
    }

    suspend fun deleteTask(id: Int) {
        db.taskDao().deleteTask(id)
    }
}
