package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.screens.AgendaScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvitationsScreen
import com.example.ui.screens.MinutesScreen
import com.example.ui.screens.RecipientsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DesiViewModel
import com.example.ui.viewmodel.DesiViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.NotificationHelper.createNotificationChannel(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Initialize Room DB & Repository
                val database = recuerDatabaseInstance()
                val repository = remember {
                    AppRepository(database.meetingDao(), database.recipientDao())
                }
                
                // Set Up ViewModel using custom Factory
                val factory = DesiViewModelFactory(application, repository)
                val viewModel: DesiViewModel = viewModel(factory = factory)

                // App Navigation State
                var currentTab by remember { mutableStateOf(0) }
                // Deep link state from Dashboard to Autostart Create invitation
                var autoCreateInvitation by remember { mutableStateOf(false) }

                // Collect Streams
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val meetings by viewModel.meetingsState.collectAsState()
                val recipients by viewModel.recipientsState.collectAsState()

                if (!isLoggedIn) {
                    com.example.ui.screens.AuthScreen(viewModel = viewModel)
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dasbor") },
                                label = { Text("Dasbor") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Agenda") },
                                label = { Text("Agenda") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Default.Email, contentDescription = "Undangan") },
                                label = { Text("Undangan") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Default.Assignment, contentDescription = "Notulen") },
                                label = { Text("Notulen") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 4,
                                onClick = { currentTab = 4 },
                                icon = { Icon(Icons.Default.People, contentDescription = "Penerima") },
                                label = { Text("Kontak") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            0 -> DashboardScreen(
                                viewModel = viewModel,
                                meetings = meetings,
                                recipientsCount = recipients.size,
                                onNavigateToInvitations = { currentTab = 2 },
                                onNavigateToMinutes = { currentTab = 3 },
                                onNavigateToRecipients = { currentTab = 4 },
                                onCreateInvitation = {
                                    autoCreateInvitation = true
                                    currentTab = 2
                                }
                            )
                            1 -> AgendaScreen(
                                viewModel = viewModel,
                                meetings = meetings
                            )
                            2 -> InvitationsScreen(
                                viewModel = viewModel,
                                meetings = meetings,
                                recipients = recipients,
                                isAddingInitial = autoCreateInvitation,
                                onResetAddFlag = { autoCreateInvitation = false }
                            )
                            3 -> MinutesScreen(
                                viewModel = viewModel,
                                meetings = meetings
                            )
                            4 -> RecipientsScreen(
                                viewModel = viewModel,
                                recipients = recipients
                            )
                        }
                    }
                }
                }
            }
        }
    }

    @Composable
    private fun recuerDatabaseInstance(): AppDatabase {
        val context = androidx.compose.ui.platform.LocalContext.current
        return remember { AppDatabase.getDatabase(context) }
    }
}
