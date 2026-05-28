package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Int): ChatSession?

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Update
    suspend fun updateSession(session: ChatSession)

    @Delete
    suspend fun deleteSession(session: ChatSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_sessions")
    suspend fun clearAllSessions()
}

@Database(entities = [ChatSession::class, ChatMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ChatRepository(private val chatDao: ChatDao) {
    val allSessions: Flow<List<ChatSession>> = chatDao.getSessions()

    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun getSessionById(sessionId: Int): ChatSession? {
        return chatDao.getSessionById(sessionId)
    }

    suspend fun createSession(title: String, personaId: String): Long {
        val session = ChatSession(title = title, personaId = personaId)
        return chatDao.insertSession(session)
    }

    suspend fun updateSession(session: ChatSession) {
        chatDao.updateSession(session)
    }

    suspend fun deleteSession(session: ChatSession) {
        chatDao.deleteSession(session)
    }

    suspend fun saveMessage(sessionId: Int, role: String, text: String, timestamp: Long = System.currentTimeMillis()): Long {
        val message = ChatMessage(sessionId = sessionId, role = role, text = text, timestamp = timestamp)
        return chatDao.insertMessage(message)
    }

    suspend fun clearAll() {
        chatDao.clearAllSessions()
    }
}
