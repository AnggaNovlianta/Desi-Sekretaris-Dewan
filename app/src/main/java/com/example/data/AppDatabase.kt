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

@Database(entities = [Meeting::class, Recipient::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun recipientDao(): RecipientDao

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
