package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddLaborDialog
import com.example.ui.components.AddMaterialDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppNavScreen
import com.example.ui.viewmodel.ProjectViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ProjectViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val statusMessage by viewModel.statusMessage.collectAsState()
                val projects by viewModel.projects.collectAsState()
                val selectedProjId by viewModel.selectedProjectId.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }

                var showAddMaterialDialog by remember { mutableStateOf(false) }
                var showAddLaborDialog by remember { mutableStateOf(false) }

                // Display snackbar whenever statusMessage updates
                LaunchedEffect(statusMessage) {
                    statusMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearStatusMessage()
                    }
                }

                // Handle back button for secondary screens
                BackHandler(enabled = currentScreen != AppNavScreen.DASHBOARD) {
                    viewModel.setNavScreen(AppNavScreen.DASHBOARD)
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(SafetyAmber),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Construction,
                                            contentDescription = null,
                                            tint = BlueprintNavy,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Rekap Proyek AI",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = when (currentScreen) {
                                                AppNavScreen.DASHBOARD -> "Dashboard Realisasi & Progress"
                                                AppNavScreen.LAPORAN -> "Laporan Pemakaian & Upah"
                                                AppNavScreen.RAB_EXPLORER -> "RAB Estimate Engineer (EE)"
                                                AppNavScreen.TELEGRAM_HUB -> "Integrasi Bot Telegram & AI"
                                                AppNavScreen.AI_ANALYSIS -> "Otak AI Analisis Finansial"
                                                AppNavScreen.PROYEK -> "Kelola Proyek Pembangunan"
                                                AppNavScreen.MANAJEMEN_USER -> "Manajemen Pengguna & Tim"
                                                AppNavScreen.ARMBIAN_SYNC -> "Server Armbian MySQL"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            },
                            actions = {
                                // User management quick button
                                IconButton(
                                    onClick = { viewModel.setNavScreen(AppNavScreen.MANAJEMEN_USER) },
                                    modifier = Modifier.testTag("top_user_management")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ManageAccounts,
                                        contentDescription = "Manajemen Pengguna",
                                        tint = if (currentScreen == AppNavScreen.MANAJEMEN_USER) SafetyAmberDark else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                // Armbian quick button
                                IconButton(
                                    onClick = { viewModel.setNavScreen(AppNavScreen.ARMBIAN_SYNC) },
                                    modifier = Modifier.testTag("top_armbian_settings")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dns,
                                        contentDescription = "Server Armbian MySQL",
                                        tint = if (currentScreen == AppNavScreen.ARMBIAN_SYNC) SafetyAmberDark else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == AppNavScreen.DASHBOARD,
                                onClick = { viewModel.setNavScreen(AppNavScreen.DASHBOARD) },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_dashboard")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppNavScreen.RAB_EXPLORER,
                                onClick = { viewModel.setNavScreen(AppNavScreen.RAB_EXPLORER) },
                                icon = { Icon(Icons.Default.AccountBalance, contentDescription = "RAB EE") },
                                label = { Text("RAB EE", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_rab_explorer")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppNavScreen.LAPORAN,
                                onClick = { viewModel.setNavScreen(AppNavScreen.LAPORAN) },
                                icon = { Icon(Icons.Default.Assessment, contentDescription = "Laporan") },
                                label = { Text("Laporan", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_laporan")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppNavScreen.TELEGRAM_HUB,
                                onClick = { viewModel.setNavScreen(AppNavScreen.TELEGRAM_HUB) },
                                icon = { Icon(Icons.Default.SmartToy, contentDescription = "Bot AI") },
                                label = { Text("Bot AI", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_telegram")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppNavScreen.PROYEK,
                                onClick = { viewModel.setNavScreen(AppNavScreen.PROYEK) },
                                icon = { Icon(Icons.Default.HomeWork, contentDescription = "Proyek") },
                                label = { Text("Proyek", fontSize = 10.sp) },
                                modifier = Modifier.testTag("nav_proyek")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppNavScreen.DASHBOARD -> {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onOpenAddMaterial = { showAddMaterialDialog = true },
                                    onOpenAddLabor = { showAddLaborDialog = true }
                                )
                            }
                            AppNavScreen.RAB_EXPLORER -> {
                                RabExplorerScreen(viewModel = viewModel)
                            }
                            AppNavScreen.LAPORAN -> {
                                PeriodicReportScreen(viewModel = viewModel)
                            }
                            AppNavScreen.TELEGRAM_HUB -> {
                                TelegramHubScreen(viewModel = viewModel)
                            }
                            AppNavScreen.AI_ANALYSIS -> {
                                AiAnalysisScreen(viewModel = viewModel)
                            }
                            AppNavScreen.PROYEK -> {
                                ProjectsManagementScreen(viewModel = viewModel)
                            }
                            AppNavScreen.MANAJEMEN_USER -> {
                                UserManagementScreen(viewModel = viewModel)
                            }
                            AppNavScreen.ARMBIAN_SYNC -> {
                                ArmbianSyncScreen(viewModel = viewModel)
                            }
                        }
                    }
                }

                // Add Material Dialog
                if (showAddMaterialDialog) {
                    AddMaterialDialog(
                        projects = projects,
                        preselectedProjectId = selectedProjId,
                        onDismiss = { showAddMaterialDialog = false },
                        onConfirm = { projId, name, cat, qty, unit, price, supplier, note ->
                            viewModel.addManualMaterial(projId, name, cat, qty, unit, price, supplier, note)
                            showAddMaterialDialog = false
                        }
                    )
                }

                // Add Labor Dialog
                if (showAddLaborDialog) {
                    AddLaborDialog(
                        projects = projects,
                        preselectedProjectId = selectedProjId,
                        onDismiss = { showAddLaborDialog = false },
                        onConfirm = { projId, role, count, dur, rate, desc, foreman ->
                            viewModel.addManualLabor(projId, role, count, dur, rate, desc, foreman)
                            showAddLaborDialog = false
                        }
                    )
                }
            }
        }
    }
}
