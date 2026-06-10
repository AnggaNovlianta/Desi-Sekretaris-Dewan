package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.ui.ToastUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Meeting
import com.example.data.Recipient
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.DesiViewModel

// Helper functions for attendance serialization
fun parseAttendanceStatus(serialized: String): Map<Int, String> {
    if (serialized.isBlank()) return emptyMap()
    return serialized.split(";").mapNotNull {
        val parts = it.split(":")
        if (parts.size == 2) {
            val id = parts[0].toIntOrNull()
            if (id != null) id to parts[1] else null
        } else {
            null
        }
    }.toMap()
}

fun serializeAttendanceStatus(map: Map<Int, String>): String {
    return map.entries.joinToString(";") { "${it.key}:${it.value}" }
}

@Composable
fun AttendanceBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitationsScreen(
    viewModel: DesiViewModel,
    meetings: List<Meeting>,
    recipients: List<Recipient>,
    isAddingInitial: Boolean = false,
    onResetAddFlag: () -> Unit = {}
) {
    val context = LocalContext.current
    val userRole by viewModel.userRole.collectAsState()
    var showAddDialog by remember { mutableStateOf(isAddingInitial) }
    var selectedMeetingForDetail by remember { mutableStateOf<Meeting?>(null) }
    var showRsvpDialog by remember { mutableStateOf(false) }
    
    // AI Dialog State
    var showAiResultDialog by remember { mutableStateOf(false) }
    var aiDraftText by remember { mutableStateOf("") }
    val isGeneratingState by viewModel.isGeneratingAi.collectAsState()

    // Form inputs
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var agenda by remember { mutableStateOf("") }
    var recipientGroup by remember { mutableStateOf("Seluruh Anggota DPRD") }
    var documentPath by remember { mutableStateOf("") }
    var documentName by remember { mutableStateOf("") }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            documentPath = uri.toString()
            documentName = "Hardcopy_" + (uri.lastPathSegment?.substringAfterLast("/") ?: "Dokumen.pdf")
            ToastUtils.show(context, "Dokumen hardcopy berhasil dilampirkan!")
        }
    }
    
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

    // Trigger the dialog if opened from dashboard
    LaunchedEffect(isAddingInitial) {
        if (isAddingInitial) {
            showAddDialog = true
            onResetAddFlag()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Undangan Rapat DPRD", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                actions = {
                    if (userRole == "SEKWAN") {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Buat Undangan Baru", tint = Color.White)
                        }
                    } else {
                        // Small real-time status indicating synchronization is live
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
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
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
            val filteredMeetings = remember(meetings, userRole) {
                if (userRole == "ANGGOTA") {
                    meetings.filter { it.status == "DIKIRIM" || it.status == "SELESAI" }
                } else {
                    meetings
                }
            }

            if (filteredMeetings.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (userRole == "ANGGOTA") "Kotak Masuk Undangan Bersih" else "Belum ada undangan rapat",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (userRole == "ANGGOTA") "Menunggu undangan rapat resmi didepositkan langsung oleh Sekretaris Dewan." else "Kelola dan kirimkan draf undangan DPRD Kota Prabumulih di sini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                    if (userRole == "SEKWAN") {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Buat Undangan Pertama", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMeetings) { meeting ->
                        InvitationItemCard(
                            meeting = meeting,
                            onClick = { selectedMeetingForDetail = meeting },
                            onShare = {
                                shareText(
                                    context,
                                    "UNDANGAN RESMI DPRD KOTA PRABUMULIH\n\n" +
                                    "Kepada Yth. $recipientGroup\n\n" +
                                    "Perihal: ${meeting.title}\n" +
                                    "Hari/Tanggal: ${meeting.date}\n" +
                                    "Waktu: ${meeting.time}\n" +
                                    "Tempat: ${meeting.location}\n" +
                                    "Agenda: ${meeting.agenda}\n\n" +
                                    "Demikian undangan ini disampaikan. Atas kehadiran dan kerja samanya diucapkan terima kasih.\n\n" +
                                    "Dari: Sekretariat DPRD Kota Prabumulih"
                                )
                                if (meeting.status == "DRAFT") {
                                    viewModel.updateMeeting(meeting.copy(status = "DIKIRIM"))
                                }
                            }
                        )
                    }
                }
            }

            // ADD DIALOG
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Undangan Rapat Baru", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Isi detail undangan rapat di bawah ini secara lengkap. Layar ini mendukung gulir (scroll) saat mengetik.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Perihal / Nama Rapat", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Class, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                label = { Text("Hari, Tanggal pelaksanaan", fontWeight = FontWeight.Bold) },
                                placeholder = { Text("Contoh: Senin, 15 Juni 2026") },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = time,
                                onValueChange = { time = it },
                                label = { Text("Waktu Pelaksanaan", fontWeight = FontWeight.Bold) },
                                placeholder = { Text("Contoh: 09:00 WIB s/d Selesai") },
                                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Tempat / Lokasi Ruangan", fontWeight = FontWeight.Bold) },
                                placeholder = { Text("Contoh: Ruang Rapat Paripurna") },
                                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            // Recipient selector drop down & horizontal scroll shortcut chips
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Ditujukan Kepada (Pilih Cepat & Mudah Dibaca):",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    recipientOptions.forEach { option ->
                                        val isSelected = recipientGroup == option
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { recipientGroup = option },
                                            label = {
                                                Text(
                                                    text = option,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        )
                                    }
                                }
                                
                                var expanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = recipientGroup,
                                        onValueChange = { recipientGroup = it },
                                        label = { Text("Nama/Grup Kontak Penerima", fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        trailingIcon = {
                                            IconButton(onClick = { expanded = !expanded }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        },
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        recipientOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    recipientGroup = option
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = agenda,
                                onValueChange = { agenda = it },
                                label = { Text("Agenda Rapat", fontWeight = FontWeight.Bold) },
                                placeholder = { Text("Sebutkan rincian agenda pembahasan rapat...") },
                                leadingIcon = { Icon(Icons.Default.Subject, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )

                            // Document Upload Component for Hardcopy attachment
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Berkas Hardcopy Undangan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                            Text(
                                                text = if (documentName.isNotEmpty()) documentName else "Lampirkan PDF/Gambar (Maks. 10MB)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = if (documentName.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (documentName.isNotEmpty()) {
                                        IconButton(onClick = {
                                            documentPath = ""
                                            documentName = ""
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete attachment", tint = Color.Red)
                                        }
                                    } else {
                                        Button(
                                            onClick = { documentPickerLauncher.launch("*/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Unggahi", color = Color.White, style = MaterialTheme.typography.labelSmall)
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
                            // AI Assistant Button within create dialog
                            Button(
                                onClick = {
                                    if (title.isBlank() || date.isBlank()) {
                                        ToastUtils.show(context, "Perihal dan Tanggal harus terisi terlebih dahulu")
                                    } else {
                                        viewModel.generateInvitationDraft(
                                            title, date, time, location, agenda, recipientGroup
                                        ) { generated ->
                                            aiDraftText = generated
                                        }
                                        showAiResultDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Draf AI", color = Color.White)
                            }

                             Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        val newMeeting = Meeting(
                                            title = title,
                                            date = date,
                                            time = time,
                                            location = location,
                                            agenda = agenda,
                                            recipientGroup = recipientGroup,
                                            status = "DRAFT",
                                            documentPath = documentPath,
                                            documentName = documentName
                                        )
                                        viewModel.saveMeeting(newMeeting)
                                        showAddDialog = false
                                        // clear inputs
                                        title = ""
                                        date = ""
                                        time = ""
                                        location = ""
                                        agenda = ""
                                        documentPath = ""
                                        documentName = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Simpan", color = Color.White)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // DETAILS & EDIT DIALOG
            if (selectedMeetingForDetail != null) {
                val meeting = selectedMeetingForDetail!!
                var editMode by remember { mutableStateOf(false) }
                
                var dTitle by remember { mutableStateOf(meeting.title) }
                var dDate by remember { mutableStateOf(meeting.date) }
                var dTime by remember { mutableStateOf(meeting.time) }
                var dLocation by remember { mutableStateOf(meeting.location) }
                var dAgenda by remember { mutableStateOf(meeting.agenda) }
                var dGroup by remember { mutableStateOf(meeting.recipientGroup) }
                var dStatus by remember { mutableStateOf(meeting.status) }

                AlertDialog(
                    onDismissRequest = { selectedMeetingForDetail = null },
                    title = {
                        Text(
                            text = if (editMode) "Edit Detail Undangan" else "Detail Kegiatan Rapat",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (editMode) {
                                OutlinedTextField(
                                    value = dTitle,
                                    onValueChange = { dTitle = it },
                                    label = { Text("Perihal / Nama Rapat", fontWeight = FontWeight.Bold) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = dDate,
                                    onValueChange = { dDate = it },
                                    label = { Text("Hari, Tanggal", fontWeight = FontWeight.Bold) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = dTime,
                                    onValueChange = { dTime = it },
                                    label = { Text("Waktu Pelaksanaan", fontWeight = FontWeight.Bold) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = dLocation,
                                    onValueChange = { dLocation = it },
                                    label = { Text("Tempat / Ruangan", fontWeight = FontWeight.Bold) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                // Recipient Selection column with horizontal chips
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Ditujukan Kepada (Pilih Cepat & Mudah Dibaca):",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        recipientOptions.forEach { option ->
                                            val isSelected = dGroup == option
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { dGroup = option },
                                                label = {
                                                    Text(
                                                        text = option,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            )
                                        }
                                    }
                                    
                                    var dGroupExpanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = dGroup,
                                            onValueChange = { dGroup = it },
                                            label = { Text("Penerima Undangan (Kepada Yth)", fontWeight = FontWeight.Bold) },
                                            trailingIcon = {
                                                IconButton(onClick = { dGroupExpanded = !dGroupExpanded }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                }
                                            },
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        DropdownMenu(
                                            expanded = dGroupExpanded,
                                            onDismissRequest = { dGroupExpanded = false },
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            recipientOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option) },
                                                    onClick = {
                                                        dGroup = option
                                                        dGroupExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = dAgenda,
                                    onValueChange = { dAgenda = it },
                                    label = { Text("Agenda Rapat", fontWeight = FontWeight.Bold) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("Status:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    val statusOptions = listOf("DRAFT", "DIKIRIM", "SELESAI")
                                    statusOptions.forEach { opt ->
                                        ElevatedFilterChip(
                                            selected = dStatus == opt,
                                            onClick = { dStatus = opt },
                                            label = { Text(opt) }
                                        )
                                    }
                                }
                            } else {
                                InfoItem(label = "Perihal Rapat", value = meeting.title, icon = Icons.Default.Class)
                                InfoItem(label = "Hari, Tanggal", value = meeting.date, icon = Icons.Default.DateRange)
                                InfoItem(label = "Waktu pelaksanaan", value = meeting.time, icon = Icons.Default.AccessTime)
                                InfoItem(label = "Lokasi Ruangan", value = meeting.location, icon = Icons.Default.Place)
                                InfoItem(label = "Ditujukan Kepada", value = meeting.recipientGroup, icon = Icons.Default.People)
                                InfoItem(label = "Agenda Pembahasan", value = meeting.agenda, icon = Icons.Default.Subject)
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Status Kegiatan: ", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when(meeting.status) {
                                                    "DIKIRIM" -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                                                    "SELESAI" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    else -> Color(0xFFE65100).copy(alpha = 0.12f)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = meeting.status,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = when(meeting.status) {
                                                "DIKIRIM" -> Color(0xFF2E7D32)
                                                "SELESAI" -> MaterialTheme.colorScheme.primary
                                                else -> Color(0xFFE65100)
                                            }
                                        )
                                    }
                                }

                                // Document Display / Viewer attachment card
                                if (meeting.documentPath.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Dokumen Hardcopy Resmi:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = meeting.documentName.ifEmpty { "Lampiran_Undangan.pdf" },
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text("Jenis: Berkas Hardcopy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Button(
                                                onClick = {
                                                    try {
                                                        val viewIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(meeting.documentPath)).apply {
                                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                        }
                                                        context.startActivity(viewIntent)
                                                    } catch (e: Exception) {
                                                        ToastUtils.show(context, "Membuka Dokumen: ${meeting.documentName}")
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Buka", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                // Attendance/RSVP management details
                                val attendanceMap = remember(meeting.attendanceStatus) {
                                    parseAttendanceStatus(meeting.attendanceStatus)
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Text(
                                    text = "Kehadiran Anggota Dewan (RSVP):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                                
                                // Fetch eligible recipients to count stats
                                val eligibleRecipients = remember(meeting.recipientGroup, recipients) {
                                    if (meeting.recipientGroup == "Seluruh Anggota DPRD") {
                                        recipients.filter { it.role.contains("Anggota") || it.role.contains("Ketua") || it.role.contains("Pimpinan") }
                                    } else if (meeting.recipientGroup.contains("Komisi I")) {
                                        recipients.filter { it.role.contains("Komisi I") }
                                    } else if (meeting.recipientGroup.contains("Komisi II")) {
                                        recipients.filter { it.role.contains("Komisi II") }
                                    } else if (meeting.recipientGroup.contains("Komisi III")) {
                                        recipients.filter { it.role.contains("Komisi III") }
                                    } else if (meeting.recipientGroup.contains("Pimpinan")) {
                                        recipients.filter { it.role.contains("Ketua") || it.role.contains("Wakil") || it.role.contains("Pimpinan") }
                                    } else if (meeting.recipientGroup.contains("Fraksi")) {
                                        recipients.filter { it.role.contains("Fraksi") }
                                    } else {
                                        recipients
                                    }
                                }
                                val finalRecipients = if (eligibleRecipients.isEmpty()) recipients else eligibleRecipients
                                
                                val totalInvitees = finalRecipients.size
                                val hadirCount = finalRecipients.count { attendanceMap[it.id] == "Hadir" }
                                val izinCount = finalRecipients.count { attendanceMap[it.id] == "Izin" }
                                val sakitCount = finalRecipients.count { attendanceMap[it.id] == "Sakit" }
                                val absenCount = finalRecipients.count { attendanceMap[it.id] == "Absen" }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AttendanceBadge(label = "Hadir: $hadirCount", color = Color(0xFF2E7D32))
                                    AttendanceBadge(label = "Izin: $izinCount", color = Color(0xFFEF6C00))
                                    AttendanceBadge(label = "Sakit: $sakitCount", color = Color(0xFF1565C0))
                                    AttendanceBadge(label = "Absen: $absenCount", color = Color(0xFFC62828))
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Button(
                                    onClick = {
                                        showRsvpDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Input Kehadiran (RSVP)", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }

                                // AI output preview directly in details
                                if (meeting.aiSummary.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Ringkasan Notulen (Format AI):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = meeting.aiSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.heightIn(max = 120.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (editMode) {
                                Button(
                                    onClick = {
                                        viewModel.updateMeeting(
                                            meeting.copy(
                                                title = dTitle,
                                                date = dDate,
                                                time = dTime,
                                                location = dLocation,
                                                agenda = dAgenda,
                                                recipientGroup = dGroup,
                                                status = dStatus
                                            )
                                        )
                                        selectedMeetingForDetail = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Simpan Perubahan", color = Color.White)
                                }
                             } else {
                                if (userRole == "SEKWAN") {
                                    IconButton(onClick = {
                                        viewModel.deleteMeeting(meeting)
                                        selectedMeetingForDetail = null
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                                    }
                                }
                                
                                Button(
                                    onClick = {
                                        viewModel.generateInvitationDraft(
                                            meeting.title, meeting.date, meeting.time, meeting.location, meeting.agenda, meeting.recipientGroup
                                        ) { generated ->
                                            aiDraftText = generated
                                        }
                                        showAiResultDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lihat Draf AI", color = Color.White)
                                }

                                if (userRole == "SEKWAN") {
                                    Button(
                                        onClick = { editMode = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Edit", color = Color.White)
                                    }
                                }
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedMeetingForDetail = null }) {
                            Text("Tutup")
                        }
                    }
                )
            }

            // AI RESULT DRAFT DIALOG
            if (showAiResultDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showAiResultDialog = false
                        viewModel.clearAiResult()
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Draf Undangan Resmi AI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            val liveResult by viewModel.aiResult.collectAsState()
                            
                            if (isGeneratingState) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Desi sedang merumuskan surat undangan resmi...", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        Text(
                                            text = liveResult.ifEmpty { aiDraftText.ifEmpty { "Gagal menghasilkan draf surat. Periksa API key Anda." } },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledIconButton(
                                onClick = {
                                    val finalStr = viewModel.aiResult.value.ifEmpty { aiDraftText }
                                    if (finalStr.isNotEmpty()) {
                                        shareText(context, finalStr)
                                    }
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = Color.White)
                            }

                            Button(
                                onClick = {
                                    showAiResultDialog = false
                                    viewModel.clearAiResult()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Selesai", color = Color.White)
                            }
                        }
                    }
                )
            }

            // RSVP / ATTENDANCE STATUS DIALOG
            if (showRsvpDialog && selectedMeetingForDetail != null) {
                val meeting = selectedMeetingForDetail!!
                
                // Keep a local copy of attendance status being modified
                var localAttendanceMap by remember {
                    mutableStateOf(parseAttendanceStatus(meeting.attendanceStatus))
                }
                
                // Get general/matching recipients list
                val eligibleRecipients = remember(meeting.recipientGroup, recipients) {
                    if (meeting.recipientGroup == "Seluruh Anggota DPRD") {
                        recipients.filter { it.role.contains("Anggota") || it.role.contains("Ketua") || it.role.contains("Pimpinan") }
                    } else if (meeting.recipientGroup.contains("Komisi I")) {
                        recipients.filter { it.role.contains("Komisi I") }
                    } else if (meeting.recipientGroup.contains("Komisi II")) {
                        recipients.filter { it.role.contains("Komisi II") }
                    } else if (meeting.recipientGroup.contains("Komisi III")) {
                        recipients.filter { it.role.contains("Komisi III") }
                    } else if (meeting.recipientGroup.contains("Pimpinan")) {
                        recipients.filter { it.role.contains("Ketua") || it.role.contains("Wakil") || it.role.contains("Pimpinan") }
                    } else if (meeting.recipientGroup.contains("Fraksi")) {
                        recipients.filter { it.role.contains("Fraksi") }
                    } else {
                        recipients
                    }
                }
                val finalRecipients = if (eligibleRecipients.isEmpty()) recipients else eligibleRecipients

                AlertDialog(
                    onDismissRequest = { showRsvpDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Manajemen Kehadiran",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                        ) {
                            Text(
                                text = meeting.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1
                            )
                            Text(
                                text = "Grup Sasaran: ${meeting.recipientGroup}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            if (finalRecipients.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.AccountBox, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Belum ada data Kontak Penerima", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(finalRecipients) { recipient ->
                                        val currentStatus = localAttendanceMap[recipient.id] ?: "Belum"
                                        
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = recipient.name,
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                        Text(
                                                            text = "${recipient.role} • ${recipient.partyOrFaction}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                // RSVP option Chips row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    val options = listOf("Hadir", "Izin", "Sakit", "Absen")
                                                    options.forEach { opt ->
                                                        val isSelected = currentStatus == opt
                                                        val color = when (opt) {
                                                            "Hadir" -> Color(0xFF2E7D32)
                                                            "Izin" -> Color(0xFFEF6C00)
                                                            "Sakit" -> Color(0xFF1565C0)
                                                            else -> Color(0xFFC62828)
                                                        }
                                                        
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isSelected) color else Color.Transparent)
                                                                .border(1.dp, if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                                .clickable {
                                                                    val newMap = localAttendanceMap.toMutableMap()
                                                                    if (isSelected) {
                                                                        newMap.remove(recipient.id)
                                                                    } else {
                                                                        newMap[recipient.id] = opt
                                                                    }
                                                                    localAttendanceMap = newMap
                                                                }
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = opt,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val serialized = serializeAttendanceStatus(localAttendanceMap)
                                val updatedM = meeting.copy(attendanceStatus = serialized)
                                viewModel.updateMeeting(updatedM)
                                // Also update our selected detail state so that statistics are immediately updated in real-time in the detail screen
                                selectedMeetingForDetail = updatedM
                                showRsvpDialog = false
                                ToastUtils.show(context, "Status kehadiran berhasil disimpan & disinkronkan.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Simpan", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRsvpDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun InvitationItemCard(
    meeting: Meeting,
    onClick: () -> Unit,
    onShare: () -> Unit
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
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
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
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when(meeting.status) {
                                "DIKIRIM" -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                                "SELESAI" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else -> Color(0xFFE65100).copy(alpha = 0.12f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = meeting.status,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when(meeting.status) {
                            "DIKIRIM" -> Color(0xFF2E7D32)
                            "SELESAI" -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFFE65100)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = meeting.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconTextRow(icon = Icons.Default.DateRange, label = "${meeting.date} • ${meeting.time}")
                IconTextRow(icon = Icons.Default.Place, label = meeting.location)
                if (meeting.agenda.isNotEmpty()) {
                    IconTextRow(icon = Icons.Default.Subject, label = meeting.agenda)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.RemoveRedEye, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Detail")
                }

                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kirim Undangan", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun IconTextRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}

@Composable
fun InfoItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = value.ifEmpty { "-" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun shareText(context: Context, text: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Kirim undangan melalui:")
    context.startActivity(shareIntent)
}
