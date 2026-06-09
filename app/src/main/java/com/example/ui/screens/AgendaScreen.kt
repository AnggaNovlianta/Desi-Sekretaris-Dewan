package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import com.example.ui.ToastUtils
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Meeting
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.DesiViewModel
import java.text.SimpleDateFormat
import java.util.*

object AgendaDateUtils {
    private val localeId = Locale("id", "ID")

    fun formatDate(calendar: Calendar): String {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", localeId)
        return sdf.format(calendar.time)
    }

    fun parseDate(dateStr: String): Date? {
        val formats = listOf(
            SimpleDateFormat("EEEE, d MMMM yyyy", localeId),
            SimpleDateFormat("EEEE, dd MMMM yyyy", localeId),
            SimpleDateFormat("d MMMM yyyy", localeId),
            SimpleDateFormat("dd MMMM yyyy", localeId),
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US)
        )
        for (fmt in formats) {
            try {
                return fmt.parse(dateStr)
            } catch (e: Exception) {
                // Ignore and try next
            }
        }
        return null
    }

    fun isToday(dateStr: String): Boolean {
        // Fallback check if simple string matching succeeds
        val todayCal = Calendar.getInstance()
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH).toString()
        val todayMonthName = SimpleDateFormat("MMMM", localeId).format(todayCal.time)
        val todayYear = todayCal.get(Calendar.YEAR).toString()
        
        if (dateStr.contains(todayMonthName, ignoreCase = true) && 
            dateStr.contains(todayDay) && 
            dateStr.contains(todayYear)) {
            return true
        }

        val parsedDate = parseDate(dateStr) ?: return false
        val target = Calendar.getInstance().apply { time = parsedDate }
        return target.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
               target.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    }

    fun isThisWeek(dateStr: String): Boolean {
        val parsedDate = parseDate(dateStr) ?: return false
        val target = Calendar.getInstance().apply { time = parsedDate }
        
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        
        val endOfWeek = Calendar.getInstance().apply {
            time = startOfWeek.time
            add(Calendar.DAY_OF_YEAR, 7)
        }
        
        return !target.before(startOfWeek) && target.before(endOfWeek)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    viewModel: DesiViewModel,
    meetings: List<Meeting>
) {
    val context = LocalContext.current
    val userRole by viewModel.userRole.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Hari Ini, 1: Pekan Ini, 2: Semua
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMeetingForDetail by remember { mutableStateOf<Meeting?>(null) }

    // Form states
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var agenda by remember { mutableStateOf("") }
    var recipientGroup by remember { mutableStateOf("Seluruh Anggota DPRD") }

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

    val commonLocations = listOf(
        "Ruang Rapat Paripurna",
        "Ruang Komisi I",
        "Ruang Komisi II",
        "Ruang Komisi III",
        "Ruang Rapat Pimpinan"
    )

    // Filtered lists
    val todayMeetings = remember(meetings) {
        meetings.filter { AgendaDateUtils.isToday(it.date) }
    }

    val weekMeetings = remember(meetings) {
        meetings.filter { AgendaDateUtils.isThisWeek(it.date) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Agenda Rapat & Kerja",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Sekretariat DPRD Kota Prabumulih",
                            style = MaterialTheme.typography.labelSmall.copy(color = GoldAccent),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                actions = {
                    if (userRole == "SEKWAN") {
                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("action_add_agenda")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah Agenda", tint = Color.White)
                        }
                    } else {
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
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_agenda")
                ) {
                    Icon(Icons.Default.AddHomeWork, contentDescription = "Buat Agenda")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Row Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Hari Ini (${todayMeetings.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Today, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pekan Ini (${weekMeetings.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Semua (${meetings.size})", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                )
            }

            // Current List selection
            val currentMeetingsList = when (selectedTab) {
                0 -> todayMeetings
                1 -> weekMeetings
                else -> meetings
            }

            if (currentMeetingsList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            0 -> Icons.Default.EventAvailable
                            1 -> Icons.Default.DateRange
                            else -> Icons.Default.EventNote
                        },
                        contentDescription = "Agenda Kosong",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (selectedTab) {
                            0 -> "Tidak ada agenda hari ini"
                            1 -> "Tidak ada agenda pekan ini"
                            else -> "Belum ada agenda terdaftar"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Silakan klik tombol '+' untuk menjadwalkan agenda kerja atau hearing baru dewan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                    if (userRole == "SEKWAN") {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("button_add_agenda_empty")
                        ) {
                            Text("Buat Agenda Sekarang", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentMeetingsList) { meeting ->
                        AgendaItemCard(
                            meeting = meeting,
                            onClick = { selectedMeetingForDetail = meeting }
                        )
                    }
                }
            }

            // ADD AGENDA DIALOG
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("Tambah Agenda Kegiatan Baru", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Perihal Rapat / Kegiatan") },
                                leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_agenda_title")
                            )

                            // Date Field utilizing DatePickerDialog
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = date,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Pilih Tanggal") },
                                    placeholder = { Text("Klik ikon kalender...") },
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
                                                        date = AgendaDateUtils.formatDate(selectedCal)
                                                    },
                                                    calendar.get(Calendar.YEAR),
                                                    calendar.get(Calendar.MONTH),
                                                    calendar.get(Calendar.DAY_OF_MONTH)
                                                )
                                                datePickerDialog.show()
                                            }
                                        ) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = "Buka Kalender")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_agenda_date")
                                )
                            }

                            // Time Field utilizing TimePickerDialog
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = time,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Waktu Pelaksanaan") },
                                    placeholder = { Text("Klik ikon jam...") },
                                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                val calendar = Calendar.getInstance()
                                                val timePickerDialog = TimePickerDialog(
                                                    context,
                                                    { _, hourOfDay, minute ->
                                                        time = String.format("%02d:%02d WIB", hourOfDay, minute)
                                                    },
                                                    calendar.get(Calendar.HOUR_OF_DAY),
                                                    calendar.get(Calendar.MINUTE),
                                                    true
                                                )
                                                timePickerDialog.show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Schedule, contentDescription = "Buka Jam")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_agenda_time")
                                )
                            }

                            // Location Field with suggestions
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("Tempat / Lokasi Ruangan") },
                                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_agenda_location")
                            )

                            // Quick Location Suggestion Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                commonLocations.take(3).forEach { room ->
                                    SuggestionChip(
                                        onClick = { location = room },
                                        label = { Text(room.replace("Ruang Rapat ", "R. "), fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Dictated / Targeted Recipient Group
                            var expandedGroup by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = recipientGroup,
                                    onValueChange = { recipientGroup = it },
                                    label = { Text("Ditujukan Kepada / Peserta") },
                                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { expandedGroup = !expandedGroup }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_agenda_group")
                                )
                                DropdownMenu(
                                    expanded = expandedGroup,
                                    onDismissRequest = { expandedGroup = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    recipientOptions.forEach { group ->
                                        DropdownMenuItem(
                                            text = { Text(group) },
                                            onClick = {
                                                recipientGroup = group
                                                expandedGroup = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Agenda Detail description
                            OutlinedTextField(
                                value = agenda,
                                onValueChange = { agenda = it },
                                label = { Text("Detail Bahasan / Pokok Acara") },
                                leadingIcon = { Icon(Icons.Default.Subject, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_agenda_notes"),
                                minLines = 2
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (title.isBlank() || date.isBlank() || time.isBlank() || location.isBlank()) {
                                    ToastUtils.show(context, "Silakan isi Perihal, Tanggal, Waktu, dan Ruangan")
                                } else {
                                    val newAgenda = Meeting(
                                        title = title,
                                        date = date,
                                        time = time,
                                        location = location,
                                        agenda = agenda,
                                        recipientGroup = recipientGroup,
                                        status = "DRAFT"
                                    )
                                    viewModel.saveMeeting(newAgenda)
                                    showAddDialog = false
                                    // Reset fields
                                    title = ""
                                    date = ""
                                    time = ""
                                    location = ""
                                    agenda = ""
                                    recipientGroup = "Seluruh Anggota DPRD"
                                    ToastUtils.show(context, "Agenda telah dijadwalkan")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("button_save_agenda")
                        ) {
                            Text("Jadwalkan", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // AGENDA DETAIL DIALOG
            if (selectedMeetingForDetail != null) {
                val meeting = selectedMeetingForDetail!!
                AlertDialog(
                    onDismissRequest = { selectedMeetingForDetail = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(meeting.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Tanggal & Waktu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text("${meeting.date} • ${meeting.time}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Ruangan / Tempat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(meeting.location, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Peserta Agenda", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(meeting.recipientGroup, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Subject, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Pokok Pembahasan / Deskripsi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(
                                        text = meeting.agenda.ifEmpty { "(Belum ada rincian bahasan)" },
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (userRole == "SEKWAN") {
                                IconButton(
                                    onClick = {
                                        viewModel.deleteMeeting(meeting)
                                        selectedMeetingForDetail = null
                                        ToastUtils.show(context, "Agenda telah dihapus")
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Agenda", tint = Color.Red)
                                }
                            }

                            Button(
                                onClick = { selectedMeetingForDetail = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Tutup", color = Color.White)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AgendaItemCard(
    meeting: Meeting,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("agenda_card_${meeting.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ditujukan: ${meeting.recipientGroup}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Tanggal",
                        tint = GoldAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = meeting.date,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1.3f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Waktu",
                        tint = GoldAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = meeting.time,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Tempat",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = meeting.location,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
