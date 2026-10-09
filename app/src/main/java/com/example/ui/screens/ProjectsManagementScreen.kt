package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CreateProjectDialog
import com.example.ui.components.formatDateShort
import com.example.ui.components.formatRupiah
import com.example.ui.theme.SafetyAmber
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.ProjectViewModel

@Composable
fun ProjectsManagementScreen(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_project_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Proyek")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("projects_management_screen"),
            contentPadding = PaddingValues(16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daftar Proyek Pembangunan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kelola progress dan anggaran RAB per proyek",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Badge { Text("${projects.size} Proyek") }
                }
            }

            items(projects) { project ->
                var tempProgress by remember(project.progressPercent) { mutableStateOf(project.progressPercent) }
                var isModified by remember(tempProgress, project.progressPercent) {
                    mutableStateOf(tempProgress != project.progressPercent)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${project.codeSpk} • ${project.location}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AssistChip(
                                onClick = { },
                                label = { Text(project.status, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = if (project.status == "Aktif") SuccessGreen else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Anggaran RAB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatRupiah(project.budgetRAB), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Mandor Lapangan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(project.foremanInCharge.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Target Selesai", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatDateShort(project.targetEndDate), style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progress Fisik Lapangan:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${String.format("%.1f", tempProgress)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = tempProgress,
                            onValueChange = { tempProgress = it },
                            valueRange = 0f..100f,
                            steps = 199,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isModified) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.updateProgress(project.id, tempProgress)
                                    },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Simpan Progress", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, codeSpk, location, client, budget, foreman ->
                viewModel.createProject(name, codeSpk, location, client, budget, foreman)
                showCreateDialog = false
            }
        )
    }
}
