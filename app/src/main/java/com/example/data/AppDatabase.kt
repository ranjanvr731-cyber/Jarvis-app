package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM user_memories")
    fun getAllMemories(): Flow<List<UserMemory>>

    @Query("SELECT * FROM user_memories WHERE `key` = :key LIMIT 1")
    suspend fun getMemoryValue(key: String): UserMemory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMemory(memory: UserMemory)

    @Query("DELETE FROM user_memories WHERE `key` = :key")
    suspend fun deleteMemory(key: String)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, completed: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Int)
}

@Database(entities = [ChatMessage::class, UserMemory::class, Task::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun memoryDao(): MemoryDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jarvis_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
