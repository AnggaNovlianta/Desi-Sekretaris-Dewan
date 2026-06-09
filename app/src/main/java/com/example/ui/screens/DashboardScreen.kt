package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.Meeting
import com.example.ui.theme.GoldAccent
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.ToastUtils

import com.example.ui.viewmodel.DesiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DesiViewModel,
    meetings: List<Meeting>,
    recipientsCount: Int,
    onNavigateToInvitations: () -> Unit,
    onNavigateToMinutes: () -> Unit,
    onNavigateToRecipients: () -> Unit,
    onCreateInvitation: () -> Unit
) {
    val userRole by viewModel.userRole.collectAsState()
    val userEmail by viewModel.currentUserEmail.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()
    val meetingsInNext24Hours by viewModel.meetingsInNext24Hours.collectAsState()
    val notificationsList by viewModel.notificationsList.collectAsState()

    val context = LocalContext.current
    var showAddMeetingDialog by remember { mutableStateOf(false) }
    var meetingToEdit by remember { mutableStateOf<Meeting?>(null) }
    var meetingToView by remember { mutableStateOf<Meeting?>(null) }
    val rsvpStatuses = remember { mutableStateMapOf<Int, String>() }

    // Quick Add Form fields
    var addTitle by remember { mutableStateOf("") }
    var addDate by remember { mutableStateOf("") }
    var addTime by remember { mutableStateOf("") }
    var addLocation by remember { mutableStateOf("") }
    var addAgenda by remember { mutableStateOf("") }
    var addGroup by remember { mutableStateOf("Seluruh Anggota DPRD") }

    val recipientOptions = listOf(
        "Seluruh Anggota DPRD",
        "Komisi I (Pemerintahan)",
        "Komisi II (Keuangan)",
        "Komisi III (Kesejahteraan)",
        "Fraksi-Fraksi DPRD",
        "Pimpinan DPRD Prabumulih",
        "Pemerintah Kota Prabumulih",
        "Custom / Lainnya"
    )

    // Edit Form fields
    var editTitle by remember { mutableStateOf("") }
    var editDate by remember { mutableStateOf("") }
    var editTime by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editAgenda by remember { mutableStateOf("") }
    var editGroup by remember { mutableStateOf("") }
    var editStatus by remember { mutableStateOf("") }

    LaunchedEffect(meetingToEdit) {
        meetingToEdit?.let {
            editTitle = it.title
            editDate = it.date
            editTime = it.time
            editLocation = it.location
            editAgenda = it.agenda
            editGroup = it.recipientGroup
            editStatus = it.status
        }
    }

    val totalInvitations = meetings.size
    val draftCount = meetings.count { it.status == "DRAFT" }
    val sentCount = meetings.count { it.status == "DIKIRIM" }
    val finishedCount = meetings.count { it.status == "SELESAI" }

    // Upcoming agendas (meetings not finished yet)
    val upcomingMeetings = meetings.filter { it.status != "SELESAI" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DESI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        Text(
                            text = "Delegasi Elektronik & Sekretariat Interaktif",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GoldAccent,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (userRole == "SEKWAN") "Admin Sekwan" else "Anggota Dewan",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Keluar Akun",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Section with elegant background banner (Reacts to elected User Role)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Selamat Datang,",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (userRole == "SEKWAN") {
                                "Sekretaris DPRD Kota Prabumulih"
                            } else {
                                "Anggota Dewan Legislatif"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = GoldAccent.copy(alpha = 0.5f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (userRole == "SEKWAN") {
                                    "Kelola undangan, notulen rapat, dan optimalkan kegiatan dewan dengan asisten cerdas Desi."
                                } else {
                                    "Lihat agenda terkini, draf undangan, dan notulen keputusan rapat dewan yang tersinkron otomatis secara real-time dari Sekwan."
                                },
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // USER INTERACTIVE SINKRONISASI REAL-TIME & ROLE SWITCHER HUB (CRITICAL CUSTOMER REQUEST)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pusat Sinkronisasi Real-Time",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Glowing green dot indicating simulated WebSocket is open and listening
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF2E7D32))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // CURRENT FIREBASE AUTH SESSION INFO
                        val userEmailVal = userEmail ?: "Bypass Mode"
                        Text(
                            text = "SESI AKTIF (TERVERIFIKASI FIREBASE SECURE):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "E-mail: $userEmailVal",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Hak Otoritas: " + if (userRole == "SEKWAN") "Sekretaris Dewan (Administrator)" else "Anggota Dewan (Viewer Only)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // SYNC STATS OR LOGS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Status Jaringan Server:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = syncStatus,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSyncing) MaterialTheme.colorScheme.secondary else Color(0xFF2E7D32)
                                )
                            }
                            
                            IconButton(
                                onClick = { viewModel.triggerSimulatedSync() },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Sync",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Streaming logs terminal
                        Text(
                            text = "LOG REAL-TIME TRANSMISI DATA (AUTO-SYNC):",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF212121))
                                .padding(8.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(syncLogs) { log ->
                                    Text(
                                        text = log,
                                        color = if (log.contains("Unggah") || log.contains("Mencoba") || log.contains("Sistem")) Color(0xFF81D4FA) else if (log.contains("Hapus")) Color(0xFFFFCC80) else Color(0xFFA5D6A7),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // VISUAL ALERT FOR MEETINGS WITHIN THE NEXT 24 HOURS
            if (meetingsInNext24Hours.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.5.dp, 
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f), 
                                RoundedCornerShape(16.dp)
                            )
                            .testTag("visual_alert_next_24h_container"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Peringatan",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Rapat Mendatang (< 24 Jam)!",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Terdapat ${meetingsInNext24Hours.size} agenda rapat penting dalam 24 jam ke depan.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                meetingsInNext24Hours.forEach { meeting ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                            .clickable { meetingToView = meeting }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = meeting.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarMonth,
                                                        contentDescription = null,
                                                        tint = GoldAccent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = meeting.date,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.AccessTime,
                                                        contentDescription = null,
                                                        tint = GoldAccent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = meeting.time,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Detail",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // REAL-TIME NOTIFICATION INBOX CARD (Firestore Alerts)
            item {
                var isExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notification_center_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { isExpanded = !isExpanded }
                                .fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (notificationsList.size > 1) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Pemberitahuan",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notifikasi & Alarm DPRD (${notificationsList.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pemantau server & perubahan jadwal rapat real-time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Sembunyikan" else "Tampilkan",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                            Spacer(modifier = Modifier.height(12.dp))

                            if (notificationsList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Belum ada notifikasi baru.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    notificationsList.forEach { alarm ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when (alarm.type) {
                                                        "SCHEDULE_CHANGE" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                                        "NEW_MEETING" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                        "ALERT_24H" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                                    }
                                                )
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = when (alarm.type) {
                                                    "SCHEDULE_CHANGE" -> Icons.Default.Edit
                                                    "NEW_MEETING" -> Icons.Default.AddAlert
                                                    "ALERT_24H" -> Icons.Default.Warning
                                                    else -> Icons.Default.Info
                                                },
                                                contentDescription = alarm.type,
                                                tint = when (alarm.type) {
                                                    "SCHEDULE_CHANGE" -> MaterialTheme.colorScheme.error
                                                    "NEW_MEETING" -> MaterialTheme.colorScheme.primary
                                                    "ALERT_24H" -> MaterialTheme.colorScheme.secondary
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = alarm.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = alarm.body,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale("id", "ID"))
                                                val timestampString = "Pukul " + sdf.format(java.util.Date(alarm.timestamp))
                                                Text(
                                                    text = timestampString,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions Title
            item {
                Text(
                    text = if (userRole == "SEKWAN") "Kelola & Kirim Data" else "Akses Data Ter-Update",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Quick Action Buttons Grid (Depends on Selected User Role)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (userRole == "SEKWAN") {
                        // Create Invitation Button
                        Card(
                            onClick = onCreateInvitation,
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Buat Undangan",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Buat Undangan",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        // View Agenda/Invitations Button for Council member
                        Card(
                            onClick = onNavigateToInvitations,
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Lihat Jadwal",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Lihat Jadwal",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // View Contacts Button
                    Card(
                        onClick = onNavigateToRecipients,
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = "Daftar Penerima",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (userRole == "SEKWAN") "Daftar Penerima" else "Kontak Dewan",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Stat Cards Container
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ringkasan Kinerja",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Rapat",
                            value = totalInvitations.toString(),
                            icon = Icons.Default.Class,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToInvitations
                        )
                        StatCard(
                            title = "Undangan Terkirim",
                            value = sentCount.toString(),
                            icon = Icons.Default.Send,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToInvitations
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Notulen Selesai",
                            value = finishedCount.toString(),
                            icon = Icons.Default.AssignmentTurnedIn,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToMinutes
                        )
                        StatCard(
                            title = "Draft Undangan",
                            value = draftCount.toString(),
                            icon = Icons.Default.Drafts,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToInvitations
                        )
                    }
                }
            }

            // DEDICATED RECHARTS-STYLE MONTHLY AGENDA STATS BAR CHART
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .testTag("agenda_monthly_stats_container"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    val IndonesianMonths = remember {
                        listOf(
                            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                        )
                    }
                    val ShortMonths = remember {
                        listOf(
                            "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", 
                            "Jul", "Agt", "Sep", "Okt", "Nov", "Des"
                        )
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        // Title / Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dinamika Agenda Rapat",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Recharts Engine",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Statistik jumlah rapat yang diselenggarakan dewan per bulan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Filter Selection
                        var filterActiveOnly by remember { mutableStateOf(false) }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val filterOptions = listOf(
                                false to "Semua Bulan (12)",
                                true to "Hanya Bulan Aktif"
                            )
                            filterOptions.forEach { (optionVal, label) ->
                                val isSelected = filterActiveOnly == optionVal
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { filterActiveOnly = optionVal }
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Map monthly stats
                        val monthlyCounts = remember(meetings) {
                            val counts = IntArray(12) { 0 }
                            meetings.forEach { meeting ->
                                val dateStr = meeting.date
                                var parsedMonth = -1
                                
                                val parsedDate = AgendaDateUtils.parseDate(dateStr)
                                if (parsedDate != null) {
                                    val cal = Calendar.getInstance().apply { time = parsedDate }
                                    parsedMonth = cal.get(Calendar.MONTH)
                                } else {
                                    for (i in IndonesianMonths.indices) {
                                        if (dateStr.contains(IndonesianMonths[i], ignoreCase = true) ||
                                            dateStr.contains(ShortMonths[i], ignoreCase = true)) {
                                            parsedMonth = i
                                            break
                                        }
                                    }
                                }
                                if (parsedMonth in 0..11) {
                                    counts[parsedMonth]++
                                }
                            }
                            counts
                        }
                        
                        val maxCount = monthlyCounts.maxOrNull() ?: 0
                        val displayMax = maxOf(4, if (maxCount % 2 == 0) maxCount else maxCount + 1)
                        
                        var selectedLocalMonth by remember { mutableStateOf(-1) }
                        
                        val activeIndices = remember(monthlyCounts) {
                            (0..11).filter { monthlyCounts[it] > 0 }
                        }
                        val displayedMonthIndices = if (filterActiveOnly && activeIndices.isNotEmpty()) {
                            activeIndices
                        } else {
                            (0..11).toList()
                        }
                        
                        // CHART DRAWING CONTAINER
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 12.dp)
                        ) {
                            // Background Recharts-like horizontal tick grids
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 24.dp, bottom = 24.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                val tickStep = displayMax / 4
                                for (t in 4 downTo 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Draw thin gridline
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(0.8.dp)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                        )
                                    }
                                }
                            }
                            
                            // Y-Axis Ticks (Number scale on the left)
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(bottom = 24.dp)
                                    .width(18.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                val tickStep = displayMax / 4
                                for (t in 4 downTo 0) {
                                    val labelValue = t * tickStep
                                    Text(
                                        text = "$labelValue",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.height(11.sp.value.dp)
                                    )
                                }
                            }
                            
                            // Bars and labels
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 24.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                displayedMonthIndices.forEach { monthIdx ->
                                    val count = monthlyCounts[monthIdx]
                                    val isSelected = selectedLocalMonth == monthIdx
                                    
                                    val animatedRatio = animateFloatAsState(
                                        targetValue = if (displayMax > 0) count.toFloat() / displayMax else 0f,
                                        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
                                    )
                                    
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable {
                                                selectedLocalMonth = if (isSelected) -1 else monthIdx
                                            }
                                            .testTag("chart_bar_${ShortMonths[monthIdx].lowercase()}"),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom
                                    ) {
                                        // Count top overlay
                                        Box(
                                            modifier = Modifier
                                                .height(16.dp)
                                                .padding(horizontal = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (count > 0) {
                                                Text(
                                                    text = "$count",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        
                                        // Dynamic animated column
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .padding(horizontal = 3.dp),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
                                            )
                                            
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight(animatedRatio.value.coerceIn(0f, 1f))
                                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = if (isSelected) {
                                                                listOf(GoldAccent, MaterialTheme.colorScheme.primary)
                                                            } else {
                                                                listOf(
                                                                    MaterialTheme.colorScheme.primary,
                                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                                                )
                                                            }
                                                        )
                                                    )
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 0.dp,
                                                        color = if (isSelected) GoldAccent else Color.Transparent,
                                                        shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                                                    )
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            text = ShortMonths[monthIdx],
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold, 
                                                fontSize = 8.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            
                            // Highlight Tooltip overlay info
                            if (selectedLocalMonth != -1) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 10.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2D3748).copy(alpha = 0.95f))
                                        .border(0.8.dp, GoldAccent.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Rapat " + IndonesianMonths[selectedLocalMonth],
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GoldAccent)
                                        )
                                        Text(
                                            text = "Jumlah Agenda: ${monthlyCounts[selectedLocalMonth]} rapat",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Color.White)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Legend info block
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Jumlah Rapat Sekretariat & Dewan",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Clickable Meetings breakdown details underneath
                        if (selectedLocalMonth != -1) {
                            val selectedMonthName = IndonesianMonths[selectedLocalMonth]
                            val filteredMeetings = remember(selectedLocalMonth, meetings) {
                                meetings.filter { meeting ->
                                    val dateStr = meeting.date
                                    var parsedMonth = -1
                                    val parsedDate = AgendaDateUtils.parseDate(dateStr)
                                    if (parsedDate != null) {
                                        val cal = Calendar.getInstance().apply { time = parsedDate }
                                        parsedMonth = cal.get(Calendar.MONTH)
                                    } else {
                                        for (i in IndonesianMonths.indices) {
                                            if (dateStr.contains(IndonesianMonths[i], ignoreCase = true) ||
                                                dateStr.contains(ShortMonths[i], ignoreCase = true)) {
                                                parsedMonth = i
                                                break
                                            }
                                        }
                                    }
                                    parsedMonth == selectedLocalMonth
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Daftar Rapat Bulan $selectedMonthName (${filteredMeetings.size}):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                                Text(
                                    text = "Tap rapat untuk melihat info",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (filteredMeetings.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tidak ada agenda rapat terjadwal pada bulan ini.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    filteredMeetings.forEach { meeting ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { meetingToView = meeting },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = meeting.title,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Row(
                                                        modifier = Modifier.padding(top = 2.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = meeting.date.substringBefore(",").ifEmpty { meeting.date },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "•",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = meeting.time,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            when (meeting.status) {
                                                                "SELESAI" -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                                                                "DIKIRIM" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                                else -> Color(0xFFE65100).copy(alpha = 0.12f)
                                                            }
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = meeting.status,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                                                        color = when (meeting.status) {
                                                            "SELESAI" -> Color(0xFF2E7D32)
                                                            "DIKIRIM" -> MaterialTheme.colorScheme.primary
                                                            else -> Color(0xFFE65100)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // DEDICATED INLINE FORM COMPONENT FOR ADDING MEETINGS DIRECTLY IN PORTAL
            if (userRole == "SEKWAN") {
                item {
                    var showForm by remember { mutableStateOf(true) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showForm = !showForm },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Formulir Rapat Baru",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Tambah agenda pembahasan baru langsung dari dasbor",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (showForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (showForm) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (showForm) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = addTitle,
                                        onValueChange = { addTitle = it },
                                        label = { Text("Perihal Rapat (Title)*") },
                                        placeholder = { Text("cth: Rapat Paripurna LKPJ Walikota") },
                                        modifier = Modifier.fillMaxWidth().testTag("form_input_title"),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = addDate,
                                            onValueChange = { addDate = it },
                                            label = { Text("Tanggal (Date)*") },
                                            placeholder = { Text("Senin, 15 Juni 2026") },
                                            modifier = Modifier.weight(1.1f).testTag("form_input_date"),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = addTime,
                                            onValueChange = { addTime = it },
                                            label = { Text("Waktu (Time)") },
                                            placeholder = { Text("09:00 WIB") },
                                            modifier = Modifier.weight(0.9f).testTag("form_input_time"),
                                            singleLine = true
                                        )
                                    }

                                    OutlinedTextField(
                                        value = addLocation,
                                        onValueChange = { addLocation = it },
                                        label = { Text("Tempat / Ruang Rapat (Location)*") },
                                        placeholder = { Text("Ruang Rapat Paripurna DPRD") },
                                        modifier = Modifier.fillMaxWidth().testTag("form_input_location"),
                                        singleLine = true
                                    )

                                    var dropdownExpanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = addGroup,
                                            onValueChange = { addGroup = it },
                                            label = { Text("Sasaran Kepesertaan") },
                                            trailingIcon = {
                                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("form_input_group"),
                                            readOnly = true
                                        )
                                        DropdownMenu(
                                            expanded = dropdownExpanded,
                                            onDismissRequest = { dropdownExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.95f)
                                        ) {
                                            recipientOptions.forEach { groupName ->
                                                DropdownMenuItem(
                                                    text = { Text(groupName) },
                                                    onClick = {
                                                        addGroup = groupName
                                                        dropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = addAgenda,
                                        onValueChange = { addAgenda = it },
                                        label = { Text("Deskripsi Rapat / Agenda Utama (Description)*") },
                                        placeholder = { Text("Deskripsi rincian agenda, pokok permasalahan yang akan dibahas, rilis pers dewan, dll.") },
                                        modifier = Modifier.fillMaxWidth().testTag("form_input_description"),
                                        minLines = 3
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                addTitle = ""
                                                addDate = ""
                                                addTime = ""
                                                addLocation = ""
                                                addAgenda = ""
                                            },
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("Reset", color = MaterialTheme.colorScheme.error)
                                        }

                                        Button(
                                            onClick = {
                                                if (addTitle.isNotBlank() && addDate.isNotBlank() && addLocation.isNotBlank() && addAgenda.isNotBlank()) {
                                                    val newMObj = Meeting(
                                                        title = addTitle,
                                                        date = addDate,
                                                        time = addTime.ifEmpty { "09:00 WIB" },
                                                        location = addLocation,
                                                        agenda = addAgenda,
                                                        recipientGroup = addGroup,
                                                        status = "DRAFT"
                                                    )
                                                    viewModel.saveMeeting(newMObj)
                                                    viewModel.addSyncLog("Unggah: Agenda '${addTitle}' ditambahkan lewat Formulir Dasbor.")
                                                    
                                                    // Reset
                                                    addTitle = ""
                                                    addDate = ""
                                                    addTime = ""
                                                    addLocation = ""
                                                    addAgenda = ""
                                                    ToastUtils.show(context, "Agenda rapat baru sukses disimpan ke database!")
                                                } else {
                                                    ToastUtils.show(context, "Kolom bertanda bintang (*) wajib diisi!")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.testTag("form_submit_button")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Simpan Agenda", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CENTRALIZED UPCOMING MEETINGS COMPONENT (DESAIN DAN PENGELOLAAN INTEGRAL)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dashboard Pengelolaan Rapat",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Rapat aktif & tindakan real-time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (userRole == "SEKWAN") {
                            IconButton(
                                onClick = { showAddMeetingDialog = true },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah Rapat",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = "Lihat Semua",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier
                                .clickable { onNavigateToInvitations() }
                                .padding(4.dp)
                        )
                    }
                }
            }

            if (upcomingMeetings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = "Tidak ada agenda",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tidak ada agenda rapat dalam waktu dekat",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (userRole == "SEKWAN") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showAddMeetingDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buat Agenda Rapat", color = Color.White)
                                }
                            }
                        }
                    }
                }
            } else {
                items(upcomingMeetings) { meeting ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = meeting.recipientGroup,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (meeting.status) {
                                                "DIKIRIM" -> Color(0xFFE8F5E9)
                                                "SELESAI" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else -> Color(0xFFFFF3E0)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = meeting.status,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = when (meeting.status) {
                                            "DIKIRIM" -> Color(0xFF2E7D32)
                                            "SELESAI" -> MaterialTheme.colorScheme.primary
                                            else -> Color(0xFFE65100)
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = meeting.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Tanggal",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${meeting.date} • ${meeting.time}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Lokasi",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "DISELENGGARAKAN DI:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = meeting.location.ifEmpty { "Ruang Rapat Paripurna DPRD" },
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                if (userRole != "SEKWAN") {
                                    val rsvp = rsvpStatuses[meeting.id] ?: "Belum Konfirmasi"
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (rsvp != "Belum Konfirmasi") Icons.Default.CheckCircle else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (rsvp == "Hadir") Color(0xFF2E7D32) else if (rsvp == "Izin") Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Kehadiran Anggota: $rsvp",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (rsvp == "Hadir") Color(0xFF2E7D32) else if (rsvp == "Izin") Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(12.dp))

                            if (userRole == "SEKWAN") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { meetingToEdit = meeting },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit Rapat", style = MaterialTheme.typography.labelSmall)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.updateMeeting(meeting.copy(status = "SELESAI"))
                                            viewModel.addSyncLog("Unggah: Rapat '${meeting.title}' ditandai SELESAI lewat dasbor.")
                                            ToastUtils.show(context, "Pertemuan rapat selesai diperbarui")
                                        },
                                        modifier = Modifier.weight(1.2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Selesai", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }

                                    FilledIconButton(
                                        onClick = {
                                            viewModel.deleteMeeting(meeting)
                                            ToastUtils.show(context, "Agenda telah dihapus")
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFFFEBEE)),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "RSVP:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )

                                        FilterChip(
                                            selected = rsvpStatuses[meeting.id] == "Hadir",
                                            onClick = {
                                                rsvpStatuses[meeting.id] = "Hadir"
                                                viewModel.addSyncLog("Anggota: Konfirmasi HADIR untuk rapat '${meeting.title}'")
                                                ToastUtils.show(context, "Kehadiran Anda berhasil dikonfirmasi!")
                                            },
                                            label = { Text("Hadir", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFE8F5E9),
                                                selectedLabelColor = Color(0xFF2E7D32)
                                            )
                                        )

                                        FilterChip(
                                            selected = rsvpStatuses[meeting.id] == "Izin",
                                            onClick = {
                                                rsvpStatuses[meeting.id] = "Izin"
                                                viewModel.addSyncLog("Anggota: Konfirmasi IZIN untuk rapat '${meeting.title}'")
                                                ToastUtils.show(context, "Konfirmasi izin tercatat.")
                                            },
                                            label = { Text("Izin", fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFFFF3E0),
                                                selectedLabelColor = Color(0xFFE65100)
                                            )
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { meetingToView = meeting },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Pembahasan", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ============================================
    // DIALOGS & OVERLAYS FOR RAPAT MANAGEMENT
    // ============================================

    // 1. QUICK ADD MEETING DIALOG
    if (showAddMeetingDialog) {
        AlertDialog(
            onDismissRequest = { showAddMeetingDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tambah Rapat/Agenda Baru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = addTitle,
                        onValueChange = { addTitle = it },
                        label = { Text("Perihal Rapat") },
                        placeholder = { Text("cth: Rapat Dengar Pendapat Umum") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = addDate,
                        onValueChange = { addDate = it },
                        label = { Text("Hari, Tanggal") },
                        placeholder = { Text("cth: Senin, 15 Juni 2026") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = addTime,
                        onValueChange = { addTime = it },
                        label = { Text("Waktu Pelaksanaan") },
                        placeholder = { Text("cth: 09:00 WIB") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = addLocation,
                        onValueChange = { addLocation = it },
                        label = { Text("Tempat Ruang Rapat") },
                        placeholder = { Text("cth: Ruang Rapat Komisi III") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    var dropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = addGroup,
                            onValueChange = { addGroup = it },
                            label = { Text("Ditujukan Kepada") },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            recipientOptions.forEach { groupName ->
                                DropdownMenuItem(
                                    text = { Text(groupName) },
                                    onClick = {
                                        addGroup = groupName
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = addAgenda,
                        onValueChange = { addAgenda = it },
                        label = { Text("Agenda Utama") },
                        placeholder = { Text("Deskripsi materi yang dibahas...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addTitle.isNotBlank()) {
                            val newMObj = Meeting(
                                title = addTitle,
                                date = addDate.ifEmpty { "Hari Ini" },
                                time = addTime.ifEmpty { "09:00 WIB" },
                                location = addLocation.ifEmpty { "Ruang Rapat Paripurna DPRD" },
                                agenda = addAgenda,
                                recipientGroup = addGroup,
                                status = "DRAFT"
                            )
                            viewModel.saveMeeting(newMObj)
                            showAddMeetingDialog = false
                            addTitle = ""
                            addDate = ""
                            addTime = ""
                            addLocation = ""
                            addAgenda = ""
                            ToastUtils.show(context, "Agenda rapat baru sukses disimpan!")
                        } else {
                            ToastUtils.show(context, "Silakan masukkan Perihal Rapat")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Simpan Agenda", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMeetingDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // 2. QUICK EDIT DIALOG
    if (meetingToEdit != null) {
        val meeting = meetingToEdit!!
        AlertDialog(
            onDismissRequest = { meetingToEdit = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ubah Data Pertemuan Rapat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Perihal Rapat") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDate,
                        onValueChange = { editDate = it },
                        label = { Text("Hari, Tanggal") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTime,
                        onValueChange = { editTime = it },
                        label = { Text("Waktu Pertemuan") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("Ruangan / Tempat Rapat") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editGroup,
                        onValueChange = { editGroup = it },
                        label = { Text("Sasaran Peserta (Kelompok)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editAgenda,
                        onValueChange = { editAgenda = it },
                        label = { Text("Agenda") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ubah Status:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statusList = listOf("DRAFT", "DIKIRIM", "SELESAI")
                        statusList.forEach { stat ->
                            ElevatedFilterChip(
                                selected = editStatus == stat,
                                onClick = { editStatus = stat },
                                label = { Text(stat, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            val updatedM = meeting.copy(
                                title = editTitle,
                                date = editDate,
                                time = editTime,
                                location = editLocation,
                                agenda = editAgenda,
                                recipientGroup = editGroup,
                                status = editStatus
                            )
                            viewModel.updateMeeting(updatedM)
                            meetingToEdit = null
                            ToastUtils.show(context, "Perubahan rapat berhasil disimpan!")
                        } else {
                            ToastUtils.show(context, "Perihal rapat tidak boleh kosong")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Simpan Perubahan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { meetingToEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // 3. ROAD/DETIL VIEW DIALOG (ANGGOTA MEMBER ORIENTED)
    if (meetingToView != null) {
        val meeting = meetingToView!!
        AlertDialog(
            onDismissRequest = { meetingToView = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Informasi Lengkap Rapat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("ACARA RAPAT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(meeting.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tanggal", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(meeting.date, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Waktu", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(meeting.time, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("LOKASI / RUANG RAPAT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(meeting.location.ifEmpty { "Default Ruang Utama Paripurna" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("Agenda Utama Rapat:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = meeting.agenda.ifEmpty { "Tidak ada deskripsi agenda detail" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.addSyncLog("Anggota: Mengaktifkan alarm pengingat agenda '${meeting.title}'")
                            ToastUtils.show(context, "Alarm pengingat telah disetel untuk rapat ini!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aktifkan Alarm Pengingat", color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { meetingToView = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Selesai", color = Color.White)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MeetingItemRow(meeting: Meeting, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (meeting.status == "DIKIRIM") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (meeting.status == "DIKIRIM") Icons.Default.Send else Icons.Default.Edit,
                    contentDescription = "",
                    tint = if (meeting.status == "DIKIRIM") Color(0xFF2E7D32) else Color(0xFFE65100),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = meeting.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Tanggal",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${meeting.date} • ${meeting.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (meeting.status == "DIKIRIM") Color(0xFF2E7D32).copy(alpha = 0.12f)
                        else Color(0xFFE65100).copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = meeting.status,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (meeting.status == "DIKIRIM") Color(0xFF2E7D32) else Color(0xFFE65100)
                )
            }
        }
    }
}
