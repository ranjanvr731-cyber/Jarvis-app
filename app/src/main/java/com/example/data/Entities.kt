package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "jarvis"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_memories")
data class UserMemory(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long,
    val isCompleted: Boolean = false,
    val createdDate: Long = System.currentTimeMillis()
)
