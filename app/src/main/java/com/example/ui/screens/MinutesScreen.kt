package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import com.example.ui.ToastUtils
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Meeting
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.DesiViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinutesScreen(
    viewModel: DesiViewModel,
    meetings: List<Meeting>
) {
    val context = LocalContext.current
    val userRole by viewModel.userRole.collectAsState()
    var selectedMeetingForNotes by remember { mutableStateOf<Meeting?>(null) }
    
    // Form states for adding standalone meeting minutes on-the-fly
    var showAddMinutesDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf("") }
    var newLocation by remember { mutableStateOf("") }
    var newAttendees by remember { mutableStateOf("") }
    var newNotesContent by remember { mutableStateOf("") }
    var newActiveTab by remember { mutableStateOf(0) } // 0 = Tulis Catatan, 1 = Ringkasan AI

    val commonLocations = listOf(
        "Ruang Rapat Paripurna",
        "Ruang Komisi I",
        "Ruang Komisi II",
        "Ruang Komisi III",
        "Ruang Rapat Pimpinan"
    )
    
    // Filtering meetings that are active or completed
    val meetingsForNotes = meetings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notulen Sidang & Rapat", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                actions = {
                    if (userRole == "SEKWAN") {
                        IconButton(
                            onClick = {
                                newTitle = ""
                                newDate = ""
                                newLocation = ""
                                newAttendees = ""
                                newNotesContent = ""
                                viewModel.clearAiResult()
                                newActiveTab = 0
                                showAddMinutesDialog = true
                            },
                            modifier = Modifier.testTag("action_add_minutes")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Catat Rapat Baru", tint = Color.White)
                        }
                    } else {
                        // Real-time synchronization badge
                        Row(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ter-Sync", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Color.White)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (userRole == "SEKWAN") {
                FloatingActionButton(
                    onClick = {
                        newTitle = ""
                        newDate = ""
                        newLocation = ""
                        newAttendees = ""
                        newNotesContent = ""
                        viewModel.clearAiResult()
                        newActiveTab = 0
                        showAddMinutesDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_minutes")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Catat Rapat Baru")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (meetingsForNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "Empty Notes",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum ada agenda rapat terdaftar",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Silakan buat kegiatan rapat di tab Undangan atau buat catatan rapat instan langsung dari layar ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                    if (userRole == "SEKWAN") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                newTitle = ""
                                newDate = ""
                                newLocation = ""
                                newAttendees = ""
                                newNotesContent = ""
                                viewModel.clearAiResult()
                                newActiveTab = 0
                                showAddMinutesDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("button_add_minutes_empty")
                        ) {
                            Text("Catat Rapat Baru", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(meetingsForNotes) { meeting ->
                        MinutesItemCard(
                            meeting = meeting,
                            onClick = { selectedMeetingForNotes = meeting }
                        )
                    }
                }
            }

            // DETAILED NOTES & AI SUMMARIZE DIALOG
            if (selectedMeetingForNotes != null) {
                val meeting = selectedMeetingForNotes!!
                
                var notesContent by remember { mutableStateOf(meeting.minutesContent) }
                var attendees by remember { mutableStateOf(meeting.attendeesList) }
                var activeTab by remember { mutableStateOf(0) } // 0 = Tulis Catatan, 1 = Ringkasan AI
                
                val isGeneratingState by viewModel.isGeneratingAi.collectAsState()
                val liveAiResult by viewModel.aiResult.collectAsState()

                // New States for local enhancements
                val recipients by viewModel.recipientsState.collectAsState()
                var selectedStyle by remember { mutableStateOf("Formal") }
                var showAttendancePicker by remember { mutableStateOf(false) }
                
                AlertDialog(
                    onDismissRequest = { 
                        selectedMeetingForNotes = null
                        viewModel.clearAiResult()
                    },
                    title = {
                        Column {
                            Text(
                                text = meeting.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                            Text(
                                text = "Tanggal: ${meeting.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 480.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Secondary Tabs
                            TabRow(
                                selectedTabIndex = activeTab,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Tab(
                                    selected = activeTab == 0,
                                    onClick = { activeTab = 0 },
                                    text = { Text("Tulis Catatan", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                )
                                Tab(
                                    selected = activeTab == 1,
                                    onClick = { activeTab = 1 },
                                    text = { Text("Asisten AI Desi", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (activeTab == 0) {
                                // Manual notes fields
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = attendees,
                                            onValueChange = { if (userRole == "SEKWAN") attendees = it },
                                            readOnly = userRole != "SEKWAN",
                                            label = { Text("Daftar Hadir Sidang/Rapat") },
                                            placeholder = { Text("Contoh: H. Sutarno (Ketua), Drs. Aris (Sekda)...") },
                                            leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (userRole == "SEKWAN" && recipients.isNotEmpty()) {
                                            IconButton(
                                                onClick = { showAttendancePicker = !showAttendancePicker },
                                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                            ) {
                                                Icon(Icons.Default.PlaylistAddCheck, contentDescription = "Pilih Hadir")
                                            }
                                        }
                                    }

                                    // Interactive Attendance Picker Drawer
                                    if (showAttendancePicker && userRole == "SEKWAN") {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Daftar Anggota DPRD Prabumulih:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                    TextButton(onClick = { showAttendancePicker = false }) {
                                                        Text("Selesai & Tutup", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Box(modifier = Modifier.heightIn(max = 120.dp)) {
                                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        items(recipients) { recipient ->
                                                            val isChecked = attendees.contains(recipient.name)
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable {
                                                                        if (isChecked) {
                                                                            val list = attendees.split(",")
                                                                                .map { it.trim() }
                                                                                .filter { it.isNotEmpty() && it != recipient.name }
                                                                            attendees = list.joinToString(", ")
                                                                        } else {
                                                                            val list = attendees.split(",")
                                                                                .map { it.trim() }
                                                                                .filter { it.isNotEmpty() }
                                                                                .toMutableList()
                                                                            if (!list.contains(recipient.name)) {
                                                                                list.add(recipient.name)
                                                                            }
                                                                            attendees = list.joinToString(", ")
                                                                        }
                                                                    }
                                                                    .padding(vertical = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Checkbox(
                                                                    checked = isChecked,
                                                                    onCheckedChange = { _ ->
                                                                        if (isChecked) {
                                                                            val list = attendees.split(",")
                                                                                .map { it.trim() }
                                                                                .filter { it.isNotEmpty() && it != recipient.name }
                                                                            attendees = list.joinToString(", ")
                                                                        } else {
                                                                            val list = attendees.split(",")
                                                                                .map { it.trim() }
                                                                                .filter { it.isNotEmpty() }
                                                                                .toMutableList()
                                                                            if (!list.contains(recipient.name)) {
                                                                                list.add(recipient.name)
                                                                            }
                                                                            attendees = list.joinToString(", ")
                                                                        }
                                                                    }
                                                                )
                                                                Column {
                                                                    Text(recipient.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                                    Text("${recipient.role} • ${recipient.partyOrFaction}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 9.sp)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Quorum Meter
                                    if (recipients.isNotEmpty()) {
                                        val totalRecipients = recipients.size
                                        val presentCount = recipients.count { attendees.contains(it.name) }
                                        val quorumRatio = if (totalRecipients > 0) (presentCount.toFloat() / totalRecipients) else 0f
                                        val quorumPercent = (quorumRatio * 100).toInt()
                                        val isQuorumReached = quorumPercent >= 50

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isQuorumReached) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isQuorumReached) Color(0xFF81C784) else Color(0xFFFFB74D))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = if (isQuorumReached) Icons.Default.CheckCircle else Icons.Default.Info,
                                                            contentDescription = null,
                                                            tint = if (isQuorumReached) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Kalkulator Kuorum DPRD",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = if (isQuorumReached) Color(0xFF1B5E20) else Color(0xFFE65100)
                                                        )
                                                    }
                                                    Text(
                                                        text = "$quorumPercent% Sidang",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isQuorumReached) Color(0xFF1B5E20) else Color(0xFFE65100)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (isQuorumReached) 
                                                        "Kuorum Terpenuhi ($presentCount/$totalRecipients hadir). Keputusan sidang bersifat sah secara hukum." 
                                                        else "Belum Kuorum ($presentCount/$totalRecipients hadir). Diperlukan minimal ${totalRecipients/2 + 1} anggota.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isQuorumReached) Color(0xFF2E7D32) else Color(0xFFD84315),
                                                    fontSize = 10.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(
                                                    progress = quorumRatio,
                                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                    color = if (isQuorumReached) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                                    trackColor = if (isQuorumReached) Color(0xFFC8E6C9) else Color(0xFFFFE0B2)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = notesContent,
                                        onValueChange = { if (userRole == "SEKWAN") notesContent = it },
                                        readOnly = userRole != "SEKWAN",
                                        label = { Text("Hasil Pembahasan Rapat (Jalannya Sidang)") },
                                        placeholder = { Text("Masukkan draf pembahasan secara berurutan atau poin kasar...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        minLines = 4
                                    )
                                }
                            } else {
                                // AI Section
                                if (isGeneratingState) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Desi sedang menganalisis materi rapat...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Menyusun ringkasan notulen resmi...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (userRole == "SEKWAN") {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Gaya Rangkuman:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    val styleList = listOf("Formal" to "Resmi", "Humas" to "Humas", "Aksi" to "Daftar Tugas")
                                                    styleList.forEach { (key, label) ->
                                                        val isSelected = selectedStyle == key
                                                        FilterChip(
                                                            selected = isSelected,
                                                            onClick = { selectedStyle = key },
                                                            label = { Text(label, fontSize = 10.sp) },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                                selectedLabelColor = MaterialTheme.colorScheme.primary
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    if (notesContent.isBlank()) {
                                                        ToastUtils.show(context, "Silakan isi Catatan Kasar Jalannya Sidang terlebih dahulu.")
                                                    } else {
                                                        viewModel.generateMinutesSummary(
                                                            meeting.title, meeting.date, attendees, notesContent, selectedStyle
                                                        )
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Rangkum Notulen dengan AI", color = Color.White)
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "Notulen resmi ini disusun & dikunci secara real-time oleh Sekretariat Dewan (Sekwan) menggunakan Asisten AI.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        val displaySummary = liveAiResult.ifEmpty { meeting.aiSummary }
                                        
                                        if (displaySummary.isNotEmpty()) {
                                            Text("Draf Ringkasan Hasil Rapat Resmi:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp)
                                            ) {
                                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                    item {
                                                        Text(
                                                            text = displaySummary,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Tulis catatan rapat di tab pertama terlebih dahulu, lalu klik tombol di atas untuk merangkum secara otomatis dengan bantuan asisten AI Desi.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeTab == 1 && (liveAiResult.isNotEmpty() || meeting.aiSummary.isNotEmpty())) {
                                FilledIconButton(
                                    onClick = {
                                        val finalSummary = liveAiResult.ifEmpty { meeting.aiSummary }
                                        shareText(context, "RINGKASAN NOTULEN RESMI DPRD KOTA PRABUMULIH\n\n$finalSummary")
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = Color.White)
                                }

                                Button(
                                    onClick = {
                                        val finalSummary = liveAiResult.ifEmpty { meeting.aiSummary }
                                        val currentMeeting = meeting.copy(
                                            aiSummary = finalSummary,
                                            minutesContent = notesContent,
                                            attendeesList = attendees
                                        )
                                        com.example.ui.PdfExporter.exportMeetingMinutesToPdf(context, currentMeeting)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("button_export_pdf_existing")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Ekspor PDF",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ekspor PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (userRole == "SEKWAN") {
                                Button(
                                    onClick = {
                                        val finalSummary = liveAiResult.ifEmpty { meeting.aiSummary }
                                        viewModel.updateMeeting(
                                            meeting.copy(
                                                minutesContent = notesContent,
                                                attendeesList = attendees,
                                                aiSummary = finalSummary,
                                                status = "SELESAI"
                                            )
                                        )
                                        selectedMeetingForNotes = null
                                        viewModel.clearAiResult()
                                        ToastUtils.show(context, "Notulen disimpan secara offline")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Simpan & Selesai", color = Color.White)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        selectedMeetingForNotes = null
                                        viewModel.clearAiResult()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Selesai Membaca", color = Color.White)
                                }
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                selectedMeetingForNotes = null
                                viewModel.clearAiResult()
                             }
                        ) {
                            Text("Tutup")
                        }
                    }
                )
            }

            // STANDALONE ADD MINUTES DIALOG
            if (showAddMinutesDialog) {
                var localTitle by remember { mutableStateOf(newTitle) }
                var localDate by remember { mutableStateOf(newDate) }
                var localLocation by remember { mutableStateOf(newLocation) }
                var localAttendees by remember { mutableStateOf(newAttendees) }
                var localNotesContent by remember { mutableStateOf(newNotesContent) }
                
                val isGeneratingState by viewModel.isGeneratingAi.collectAsState()
                val liveAiResult by viewModel.aiResult.collectAsState()

                val recipients by viewModel.recipientsState.collectAsState()
                var showAddAttendancePicker by remember { mutableStateOf(false) }
                var localSelectedStyle by remember { mutableStateOf("Formal") }

                AlertDialog(
                    onDismissRequest = { 
                        showAddMinutesDialog = false
                        viewModel.clearAiResult()
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notulen Rapat Baru", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 480.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Secondary Tabs
                            TabRow(
                                selectedTabIndex = newActiveTab,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Tab(
                                    selected = newActiveTab == 0,
                                    onClick = { newActiveTab = 0 },
                                    text = { Text("Tulis Catatan", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                )
                                Tab(
                                    selected = newActiveTab == 1,
                                    onClick = { newActiveTab = 1 },
                                    text = { Text("Asisten AI Desi", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (newActiveTab == 0) {
                                // Scrollable form fields for writing notes
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = localTitle,
                                        onValueChange = { localTitle = it },
                                        label = { Text("Nama / Hubungan Rapat") },
                                        placeholder = { Text("Contoh: Rapat Dengar Pendapat Umum...") },
                                        leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth().testTag("input_new_minutes_title")
                                    )

                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = localDate,
                                            onValueChange = { },
                                            readOnly = true,
                                            label = { Text("Pilih Tanggal Rapat") },
                                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        val calendar = Calendar.getInstance()
                                                        val datePickerDialog = DatePickerDialog(
                                                            context,
                                                            { _, year, month, dayOfMonth ->
                                                                val selectedCal = Calendar.getInstance().apply {
                                                                    set(Calendar.YEAR, year)
                                                                    set(Calendar.MONTH, month)
                                                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                                }
                                                                localDate = AgendaDateUtils.formatDate(selectedCal)
                                                            },
                                                            calendar.get(Calendar.YEAR),
                                                            calendar.get(Calendar.MONTH),
                                                            calendar.get(Calendar.DAY_OF_MONTH)
                                                        )
                                                        datePickerDialog.show()
                                                    }
                                                ) {
                                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pilih")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("input_new_minutes_date")
                                        )
                                    }

                                    OutlinedTextField(
                                        value = localLocation,
                                        onValueChange = { localLocation = it },
                                        label = { Text("Tempat Rapat") },
                                        leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth().testTag("input_new_minutes_location")
                                    )

                                    // Quick suggestions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        commonLocations.take(3).forEach { room ->
                                            SuggestionChip(
                                                onClick = { localLocation = room },
                                                label = { Text(room.replace("Ruang Rapat ", "R. "), fontSize = 10.sp) }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = localAttendees,
                                            onValueChange = { localAttendees = it },
                                            label = { Text("Daftar Hadir (Komposisi Peserta)") },
                                            placeholder = { Text("Siapa saja yang menghadiri...") },
                                            leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                                            modifier = Modifier.weight(1f).testTag("input_new_minutes_attendees")
                                        )
                                        if (recipients.isNotEmpty()) {
                                            IconButton(
                                                onClick = { showAddAttendancePicker = !showAddAttendancePicker },
                                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                            ) {
                                                Icon(Icons.Default.PlaylistAddCheck, contentDescription = "Pilih Hadir")
                                            }
                                        }
                                    }

                                    // Attendance checklist
                                    if (showAddAttendancePicker) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Daftar Anggota DPRD Prabumulih:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                    TextButton(onClick = { showAddAttendancePicker = false }) {
                                                        Text("Selesai & Tutup", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Box(modifier = Modifier.heightIn(max = 120.dp)) {
                                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        items(recipients) { recipient ->
                                                            val isChecked = localAttendees.contains(recipient.name)
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable {
                                                                        if (isChecked) {
                                                                            val list = localAttendees.split(",")
                                                                                .map { it.trim() }
                                                                                .filter { it.isNotEmpty() && it != recipient.name }
                                                                            localAttendees = list.joinToString(", ")
                                                                        } else {
                                                                            val list = localAttendees.split(",")
                                                                                .map { it.trim() }
                                                                                .filter { it.isNotEmpty() }
                                                                                .toMutableList()
                                                                            if (!list.contains(recipient.name)) {
                                                                                list.add(recipient.name)
                                                                            }
                                                                            localAttendees = list.joinToString(", ")
                                                                        }
                                                                    }
                                                                    .padding(vertical = 2.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Checkbox(
                                                                    checked = isChecked,
                                                                    onCheckedChange = { _ ->
                                                                        if (isChecked) {
                                                                            val list = localAttendees.split(",")
                                                                                .map { it.trim() }
                                                                                .filter { it.isNotEmpty() && it != recipient.name }
                                                                            localAttendees = list.joinToString(", ")
                                                                        } else {
                                                                            val list = localAttendees.split(",")
                                                                                .map { it.trim() }
                                                                                .filter { it.isNotEmpty() }
                                                                                .toMutableList()
                                                                            if (!list.contains(recipient.name)) {
                                                                                list.add(recipient.name)
                                                                            }
                                                                            localAttendees = list.joinToString(", ")
                                                                        }
                                                                    }
                                                                )
                                                                Column {
                                                                    Text(recipient.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                                    Text("${recipient.role} • ${recipient.partyOrFaction}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 9.sp)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Quorum Calculator
                                    if (recipients.isNotEmpty()) {
                                        val totalRecipients = recipients.size
                                        val presentCount = recipients.count { localAttendees.contains(it.name) }
                                        val quorumRatio = if (totalRecipients > 0) (presentCount.toFloat() / totalRecipients) else 0f
                                        val quorumPercent = (quorumRatio * 100).toInt()
                                        val isQuorumReached = quorumPercent >= 50

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isQuorumReached) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isQuorumReached) Color(0xFF81C784) else Color(0xFFFFB74D))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = if (isQuorumReached) Icons.Default.CheckCircle else Icons.Default.Info,
                                                            contentDescription = null,
                                                            tint = if (isQuorumReached) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Kalkulator Kuorum DPRD",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = if (isQuorumReached) Color(0xFF1B5E20) else Color(0xFFE65100)
                                                        )
                                                    }
                                                    Text(
                                                        text = "$quorumPercent% Sidang",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isQuorumReached) Color(0xFF1B5E20) else Color(0xFFE65100)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (isQuorumReached) 
                                                        "Kuorum Terpenuhi ($presentCount/$totalRecipients hadir). Keputusan sidang bersifat sah secara hukum." 
                                                        else "Belum Kuorum ($presentCount/$totalRecipients hadir). Diperlukan minimal ${totalRecipients/2 + 1} anggota.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isQuorumReached) Color(0xFF2E7D32) else Color(0xFFD84315),
                                                    fontSize = 10.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(
                                                    progress = quorumRatio,
                                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                                    color = if (isQuorumReached) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                                    trackColor = if (isQuorumReached) Color(0xFFC8E6C9) else Color(0xFFFFE0B2)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = localNotesContent,
                                        onValueChange = { localNotesContent = it },
                                        label = { Text("Jalannya Sidang / Hal-Hal yang Dibahas") },
                                        placeholder = { Text("Poin-poin diskusi mentah, saran, keberatan dewan...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .testTag("input_new_minutes_notes"),
                                        minLines = 3
                                    )
                                }
                            } else {
                                // AI Generation tab
                                if (isGeneratingState) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Desi sedang merancang ringkasan dinamika rapat...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Membuat ringkasan keputusan & tindak lanjut resmi...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Gaya Rangkuman:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                val styleList = listOf("Formal" to "Resmi", "Humas" to "Humas", "Aksi" to "Daftar Tugas")
                                                styleList.forEach { (key, label) ->
                                                    val isSelected = localSelectedStyle == key
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = { localSelectedStyle = key },
                                                        label = { Text(label, fontSize = 10.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                            selectedLabelColor = MaterialTheme.colorScheme.primary
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (localTitle.isBlank()) {
                                                    ToastUtils.show(context, "Silakan isi Judul Rapat terlebih dahulu.")
                                                } else if (localNotesContent.isBlank()) {
                                                    ToastUtils.show(context, "Silakan isi Jalannya Sidang / Catatan terlebih dahulu.")
                                                } else {
                                                    viewModel.generateMinutesSummary(
                                                        localTitle, localDate.ifEmpty { "Hari Ini" }, localAttendees.ifEmpty { "Anggota Dewan" }, localNotesContent, localSelectedStyle
                                                    )
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            modifier = Modifier.fillMaxWidth().testTag("button_generate_new_minutes")
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Rangkum Notulen dengan AI", color = Color.White)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        if (liveAiResult.isNotEmpty()) {
                                            Text("Draf Ringkasan Hasil Rapat Resmi:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                    .padding(12.dp)
                                            ) {
                                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                                    item {
                                                        Text(
                                                            text = liveAiResult,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Silakan isi data catatan di tab pertama, lalu klik Rangkum dengan AI untuk merangkun pokok diskusi secara otomatis menggunakan kecerdasan buatan.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (newActiveTab == 1 && liveAiResult.isNotEmpty()) {
                                FilledIconButton(
                                    onClick = {
                                        shareText(context, "RINGKASAN NOTULEN RESMI DPRD KOTA PRABUMULIH\n\n$liveAiResult")
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("button_share_new_minutes")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = Color.White)
                                }

                                Button(
                                    onClick = {
                                        val currentMeeting = Meeting(
                                            title = localTitle,
                                            date = localDate.ifEmpty { AgendaDateUtils.formatDate(Calendar.getInstance()) },
                                            time = "Lengkap",
                                            location = localLocation.ifEmpty { "Ruang Rapat DPRD" },
                                            agenda = "",
                                            recipientGroup = "Selesai Rapat",
                                            status = "SELESAI",
                                            minutesContent = localNotesContent,
                                            attendeesList = localAttendees,
                                            aiSummary = liveAiResult
                                        )
                                        com.example.ui.PdfExporter.exportMeetingMinutesToPdf(context, currentMeeting)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("button_export_pdf_new")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Ekspor PDF",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ekspor PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    if (localTitle.isBlank() || localNotesContent.isBlank()) {
                                        ToastUtils.show(context, "Silakan lengkapi Judul Rapat dan Jalannya Sidang sebelum menyimpan.")
                                    } else {
                                        val newMinutesMeeting = Meeting(
                                            title = localTitle,
                                            date = localDate.ifEmpty { AgendaDateUtils.formatDate(Calendar.getInstance()) },
                                            time = "Lengkap",
                                            location = localLocation.ifEmpty { "Ruang Rapat DPRD" },
                                            agenda = "",
                                            recipientGroup = "Selesai Rapat",
                                            status = "SELESAI",
                                            minutesContent = localNotesContent,
                                            attendeesList = localAttendees,
                                            aiSummary = liveAiResult
                                        )
                                        viewModel.saveMeeting(newMinutesMeeting)
                                        showAddMinutesDialog = false
                                        viewModel.clearAiResult()
                                        ToastUtils.show(context, "Notulen rapat disimpan sukses!")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("button_save_new_minutes")
                             ) {
                                Text("Simpan & Selesai", color = Color.White)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                showAddMinutesDialog = false
                                viewModel.clearAiResult()
                            }
                        ) {
                            Text("Tutup")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MinutesItemCard(
    meeting: Meeting,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Ruang: ${meeting.location}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (meeting.status == "SELESAI") Color(0xFF2E7D32).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (meeting.status == "SELESAI") "SUDAH DIRANGKUM" else "BUTUH NOTULEN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (meeting.status == "SELESAI") Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = meeting.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = meeting.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (meeting.minutesContent.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "Catatan Kasar Sidang:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = meeting.minutesContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (meeting.status == "SELESAI") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (meeting.status == "SELESAI") Icons.Default.AutoAwesome else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (meeting.status == "SELESAI") "Buka Notulen & Ringkasan AI" else "Tulis & Rangkum Notulen",
                    color = Color.White
                )
            }
        }
    }
}
