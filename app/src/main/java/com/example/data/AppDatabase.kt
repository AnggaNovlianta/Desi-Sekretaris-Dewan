package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val agenda: String,
    val recipientGroup: String, // e.g. "Seluruh Anggota DPRD", "Komisi I", etc.
    val status: String, // "DRAFT", "DIKIRIM", "SELESAI"
    val minutesContent: String = "", // Notulen mentah
    val attendeesList: String = "", // Nama-nama yang hadir
    val aiSummary: String = "", // Ringkasan otomatis dari Gemini
    val category: String = "Rapat Intern", // Kategori kegiatan (e.g., "Rapat Paripurna", "Rapat Komisi", etc.)
    val attendanceStatus: String = "", // format "recipientId:status;recipientId:status;..."
    val documentPath: String = "", // Path / URI of attached hardcopy document
    val documentName: String = "", // Display/filename of hardcopy document
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recipients")
data class Recipient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // e.g., "Ketua DPRD", "Wakil Ketua I", "Anggota Komisi III", "Sekretaris Daerah"
    val phoneNumber: String, // Untuk pengiriman undangan via WA dll.
    val partyOrFaction: String = "" // Fraksi / Partai
)

@Dao
interface MeetingDao {
    @Query("SELECT * FROM meetings ORDER BY timestamp DESC")
    fun getAllMeetings(): Flow<List<Meeting>>

    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getMeetingById(id: Int): Meeting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: Meeting): Long

    @Update
    suspend fun updateMeeting(meeting: Meeting)

    @Delete
    suspend fun deleteMeeting(meeting: Meeting)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteMeetingById(id: Int)
}

@Dao
interface RecipientDao {
    @Query("SELECT * FROM recipients ORDER BY role ASC, name ASC")
    fun getAllRecipients(): Flow<List<Recipient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipient(recipient: Recipient): Long

    @Update
    suspend fun updateRecipient(recipient: Recipient)

    @Delete
    suspend fun deleteRecipient(recipient: Recipient)

    @Query("DELETE FROM recipients WHERE id = :id")
    suspend fun deleteRecipientById(id: Int)
}

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderEmail: String,
    val recipientPhoneOrGroup: String, // Phone, group name or specific recipient's tag
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val attachmentPath: String = "",
    val attachmentName: String = ""
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Database(entities = [Meeting::class, Recipient::class, ChatMessage::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun recipientDao(): RecipientDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "desi_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
