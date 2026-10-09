package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AppUserEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.ProjectViewModel

@Composable
fun UserManagementScreen(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.users.collectAsState()
    val projects by viewModel.projects.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<AppUserEntity?>(null) }
    var userToDelete by remember { mutableStateOf<AppUserEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("SEMUA") }

    val rolesList = listOf("SEMUA", "Admin", "Project Manager", "Estimator/QS", "Mandor/Pengawas", "Logistik")

    val filteredUsers = remember(users, searchQuery, selectedRoleFilter) {
        users.filter { user ->
            (selectedRoleFilter == "SEMUA" || user.role.equals(selectedRoleFilter, ignoreCase = true)) &&
                    (searchQuery.isBlank() || user.fullName.contains(searchQuery, ignoreCase = true) || user.username.contains(searchQuery, ignoreCase = true) || user.role.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Tambah Pengguna", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_user_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("user_management_screen"),
            contentPadding = PaddingValues(16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueprintNavy),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, GlassCardBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HeroGradient)
                            .padding(18.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(SafetyAmber),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = BlueprintNavy, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Manajemen Pengguna & Tim",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Kontrol Hak Akses & Personil Proyek",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0x33F59E0B),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${users.size} PENGGUNA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SafetyAmber,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar & Filter Roles
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_user_input"),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Cari nama, username, atau posisi tim...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null,
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rolesList.take(3).forEach { role ->
                            FilterChip(
                                selected = selectedRoleFilter == role,
                                onClick = { selectedRoleFilter = role },
                                label = { Text(role, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rolesList.drop(3).forEach { role ->
                            FilterChip(
                                selected = selectedRoleFilter == role,
                                onClick = { selectedRoleFilter = role },
                                label = { Text(role, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Users List
            if (filteredUsers.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.GroupOff,
                        message = "Tidak ada pengguna ditemukan dengan kriteria tersebut."
                    )
                }
            } else {
                items(filteredUsers) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (user.role) {
                                                    "Admin" -> SafetyAmber.copy(alpha = 0.2f)
                                                    "Project Manager" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                    "Estimator/QS" -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                                    "Mandor/Pengawas" -> SuccessGreen.copy(alpha = 0.2f)
                                                    else -> Color(0xFF06B6D4).copy(alpha = 0.2f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.fullName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = BlueprintNavy
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = user.fullName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "@${user.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = when (user.role) {
                                        "Admin" -> SafetyAmber.copy(alpha = 0.15f)
                                        "Project Manager" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                        "Estimator/QS" -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                        "Mandor/Pengawas" -> SuccessGreen.copy(alpha = 0.15f)
                                        else -> Color(0xFF06B6D4).copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = user.role,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (user.role) {
                                            "Admin" -> SafetyAmberDark
                                            "Project Manager" -> Color(0xFF1D4ED8)
                                            "Estimator/QS" -> Color(0xFF6D28D9)
                                            "Mandor/Pengawas" -> Color(0xFF047857)
                                            else -> Color(0xFF0E7490)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (user.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(user.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (user.assignedProject.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(user.assignedProject, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { userToEdit = user },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { userToDelete = user },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = AlertRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add User Dialog
    if (showCreateDialog) {
        UserFormDialog(
            title = "Tambah Pengguna Baru",
            initialUsername = "",
            initialFullName = "",
            initialRole = "Mandor/Pengawas",
            initialEmail = "",
            initialPhone = "",
            initialAssignedProject = "Semua Proyek",
            projectOptions = projects.map { it.name },
            onDismiss = { showCreateDialog = false },
            onConfirm = { username, fullName, role, email, phone, proj ->
                viewModel.createUser(username, fullName, role, email, phone, proj)
                showCreateDialog = false
            }
        )
    }

    // Edit User Dialog
    userToEdit?.let { user ->
        UserFormDialog(
            title = "Edit Data Pengguna",
            initialUsername = user.username,
            initialFullName = user.fullName,
            initialRole = user.role,
            initialEmail = user.email,
            initialPhone = user.phone,
            initialAssignedProject = user.assignedProject,
            projectOptions = projects.map { it.name },
            onDismiss = { userToEdit = null },
            onConfirm = { username, fullName, role, email, phone, proj ->
                val updated = user.copy(
                    username = username,
                    fullName = fullName,
                    role = role,
                    email = email,
                    phone = phone,
                    assignedProject = proj
                )
                viewModel.updateUser(updated)
                userToEdit = null
            }
        )
    }

    // Delete User Dialog
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Hapus Pengguna?", fontWeight = FontWeight.Bold) },
            text = { Text("Anda yakin ingin menghapus '${user.fullName}' (@${user.username})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(user)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun UserFormDialog(
    title: String,
    initialUsername: String,
    initialFullName: String,
    initialRole: String,
    initialEmail: String,
    initialPhone: String,
    initialAssignedProject: String,
    projectOptions: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (username: String, fullName: String, role: String, email: String, phone: String, assignedProject: String) -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var fullName by remember { mutableStateOf(initialFullName) }
    var role by remember { mutableStateOf(initialRole) }
    var email by remember { mutableStateOf(initialEmail) }
    var phone by remember { mutableStateOf(initialPhone) }
    var assignedProject by remember { mutableStateOf(initialAssignedProject) }

    val roles = listOf("Admin", "Project Manager", "Estimator/QS", "Mandor/Pengawas", "Logistik")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("user_form_dialog"),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nama Lengkap") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("Peran / Posisi:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    roles.take(3).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 10.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    roles.drop(3).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 10.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. HP / WhatsApp") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (fullName.isNotBlank() && username.isNotBlank()) {
                                onConfirm(username, fullName, role, email, phone, assignedProject)
                            }
                        },
                        enabled = fullName.isNotBlank() && username.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
