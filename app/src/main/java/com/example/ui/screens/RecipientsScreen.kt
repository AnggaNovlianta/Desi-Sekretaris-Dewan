package com.example.ui.screens

import android.widget.Toast
import com.example.ui.ToastUtils
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Recipient
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
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
                            onClick = { selectedRecipientForDetail = recipient }
                        )
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
        }
    }
}

@Composable
fun RecipientItemCard(
    recipient: Recipient,
    onClick: () -> Unit
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
