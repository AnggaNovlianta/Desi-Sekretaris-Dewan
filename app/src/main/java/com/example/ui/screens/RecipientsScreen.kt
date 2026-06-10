package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.ui.ToastUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.Recipient
import com.example.data.ChatMessage
import com.example.ui.viewmodel.DesiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientsScreen(
    viewModel: DesiViewModel,
    recipients: List<Recipient>
) {
    val context = LocalContext.current
    val userRole by viewModel.userRole.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedRecipientForDetail by remember { mutableStateOf<Recipient?>(null) }
    var activeChatRecipient by remember { mutableStateOf<Recipient?>(null) }

    // Forms
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var faction by remember { mutableStateOf("") }

    val roleOptions = listOf(
        "Ketua DPRD",
        "Wakil Ketua DPRD",
        "Pimpinan Komisi",
        "Anggota Komisi I",
        "Anggota Komisi II",
        "Anggota Komisi III",
        "Walikota Prabumulih",
        "Sekretaris Daerah",
        "Kepala Dinas",
        "Anggota Fraksi DPRD",
        "Lainnya"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Penerima & Kontak", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                actions = {
                    if (userRole == "SEKWAN") {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact", tint = Color.White)
                        }
                    } else {
                        // Real-time synchronization indicator
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
        var isSyncing by remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                isSyncing = true
                viewModel.syncDeviceContacts(
                    context = context,
                    onSuccess = { count ->
                        isSyncing = false
                        if (count > 0) {
                            ToastUtils.show(context, "Selesai! $count kontak baru berhasil disinkronkan!")
                        } else {
                            ToastUtils.show(context, "Kontak handphone sudah up-to-date!")
                        }
                    },
                    onFailure = { err ->
                        isSyncing = false
                        ToastUtils.show(context, "Gagal sinkron: $err")
                    }
                )
            } else {
                ToastUtils.show(context, "Izin membaca kontak ditolak. Silahkan aktifkan izin di pengaturan.")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sinkronisasi Kontak HP",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Otomatis mengimpor nama & nomor telepon pimpinan dewan dari HP Anda ke DESI.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Button(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_CONTACTS
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (hasPermission) {
                                    isSyncing = true
                                    viewModel.syncDeviceContacts(
                                        context = context,
                                        onSuccess = { count ->
                                            isSyncing = false
                                            if (count > 0) {
                                                ToastUtils.show(context, "Selesai! $count kontak baru disinkronkan!")
                                            } else {
                                                ToastUtils.show(context, "Kontak handphone sudah up-to-date!")
                                            }
                                        },
                                        onFailure = { err ->
                                            isSyncing = false
                                            ToastUtils.show(context, "Gagal sinkron: $err")
                                        }
                                    )
                                } else {
                                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("Sync", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
            if (recipients.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = "Empty Contacts",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Kontak belum terdaftar",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tambahkan pimpinan komisi, fraksi, kepala dinas, atau pejabat pemerintah Kota Prabumulih di sini.",
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
                            Text("Tambah Kontak Baru", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(recipients) { recipient ->
                        RecipientItemCard(
                            recipient = recipient,
                            onClick = { selectedRecipientForDetail = recipient },
                            onChatClick = { activeChatRecipient = recipient }
                        )
                    }
                }
            }
        }

            // ADD RECIPIENT DIALOG
            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("Tambah Kontak Baru", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nama Lengkap & Gelar") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            var roleExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = role,
                                    onValueChange = { role = it },
                                    label = { Text("Jabatan / Peran") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                    trailingIcon = {
                                        IconButton(onClick = { roleExpanded = !roleExpanded }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = roleExpanded,
                                    onDismissRequest = { roleExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    roleOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                role = opt
                                                roleExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("No. HP / WhatsApp (e.g. 0812...)") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = faction,
                                onValueChange = { faction = it },
                                label = { Text("Fraksi / Partai / Dinas (Optional)") },
                                leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (name.isNotBlank() && role.isNotBlank()) {
                                    val r = Recipient(
                                        name = name,
                                        role = role,
                                        phoneNumber = phone,
                                        partyOrFaction = faction
                                    )
                                    viewModel.saveRecipient(r)
                                    showAddDialog = false
                                    // Reset fields
                                    name = ""
                                    role = ""
                                    phone = ""
                                    faction = ""
                                    ToastUtils.show(context, "Kontak berhasil ditambahkan")
                                } else {
                                    ToastUtils.show(context, "Nama dan Jabatan wajib diisi")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Simpan", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }

            // RECIPIENT DETAIL & DELETE DIALOG
            if (selectedRecipientForDetail != null) {
                val r = selectedRecipientForDetail!!
                
                AlertDialog(
                    onDismissRequest = { selectedRecipientForDetail = null },
                    title = { Text(r.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InfoItem(label = "Jabatan", value = r.role, icon = Icons.Default.Badge)
                            InfoItem(label = "WhatsApp / No. HP", value = r.phoneNumber.ifEmpty { "Belum terisi" }, icon = Icons.Default.Phone)
                            InfoItem(label = "Instansi / Fraksi", value = r.partyOrFaction.ifEmpty { "Umum / Pemerintahan" }, icon = Icons.Default.Apartment)
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
                                        viewModel.deleteRecipient(r)
                                        selectedRecipientForDetail = null
                                        ToastUtils.show(context, "Kontak dihapus")
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus Kontak", tint = Color.Red)
                                }
                            }

                            Button(
                                onClick = { selectedRecipientForDetail = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Kembali", color = Color.White)
                            }
                        }
                    }
                )
            }

            // INTERACTIVE CHAT DIALOG
            if (activeChatRecipient != null) {
                InteractiveChatDialog(
                    recipient = activeChatRecipient!!,
                    viewModel = viewModel,
                    onDismiss = { activeChatRecipient = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveChatDialog(
    recipient: Recipient,
    viewModel: DesiViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val userEmail by viewModel.currentUserEmail.collectAsState()
    val rawEmail = userEmail ?: "sekwan@desi.go.id"
    val chatMessages by viewModel.chatMessagesState.collectAsState()
    
    val activeChatMessages = remember(chatMessages, recipient.phoneNumber, rawEmail) {
        chatMessages.filter { msg ->
            (msg.senderEmail == rawEmail && msg.recipientPhoneOrGroup == recipient.phoneNumber) ||
            (msg.senderEmail == "peer_reply@desi.go.id" && msg.recipientPhoneOrGroup == rawEmail)
        }
    }

    var messageText by remember { mutableStateOf("") }
    var attachedDocPath by remember { mutableStateOf("") }
    var attachedDocName by remember { mutableStateOf("") }

    val chatDocPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachedDocPath = uri.toString()
            attachedDocName = "File_" + (uri.lastPathSegment?.substringAfterLast("/") ?: "Lampiran.pdf")
            ToastUtils.show(context, "Dokumen dilampirkan ke obrolan!")
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(activeChatMessages.size) {
        if (activeChatMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeChatMessages.size - 1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f),
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            // Avatar Icon
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(19.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = recipient.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                // Online indicator dot
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF2E7D32), RoundedCornerShape(5.dp))
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(5.dp))
                                        .align(Alignment.BottomEnd)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(recipient.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${recipient.role} • Online",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    // Chat messages area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp)
                    ) {
                        if (activeChatMessages.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Mulai Chat Aman DESI",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Text(
                                    "Kirimkan pesan langsung ke gawai para pimpinan anggota dewan. Chat otomatis memicu notifikasi push.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(activeChatMessages) { msg ->
                                    val isMe = msg.senderEmail == rawEmail
                                    val alignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                                    val bubbleBg = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    val contentColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth(0.82f)
                                                .background(
                                                    color = bubbleBg,
                                                    shape = RoundedCornerShape(
                                                        topStart = 14.dp,
                                                        topEnd = 14.dp,
                                                        bottomStart = if (isMe) 14.dp else 2.dp,
                                                        bottomEnd = if (isMe) 2.dp else 14.dp
                                                    )
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isMe) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            // Handle file attachment inside chat bubble
                                            if (msg.attachmentPath.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (isMe) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            try {
                                                                val viewIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(msg.attachmentPath)).apply {
                                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                                }
                                                                context.startActivity(viewIntent)
                                                            } catch (e: Exception) {
                                                                ToastUtils.show(context, "Membuka: ${msg.attachmentName}")
                                                            }
                                                        }
                                                        .padding(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.FilePresent, contentDescription = null, tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = msg.attachmentName.ifEmpty { "Lampiran_Berkas.pdf" },
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                            
                                            Text(
                                                text = msg.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = contentColor
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(msg.timestamp)),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Attached view overlay
                    if (attachedDocName.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = attachedDocName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = {
                                attachedDocPath = ""
                                attachedDocName = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Color.Red)
                            }
                        }
                    }

                    // Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { chatDocPickerLauncher.launch("*/*") }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Attach file", tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Ketik pesan pengganti WA...", style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 80.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            ),
                            shape = RoundedCornerShape(20.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank() || attachedDocPath.isNotEmpty()) {
                                    viewModel.sendChatMessage(
                                        recipientPhoneOrGroup = recipient.phoneNumber,
                                        messageText = messageText,
                                        attachmentPath = attachedDocPath,
                                        attachmentName = attachedDocName,
                                        peerName = recipient.name,
                                        peerRole = recipient.role
                                    )
                                    messageText = ""
                                    attachedDocPath = ""
                                    attachedDocName = ""
                                }
                            },
                            enabled = messageText.isNotBlank() || attachedDocPath.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (messageText.isNotBlank() || attachedDocPath.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun RecipientItemCard(
    recipient: Recipient,
    onClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = recipient.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${recipient.role} ${if (recipient.partyOrFaction.isNotEmpty()) "• " + recipient.partyOrFaction else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (recipient.phoneNumber.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onChatClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.PhoneEnabled,
                        contentDescription = "Active Contact",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
