package com.example.ui.screens

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
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.DatePeriodFilter
import com.example.ui.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodicReportScreen(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val selectedProjId by viewModel.selectedProjectId.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val materials by viewModel.filteredMaterials.collectAsState()
    val laborWages by viewModel.filteredLaborWages.collectAsState()
    val kpi by viewModel.dashboardKpi.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Material, 1: Upah, 2: Ringkasan
    var showExportDialog by remember { mutableStateOf(false) }

    val currentProjectName = remember(projects, selectedProjId) {
        if (selectedProjId == null) "Semua Proyek"
        else projects.firstOrNull { it.id == selectedProjId }?.name ?: "Proyek"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("periodic_report_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Project selector chips
        item {
            ProjectSelectorChipRow(
                projects = projects,
                selectedProjectId = selectedProjId,
                onSelectProject = { viewModel.selectProject(it) }
            )
        }

        // Period filter chips & Export button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DatePeriodFilter.values().forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { viewModel.setPeriodFilter(period) },
                            label = { Text(period.label, fontSize = 11.sp) }
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { showExportDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("export_report_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekspor", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Header summary card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BlueprintNavy)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Laporan: $currentProjectName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Periode: ${selectedPeriod.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Material (${materials.size})", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            Text(formatRupiah(kpi.totalMaterialCost), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SafetyAmber)
                        }
                        Column {
                            Text("Upah (${laborWages.size})", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            Text(formatRupiah(kpi.totalLaborCost), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Periode", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            Text(formatRupiah(kpi.totalOverallCost), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Tab Row
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Material (${materials.size})", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Upah (${laborWages.size})", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("RAB & Biaya", fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Material list
                if (materials.isEmpty()) {
                    item {
                        EmptyStateCard("Tidak ada pemakaian material pada periode ini.")
                    }
                } else {
                    items(materials) { mat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mat.materialName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${mat.category} • ${formatDate(mat.date)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = formatRupiah(mat.totalCost),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Volume: ${mat.quantity} ${mat.unit} @ ${formatRupiah(mat.unitPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AssistChip(
                                            onClick = { },
                                            label = { Text(mat.source, fontSize = 10.sp) },
                                            modifier = Modifier.height(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteMaterial(mat) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Hapus",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if (mat.supplier.isNotBlank() || mat.invoiceOrNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Toko: ${mat.supplier.ifBlank { "-" }} • Catatan: ${mat.invoiceOrNote.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Labor Wages list
                if (laborWages.isEmpty()) {
                    item {
                        EmptyStateCard("Tidak ada catatan upah tenaga kerja pada periode ini.")
                    }
                } else {
                    items(laborWages) { wage ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${wage.workerRole} (${wage.workerCount} orang)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Mandor: ${wage.foremanName.ifBlank { "Pengawas" }} • ${formatDate(wage.date)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = formatRupiah(wage.totalWage),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryLight
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${wage.durationHOK} HOK @ ${formatRupiah(wage.wagePerUnit)}/org",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AssistChip(
                                            onClick = { },
                                            label = { Text(wage.source, fontSize = 10.sp) },
                                            modifier = Modifier.height(26.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteLaborWage(wage) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Hapus",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if (wage.taskDescription.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tugas: ${wage.taskDescription}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Financial summary & breakdown
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Komparasi RAB vs Realisasi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                SummaryRow("Total RAB Proyek:", formatRupiah(kpi.totalRAB))
                                SummaryRow("Pengeluaran Material:", formatRupiah(kpi.totalMaterialCost))
                                SummaryRow("Pengeluaran Upah Pekerja:", formatRupiah(kpi.totalLaborCost))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                SummaryRow("Total Terpakai:", formatRupiah(kpi.totalOverallCost), isBold = true)
                                val sisa = kpi.totalRAB - kpi.totalOverallCost
                                SummaryRow(
                                    "Sisa Anggaran Tersisa:",
                                    formatRupiah(sisa),
                                    isBold = true,
                                    valueColor = if (sisa >= 0) SuccessGreen else AlertRed
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Distribusi Pengeluaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                val total = kpi.totalOverallCost.coerceAtLeast(1.0)
                                val matPercent = (kpi.totalMaterialCost / total) * 100
                                val wagePercent = (kpi.totalLaborCost / total) * 100
                                SummaryRow("Porsi Biaya Material:", "${String.format("%.1f", matPercent)}%")
                                SummaryRow("Porsi Biaya Upah Tukang:", "${String.format("%.1f", wagePercent)}%")
                                SummaryRow("Rata-rata Progress Fisik:", "${String.format("%.1f", kpi.avgProgressPercent)}%")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        val reportContent = buildString {
            append("📋 LAPORAN REKAP PEMAKAIAN MATERIAL & UPAH\n")
            append("Proyek: $currentProjectName\n")
            append("Periode: ${selectedPeriod.label}\n")
            append("Waktu Ekspor: ${formatDate(System.currentTimeMillis())}\n")
            append("------------------------------------------\n\n")

            append("🧱 1. REKAP PEMAKAIAN MATERIAL (${materials.size} item):\n")
            if (materials.isEmpty()) append("- Tidak ada pemakaian material pada periode ini.\n")
            else {
                materials.forEachIndexed { i, m ->
                    append("${i + 1}. ${m.materialName} (${m.quantity} ${m.unit} @ ${formatRupiah(m.unitPrice)}) = ${formatRupiah(m.totalCost)}\n")
                    if (m.invoiceOrNote.isNotBlank()) append("   Ket: ${m.invoiceOrNote}\n")
                }
            }
            append(">> Subtotal Material: ${formatRupiah(kpi.totalMaterialCost)}\n\n")

            append("👷 2. REKAP UPAH TENAGA KERJA (${laborWages.size} kelompok):\n")
            if (laborWages.isEmpty()) append("- Tidak ada catatan upah pada periode ini.\n")
            else {
                laborWages.forEachIndexed { i, w ->
                    append("${i + 1}. ${w.workerRole} (${w.workerCount} org, ${w.durationHOK} HOK) = ${formatRupiah(w.totalWage)}\n")
                    if (w.taskDescription.isNotBlank()) append("   Pekerjaan: ${w.taskDescription}\n")
                }
            }
            append(">> Subtotal Upah: ${formatRupiah(kpi.totalLaborCost)}\n\n")

            append("------------------------------------------\n")
            append("💰 TOTAL PENGELUARAN AKTUAL: ${formatRupiah(kpi.totalOverallCost)}\n")
            append("📊 TOTAL RAB DISETUJUI: ${formatRupiah(kpi.totalRAB)}\n")
            append("🏗️ PROGRESS FISIK: ${String.format("%.1f", kpi.avgProgressPercent)}%\n")
            append("------------------------------------------\n")
            append("Dibuat otomatis via Aplikasi Rekap Proyek AI (Bot Telegram & Armbian Server)")
        }

        ExportReportDialog(
            reportText = reportContent,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
