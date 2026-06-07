package com.example.data

import kotlinx.coroutines.flow.Flow

class JarvisRepository(private val db: AppDatabase) {
    val chatHistory: Flow<List<ChatMessage>> = db.chatDao().getAllMessages()
    val allMemories: Flow<List<UserMemory>> = db.memoryDao().getAllMemories()
    val allTasks: Flow<List<Task>> = db.taskDao().getAllTasks()

    suspend fun addChatMessage(sender: String, message: String) {
        db.chatDao().insertMessage(ChatMessage(sender = sender, message = message))
    }

    suspend fun clearChatHistory() {
        db.chatDao().clearHistory()
    }

    suspend fun getMemoryValue(key: String): String? {
        return db.memoryDao().getMemoryValue(key)?.value
    }

    suspend fun saveMemory(key: String, value: String) {
        db.memoryDao().saveMemory(UserMemory(key = key, value = value))
    }

    suspend fun deleteMemory(key: String) {
        db.memoryDao().deleteMemory(key)
    }

    suspend fun addTask(title: String, dueDate: Long, description: String = "") {
        db.taskDao().insertTask(Task(title = title, dueDate = dueDate, description = description))
    }

    suspend fun setTaskCompleted(id: Int, isCompleted: Boolean) {
        db.taskDao().updateTaskStatus(id, isCompleted)
    }

    suspend fun deleteTask(id: Int) {
        db.taskDao().deleteTask(id)
    }
}
