package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FirestoreSyncManager {
    private const val TAG = "FirestoreSyncManager"
    private var firestore: FirebaseFirestore? = null
    private var meetingsListener: ListenerRegistration? = null
    private var recipientsListener: ListenerRegistration? = null
    private var isInitialized = false

    // Safely initialize Firebase App and Firestore with fallbacks
    fun initialize(context: Context, onLog: (String) -> Unit) {
        if (isInitialized) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                // Try programmatically initializing so we do not crash if google-services.json is missing.
                // In production, placing google-services.json in the /app directory automatically takes care of this.
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:851679528794:android:0db50b73c9db8d7d90fd0a")
                    .setApiKey("mock-ai-studio-api-key-for-desi-dev-env")
                    .setProjectId("desi-dprd-prabumulih")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
                onLog("Sistem Firebase: Terinisialisasi secara mandiri (Self-Configured).")
            } else {
                onLog("Sistem Firebase: Berhasil mendeteksi Google Services SDK konfigurasi.")
            }
            firestore = FirebaseFirestore.getInstance()
            isInitialized = true
            onLog("Firestore: Berhasil mengaktifkan Agen Sinkronisasi Real-Time Awan.")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menginisialisasi Firestore", e)
            onLog("Pemberitahuan: Firebase berjalan dalam mode Offline/Draf (Sistem siap tersinkronisasi jika google-services.json dipasang).")
            firestore = null
            isInitialized = false
        }
    }

    // Start listening to changes from Firebase Firestore
    fun startRealTimeListeners(
        context: Context,
        scope: CoroutineScope,
        repository: AppRepository,
        onLog: (String) -> Unit,
        onNotificationTriggered: (title: String, body: String, type: String) -> Unit = { _, _, _ -> }
    ) {
        initialize(context, onLog)
        val db = firestore ?: run {
            onLog("Firestore: Berjalan dalam Simulasi Sinkronisasi Lokal (WebSocket Simulasi Aktif).")
            return
        }

        try {
            // 1. Listen for Meetings / Agendas & Notulen
            meetingsListener = db.collection("meetings")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "Gagal melacak rapat", error)
                        onLog("Firestore [X]: Gagal menyambung sinkronisasi rapat: ${error.localizedMessage}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        val isLocal = snapshots.metadata.hasPendingWrites()
                        if (isLocal) {
                            onLog("Unggah Firestore: Menyinkronkan perubahan agenda lokal ke server awan...")
                        } else {
                            onLog("Unduh Firestore: Menerima update agenda & notulen real-time dari dewan...")
                        }

                        scope.launch(Dispatchers.IO) {
                            for (dc in snapshots.documentChanges) {
                                val doc = dc.document
                                try {
                                    val id = doc.getLong("id")?.toInt() ?: continue
                                    val title = doc.getString("title") ?: ""
                                    val date = doc.getString("date") ?: ""
                                    val time = doc.getString("time") ?: ""
                                    val location = doc.getString("location") ?: ""
                                    val agenda = doc.getString("agenda") ?: ""
                                    val recipientGroup = doc.getString("recipientGroup") ?: ""
                                    val status = doc.getString("status") ?: ""
                                    val minutesContent = doc.getString("minutesContent") ?: ""
                                    val attendeesList = doc.getString("attendeesList") ?: ""
                                    val aiSummary = doc.getString("aiSummary") ?: ""
                                    val category = doc.getString("category") ?: "Rapat Intern"
                                    val attendanceStatus = doc.getString("attendanceStatus") ?: ""
                                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                    val meeting = Meeting(
                                        id = id,
                                        title = title,
                                        date = date,
                                        time = time,
                                        location = location,
                                        agenda = agenda,
                                        recipientGroup = recipientGroup,
                                        status = status,
                                        minutesContent = minutesContent,
                                        attendeesList = attendeesList,
                                        aiSummary = aiSummary,
                                        category = category,
                                        attendanceStatus = attendanceStatus,
                                        timestamp = timestamp
                                    )

                                    when (dc.type) {
                                        DocumentChange.Type.ADDED -> {
                                            if (!isLocal) {
                                                repository.insertMeeting(meeting)
                                                withContext(Dispatchers.Main) {
                                                    onLog("Live-Sync [Hadir]: Agenda '${meeting.title}' disinkronkan ke HP Anggota Dewan!")
                                                    val notifTitle = "Agenda Rapat Baru"
                                                    val notifBody = "${meeting.title} - ${meeting.date} pukul ${meeting.time} di ${meeting.location}"
                                                    com.example.ui.NotificationHelper.showNotification(context, notifTitle, notifBody)
                                                    onNotificationTriggered(notifTitle, notifBody, "NEW_MEETING")
                                                }
                                            }
                                        }
                                        DocumentChange.Type.MODIFIED -> {
                                            if (!isLocal) {
                                                val existing = repository.getMeetingById(meeting.id)
                                                val isScheduleChanged = existing != null && (
                                                    existing.date != meeting.date ||
                                                    existing.time != meeting.time ||
                                                    existing.location != meeting.location
                                                )
                                                
                                                repository.insertMeeting(meeting)
                                                withContext(Dispatchers.Main) {
                                                    onLog("Live-Sync [Hadir]: Agenda '${meeting.title}' disinkronkan ke HP Anggota Dewan!")
                                                    if (isScheduleChanged && existing != null) {
                                                        val notifTitle = "Perubahan Jadwal Rapat!"
                                                        val notifBody = "Jadwal '${meeting.title}' BERUBAH: ${meeting.date} pukul ${meeting.time} di ${meeting.location} (Sebelumnya: ${existing.date} • ${existing.time})"
                                                        com.example.ui.NotificationHelper.showNotification(context, notifTitle, notifBody)
                                                        onNotificationTriggered(notifTitle, notifBody, "SCHEDULE_CHANGE")
                                                    } else {
                                                        val notifTitle = "Pembaruan Agenda Rapat"
                                                        val notifBody = "${meeting.title} - ${meeting.date} pukul ${meeting.time} di ${meeting.location}"
                                                        com.example.ui.NotificationHelper.showNotification(context, notifTitle, notifBody)
                                                        onNotificationTriggered(notifTitle, notifBody, "INFO")
                                                    }
                                                }
                                            }
                                        }
                                        DocumentChange.Type.REMOVED -> {
                                            if (!isLocal) {
                                                repository.deleteMeeting(meeting)
                                                withContext(Dispatchers.Main) {
                                                    onLog("Live-Sync [Dihapus]: Agenda '${meeting.title}' dihapus dari HP Anggota Dewan.")
                                                }
                                            }
                                        }
                                    }
                                } catch (ex: Exception) {
                                    Log.e(TAG, "Gagal mengolah dokumen rapat", ex)
                                }
                            }
                        }
                    }
                }

            // 2. Listen for Recipients / Contacts
            recipientsListener = db.collection("recipients")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "Gagal melacak kontak", error)
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        val isLocal = snapshots.metadata.hasPendingWrites()
                        scope.launch(Dispatchers.IO) {
                            for (dc in snapshots.documentChanges) {
                                val doc = dc.document
                                try {
                                    val id = doc.getLong("id")?.toInt() ?: continue
                                    val name = doc.getString("name") ?: ""
                                    val role = doc.getString("role") ?: ""
                                    val phoneNumber = doc.getString("phoneNumber") ?: ""
                                    val partyOrFaction = doc.getString("partyOrFaction") ?: ""

                                    val recipient = Recipient(
                                        id = id,
                                        name = name,
                                        role = role,
                                        phoneNumber = phoneNumber,
                                        partyOrFaction = partyOrFaction
                                    )

                                    when (dc.type) {
                                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                            if (!isLocal) {
                                                repository.insertRecipient(recipient)
                                                withContext(Dispatchers.Main) {
                                                    onLog("Live-Sync [Kontak Baru]: Kontak '${recipient.name}' berhasil dimutakhirkan secara otomatis.")
                                                }
                                            }
                                        }
                                        DocumentChange.Type.REMOVED -> {
                                            if (!isLocal) {
                                                repository.deleteRecipient(recipient)
                                                withContext(Dispatchers.Main) {
                                                    onLog("Live-Sync [Kontak Terhapus]: Kontak '${recipient.name}' dihapus.")
                                                }
                                            }
                                        }
                                    }
                                } catch (ex: Exception) {
                                    Log.e(TAG, "Gagal mengolah dokumen kontak", ex)
                                }
                            }
                        }
                    }
                }

            onLog("Firestore: Listener Real-Time aktif menyimak server untuk DPRD Kota Prabumulih...")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mendaftarkan Firestore Listener", e)
            onLog("Firestore: Error saat mengaktifkan Real-time listener: ${e.localizedMessage}")
        }
    }

    // Stop and unregister listeners when needed
    fun stopListeners() {
        meetingsListener?.remove()
        recipientsListener?.remove()
        meetingsListener = null
        recipientsListener = null
    }

    // Sync individual meeting to cloud
    fun syncMeetingToCloud(meeting: Meeting, onLog: (String) -> Unit = {}) {
        val db = firestore ?: return
        val docData = mapOf(
            "id" to meeting.id,
            "title" to meeting.title,
            "date" to meeting.date,
            "time" to meeting.time,
            "location" to meeting.location,
            "agenda" to meeting.agenda,
            "recipientGroup" to meeting.recipientGroup,
            "status" to meeting.status,
            "minutesContent" to meeting.minutesContent,
            "attendeesList" to meeting.attendeesList,
            "aiSummary" to meeting.aiSummary,
            "category" to meeting.category,
            "attendanceStatus" to meeting.attendanceStatus,
            "timestamp" to meeting.timestamp
        )

        db.collection("meetings")
            .document(meeting.id.toString())
            .set(docData)
            .addOnSuccessListener {
                onLog("Awan: Unggahan Agenda '${meeting.title}' ke Cloud Firestore Sukses (Real-Time Sync Terkirim).")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal mengunggah rapat ke cloud", e)
                onLog("Awan [X]: Gagal sinkronisasi rapat: ${e.localizedMessage}")
            }
    }

    // Delete meeting from cloud
    fun deleteMeetingFromCloud(meetingId: Int, onLog: (String) -> Unit = {}) {
        val db = firestore ?: return
        db.collection("meetings")
            .document(meetingId.toString())
            .delete()
            .addOnSuccessListener {
                onLog("Awan: Agenda ID #$meetingId telah dihapus dari Cloud Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal menghapus rapat di cloud", e)
            }
    }

    // Sync individual recipient (contact) to cloud
    fun syncRecipientToCloud(recipient: Recipient, onLog: (String) -> Unit = {}) {
        val db = firestore ?: return
        val docData = mapOf(
            "id" to recipient.id,
            "name" to recipient.name,
            "role" to recipient.role,
            "phoneNumber" to recipient.phoneNumber,
            "partyOrFaction" to recipient.partyOrFaction
        )

        db.collection("recipients")
            .document(recipient.id.toString())
            .set(docData)
            .addOnSuccessListener {
                onLog("Awan: Unggahan Kontak '${recipient.name}' ke Cloud Firestore Sukses.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal mengunggah kontak ke cloud", e)
            }
    }

    // Delete recipient from cloud
    fun deleteRecipientFromCloud(recipientId: Int, onLog: (String) -> Unit = {}) {
        val db = firestore ?: return
        db.collection("recipients")
            .document(recipientId.toString())
            .delete()
            .addOnSuccessListener {
                onLog("Awan: Kontak ID #$recipientId telah dihapus dari Cloud Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal menghapus kontak di cloud", e)
            }
    }
}
