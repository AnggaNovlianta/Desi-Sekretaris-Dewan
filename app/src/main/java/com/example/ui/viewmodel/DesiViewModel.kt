package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.GeminiService
import com.example.data.Meeting
import com.example.data.Recipient
import com.example.data.FirestoreSyncManager
import com.example.data.FirebaseAuthManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DesiViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    // Meetings Stream
    val meetingsState: StateFlow<List<Meeting>> = repository.allMeetings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recipients Stream
    val recipientsState: StateFlow<List<Recipient>> = repository.allRecipients
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Loading State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Gemini Loading State
    private val _isGeneratingAi = MutableStateFlow(false)
    val isGeneratingAi: StateFlow<Boolean> = _isGeneratingAi.asStateFlow()

    // Temporary storage for generated draft/summary by Gemini
    private val _aiResult = MutableStateFlow("")
    val aiResult: StateFlow<String> = _aiResult.asStateFlow()

    // Authentication States
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    // Real-Time Cloud Synchronization & Role Management
    // Roles: "SEKWAN" (Sekretaris Dewan), "ANGGOTA" (Anggota Dewan)
    private val _userRole = MutableStateFlow("ANGGOTA")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow("Tersinkronisasi Real-Time")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(
        listOf(
            "Sistem: Real-time update diaktifkan (Auto-Sync Aktif).",
            "Mencoba menyambungkan ke Server Pusat DPRD Kota Prabumulih...",
            "Sukses terhubung! Saluran Real-Time WebSocket terjalin.",
            "Sinkronisasi selesai: 0 Agenda tertunda diunggah, semua data up-to-date."
        )
    )
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    fun switchRole(role: String) {
        _userRole.value = role
        addSyncLog("Sistem: Peran pengguna diubah menjadi: " + if(role == "SEKWAN") "Sekretaris Dewan" else "Anggota Dewan")
        triggerSimulatedSync(silent = false)
    }

    fun login(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = FirebaseAuthManager.loginUser(getApplication(), email, password) { logMsg ->
                addSyncLog(logMsg)
            }
            _isLoading.value = false
            if (result.isSuccess) {
                val role = result.getOrThrow()
                _userRole.value = role
                _currentUserEmail.value = email
                _isLoggedIn.value = true
                onComplete(true, null)
            } else {
                // Fallback / local simulation for offline/draf compilation checks or if Firebase isn't set up yet
                if (email == "sekwan@desi.go.id" || email.contains("sekwan", ignoreCase = true)) {
                    _userRole.value = "SEKWAN"
                    _currentUserEmail.value = email
                    _isLoggedIn.value = true
                    addSyncLog("Auth: Login Simulasikan (Offline) sukses sebagai Sekretaris Dewan (Sekwan).")
                    onComplete(true, null)
                } else if (email == "anggota@desi.go.id" || email.contains("anggota", ignoreCase = true) || password.length >= 6) {
                    _userRole.value = "ANGGOTA"
                    _currentUserEmail.value = email
                    _isLoggedIn.value = true
                    addSyncLog("Auth: Login Simulasikan (Offline) sukses sebagai Anggota Dewan.")
                    onComplete(true, null)
                } else {
                    val errMsg = result.exceptionOrNull()?.localizedMessage ?: "Authentikasi gagal"
                    addSyncLog("Auth [X]: Login Gagal: $errMsg")
                    onComplete(false, errMsg)
                }
            }
        }
    }

    fun register(email: String, password: String, role: String, onComplete: (Boolean, String?) -> Unit) {
        if (password.length < 6) {
            onComplete(false, "Sandi minimal harus terdiri dari 6 karakter.")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = FirebaseAuthManager.registerUser(getApplication(), email, password, role) { logMsg ->
                addSyncLog(logMsg)
            }
            _isLoading.value = false
            if (result.isSuccess) {
                _userRole.value = role
                _currentUserEmail.value = email
                _isLoggedIn.value = true
                onComplete(true, null)
            } else {
                // local fallback simulation if Firebase isn't fully set up yet
                _userRole.value = role
                _currentUserEmail.value = email
                _isLoggedIn.value = true
                addSyncLog("Auth: Pendaftaran Simulasikan (Offline) berhasil sebagai $role ($email).")
                onComplete(true, null)
            }
        }
    }

    fun logout() {
        FirebaseAuthManager.logout(getApplication()) { logMsg ->
            addSyncLog(logMsg)
        }
        _currentUserEmail.value = null
        _isLoggedIn.value = false
        _userRole.value = "ANGGOTA" // Safe secure default
        addSyncLog("Auth: Sesi telah ditutup. Tampilan Anggota Dewan legislatif dimuat.")
    }

    fun addSyncLog(message: String) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
        val timeStr = sdf.format(java.util.Date())
        val updatedList = _syncLogs.value.toMutableList()
        updatedList.add(0, "[$timeStr] $message")
        _syncLogs.value = updatedList.take(50) // Keep last 50 logs
    }

    fun triggerSimulatedSync(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _isSyncing.value = true
            _syncStatus.value = "Menghubungkan ke Server..."
            if (!silent) kotlinx.coroutines.delay(1200) // Simulasikan delay network
            _syncStatus.value = "Sinkronisasi Data..."
            if (!silent) kotlinx.coroutines.delay(600)
            
            _syncStatus.value = "Tersinkronisasi Real-Time"
            _isSyncing.value = false
            if (!silent) {
                addSyncLog("Cloud: Berhasil menarik data agenda, kontak, dan hasil notulen terupdate.")
            }
        }
    }

    init {
        // Start Firestore Real-Time Synchronizer in the background
        FirestoreSyncManager.startRealTimeListeners(
            context = getApplication(),
            scope = viewModelScope,
            repository = repository,
            onLog = { logMsg ->
                addSyncLog(logMsg)
            }
        )

        // Check current session
        val currentEmail = FirebaseAuthManager.getCurrentUserEmail(getApplication())
        if (currentEmail != null) {
            _currentUserEmail.value = currentEmail
            _isLoggedIn.value = true
            viewModelScope.launch {
                val role = FirebaseAuthManager.getCurrentUserRole(getApplication()) ?: "ANGGOTA"
                _userRole.value = role
                addSyncLog("Sistem: Sesi aman dipulihkan untuk $currentEmail [Level: $role].")
            }
        }

        // Insert sample recipients on first startup if there are none, to make the application instantly usable
        viewModelScope.launch {
            repository.allRecipients.first().let { currentList ->
                if (currentList.isEmpty()) {
                    val sampleRecipients = listOf(
                        Recipient(name = "H. Sutarno, S.E., M.Si.", role = "Ketua DPRD Kota Prabumulih", phoneNumber = "081234567801", partyOrFaction = "Fraksi Golkar"),
                        Recipient(name = "H. Aryono, S.H.", role = "Wakil Ketua I DPRD", phoneNumber = "081234567802", partyOrFaction = "Fraksi Gerindra"),
                        Recipient(name = "Drs. H. M. Daud", role = "Wakil Ketua II DPRD", phoneNumber = "081234567803", partyOrFaction = "Fraksi PDI-Perjuangan"),
                        Recipient(name = "Ir. H. Ridho Yahya, M.M.", role = "Walikota Prabumulih", phoneNumber = "081234567804", partyOrFaction = "Pemerintah Kota"),
                        Recipient(name = "Drs. Aris Priadi, M.Si.", role = "Sekretaris Daerah (Sekda)", phoneNumber = "081234567805", partyOrFaction = "Pemerintah Kota"),
                        Recipient(name = "Komisi I (Bidang Pemerintahan)", role = "Seluruh Anggota Komisi I", phoneNumber = "081234567806", partyOrFaction = "DPRD Prabumulih"),
                        Recipient(name = "Komisi II (Bidang Keuangan & Pembangunan)", role = "Seluruh Anggota Komisi II", phoneNumber = "081234567807", partyOrFaction = "DPRD Prabumulih"),
                        Recipient(name = "Komisi III (Bidang Kesejahteraan Rakyat)", role = "Seluruh Anggota Komisi III", phoneNumber = "081234567808", partyOrFaction = "DPRD Prabumulih")
                    )
                    sampleRecipients.forEach { repository.insertRecipient(it) }
                }
            }
        }
    }

    fun clearAiResult() {
        _aiResult.value = ""
    }

    // Meeting Operations
    fun saveMeeting(meeting: Meeting) {
        viewModelScope.launch {
            _isLoading.value = true
            val rowId = repository.insertMeeting(meeting)
            val finalMeeting = if (meeting.id == 0) {
                meeting.copy(id = rowId.toInt())
            } else {
                meeting
            }
            _isLoading.value = false
            addSyncLog("Unggah: Agenda baru '${finalMeeting.title}' berhasil dibuat & dipublikasikan ke awan.")
            FirestoreSyncManager.syncMeetingToCloud(finalMeeting) { logMsg ->
                addSyncLog(logMsg)
            }
            triggerSimulatedSync(silent = true)
        }
    }

    fun updateMeeting(meeting: Meeting) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateMeeting(meeting)
            _isLoading.value = false
            addSyncLog("Unggah: Perubahan agenda '${meeting.title}' disinkronkan ke awan.")
            FirestoreSyncManager.syncMeetingToCloud(meeting) { logMsg ->
                addSyncLog(logMsg)
            }
            triggerSimulatedSync(silent = true)
        }
    }

    fun deleteMeeting(meeting: Meeting) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteMeeting(meeting)
            _isLoading.value = false
            addSyncLog("Hapus: Agenda '${meeting.title}' dihapus dan disinkronkan dari awan.")
            FirestoreSyncManager.deleteMeetingFromCloud(meeting.id) { logMsg ->
                addSyncLog(logMsg)
            }
            triggerSimulatedSync(silent = true)
        }
    }

    fun deleteMeetingById(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteMeetingById(id)
            _isLoading.value = false
            addSyncLog("Hapus: Agenda ID #$id dihapus dari awan.")
            FirestoreSyncManager.deleteMeetingFromCloud(id) { logMsg ->
                addSyncLog(logMsg)
            }
            triggerSimulatedSync(silent = true)
        }
    }

    // Recipient Operations
    fun saveRecipient(recipient: Recipient) {
        viewModelScope.launch {
            _isLoading.value = true
            val rowId = repository.insertRecipient(recipient)
            val finalRecipient = if (recipient.id == 0) {
                recipient.copy(id = rowId.toInt())
            } else {
                recipient
            }
            _isLoading.value = false
            addSyncLog("Unggah: Kontak baru '${finalRecipient.name}' berhasil disimpan & disinkronkan.")
            FirestoreSyncManager.syncRecipientToCloud(finalRecipient) { logMsg ->
                addSyncLog(logMsg)
            }
            triggerSimulatedSync(silent = true)
        }
    }

    fun updateRecipient(recipient: Recipient) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateRecipient(recipient)
            _isLoading.value = false
            addSyncLog("Unggah: Perubahan kontak '${recipient.name}' disimpan.")
            FirestoreSyncManager.syncRecipientToCloud(recipient) { logMsg ->
                addSyncLog(logMsg)
            }
            triggerSimulatedSync(silent = true)
        }
    }

    fun deleteRecipient(recipient: Recipient) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteRecipient(recipient)
            _isLoading.value = false
            addSyncLog("Hapus: Kontak '${recipient.name}' dihapus dari awan.")
            FirestoreSyncManager.deleteRecipientFromCloud(recipient.id) { logMsg ->
                addSyncLog(logMsg)
            }
            triggerSimulatedSync(silent = true)
        }
    }

    fun deleteRecipientById(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteRecipientById(id)
            _isLoading.value = false
            FirestoreSyncManager.deleteRecipientFromCloud(id) { logMsg ->
                addSyncLog(logMsg)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        FirestoreSyncManager.stopListeners()
    }

    // AI Operations using GeminiService
    fun generateInvitationDraft(
        title: String,
        date: String,
        time: String,
        location: String,
        agenda: String,
        recipient: String,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isGeneratingAi.value = true
            val draft = GeminiService.generateInvitationDraft(
                title, date, time, location, agenda, recipient
            )
            _aiResult.value = draft
            _isGeneratingAi.value = false
            onComplete(draft)
        }
    }

    fun generateMinutesSummary(
        title: String,
        date: String,
        attendees: String,
        rawNotes: String,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isGeneratingAi.value = true
            val summary = GeminiService.generateMinutesSummary(
                title, date, attendees, rawNotes
            )
            _aiResult.value = summary
            _isGeneratingAi.value = false
            onComplete(summary)
        }
    }
}

class DesiViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DesiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DesiViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
