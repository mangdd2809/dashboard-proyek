package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
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
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }

    val currentProjectName = remember(projects, selectedProjId) {
        if (selectedProjId == null) "Semua Proyek"
        else projects.firstOrNull { it.id == selectedProjId }?.name ?: "Proyek"
    }

    val filteredMaterialsList = remember(materials, searchQuery, selectedCategoryFilter) {
        materials.filter { m ->
            val matchQuery = searchQuery.isBlank() || m.materialName.contains(searchQuery, ignoreCase = true) || m.supplier.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategoryFilter == "Semua" || m.category.contains(selectedCategoryFilter, ignoreCase = true)
            matchQuery && matchCategory
        }
    }

    val filteredLaborList = remember(laborWages, searchQuery) {
        laborWages.filter { w ->
            searchQuery.isBlank() || w.workerRole.contains(searchQuery, ignoreCase = true) || w.taskDescription.contains(searchQuery, ignoreCase = true) || w.foremanName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("periodic_report_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Project selector vertical list card
        item {
            ProjectSelectorVerticalList(
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
                            label = { Text(period.label, fontSize = 11.sp, fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Button(
                    onClick = { showExportDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueprintNavy),
                    modifier = Modifier.testTag("export_report_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekspor", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Header summary card with Blueprint gradient
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                            Column {
                                Text(
                                    text = currentProjectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Periode: ${selectedPeriod.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                            Surface(
                                color = Color(0x33F59E0B),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${String.format("%.1f", kpi.avgProgressPercent)}% FISIK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SafetyAmber,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Material (${materials.size})", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(formatRupiah(kpi.totalMaterialCost), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                            }
                            Column {
                                Text("Upah (${laborWages.size})", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(formatRupiah(kpi.totalLaborCost), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SafetyAmber)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Periode", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                Text(formatRupiah(kpi.totalOverallCost), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Search Bar & Filter
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari material, toko, tukang, atau tugas...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Tab Row
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Material (${filteredMaterialsList.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Upah (${filteredLaborList.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("RAB & Biaya", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Category filter chips
                item {
                    val categories = listOf("Semua", "Semen & Pasir", "Besi & Baja", "Bata & Dinding", "Finishing & Cat")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            SuggestionChip(
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 10.sp, fontWeight = if (selectedCategoryFilter == cat) FontWeight.Bold else FontWeight.Normal) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (selectedCategoryFilter == cat) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (filteredMaterialsList.isEmpty()) {
                    item {
                        EmptyStateCard("Tidak ada pemakaian material yang sesuai kriteria pencarian.")
                    }
                } else {
                    items(filteredMaterialsList) { mat ->
                        val catColor = getCategoryColor(mat.category)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(catColor.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
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
                                    }

                                    Text(
                                        text = formatRupiah(mat.totalCost),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Volume: ${mat.quantity} ${mat.unit} @ ${formatRupiah(mat.unitPrice)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = Color(0x1A0F172A),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = mat.source,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { viewModel.deleteMaterial(mat) },
                                                modifier = Modifier.size(24.dp)
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
                                }

                                if (mat.supplier.isNotBlank() || mat.invoiceOrNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (mat.supplier.isNotBlank()) {
                                            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(mat.supplier, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        if (mat.invoiceOrNote.isNotBlank()) {
                                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(mat.invoiceOrNote, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Labor Wages list
                if (filteredLaborList.isEmpty()) {
                    item {
                        EmptyStateCard("Tidak ada catatan upah tenaga kerja yang sesuai pencarian.")
                    }
                } else {
                    items(filteredLaborList) { wage ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(SecondaryContainerLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Engineering,
                                                contentDescription = null,
                                                tint = OnSecondaryContainerLight,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
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
                                    }

                                    Text(
                                        text = formatRupiah(wage.totalWage),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryLight
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${wage.durationHOK} HOK @ ${formatRupiah(wage.wagePerUnit)}/org",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = Color(0x1A0F172A),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = wage.source,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { viewModel.deleteLaborWage(wage) },
                                                modifier = Modifier.size(24.dp)
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
                                }

                                if (wage.taskDescription.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = wage.taskDescription,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("Komparasi RAB vs Realisasi Anggaran", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                SummaryRow("Total RAB Proyek Disetujui:", formatRupiah(kpi.totalRAB))
                                SummaryRow("Realisasi Pengeluaran Material:", formatRupiah(kpi.totalMaterialCost))
                                SummaryRow("Realisasi Pengeluaran Upah:", formatRupiah(kpi.totalLaborCost))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                                SummaryRow("Total Pengeluaran Terpakai:", formatRupiah(kpi.totalOverallCost), isBold = true)
                                val sisa = kpi.totalRAB - kpi.totalOverallCost
                                SummaryRow(
                                    "Sisa Anggaran Tersedia:",
                                    formatRupiah(sisa),
                                    isBold = true,
                                    valueColor = if (sisa >= 0) SuccessGreen else AlertRed
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text("Indikator Efisiensi Fisik & Finansial", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                val total = kpi.totalOverallCost.coerceAtLeast(1.0)
                                val matPercent = (kpi.totalMaterialCost / total) * 100
                                val wagePercent = (kpi.totalLaborCost / total) * 100
                                SummaryRow("Porsi Biaya Material:", "${String.format("%.1f", matPercent)}%")
                                SummaryRow("Porsi Biaya Upah Tukang:", "${String.format("%.1f", wagePercent)}%")
                                SummaryRow("Progress Fisik Lapangan:", "${String.format("%.1f", kpi.avgProgressPercent)}%", isBold = true, valueColor = MaterialTheme.colorScheme.primary)
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
            append("==========================================\n\n")

            append("🧱 1. REKAP PEMAKAIAN MATERIAL (${materials.size} item):\n")
            if (materials.isEmpty()) append("- Tidak ada pemakaian material pada periode ini.\n")
            else {
                materials.forEachIndexed { i, m ->
                    append("${i + 1}. ${m.materialName} (${m.quantity} ${m.unit} @ ${formatRupiah(m.unitPrice)}) = ${formatRupiah(m.totalCost)}\n")
                    if (m.supplier.isNotBlank()) append("   Supplier: ${m.supplier}\n")
                    if (m.invoiceOrNote.isNotBlank()) append("   Ket: ${m.invoiceOrNote}\n")
                }
            }
            append(">> Subtotal Material: ${formatRupiah(kpi.totalMaterialCost)}\n\n")

            append("👷 2. REKAP UPAH TENAGA KERJA (${laborWages.size} kelompok):\n")
            if (laborWages.isEmpty()) append("- Tidak ada catatan upah pada periode ini.\n")
            else {
                laborWages.forEachIndexed { i, w ->
                    append("${i + 1}. ${w.workerRole} (${w.workerCount} org, ${w.durationHOK} HOK @ ${formatRupiah(w.wagePerUnit)}) = ${formatRupiah(w.totalWage)}\n")
                    if (w.taskDescription.isNotBlank()) append("   Pekerjaan: ${w.taskDescription}\n")
                }
            }
            append(">> Subtotal Upah: ${formatRupiah(kpi.totalLaborCost)}\n\n")

            append("==========================================\n")
            append("💰 TOTAL PENGELUARAN AKTUAL: ${formatRupiah(kpi.totalOverallCost)}\n")
            append("📊 TOTAL RAB DISETUJUI: ${formatRupiah(kpi.totalRAB)}\n")
            append("🏗️ PROGRESS FISIK: ${String.format("%.1f", kpi.avgProgressPercent)}%\n")
            append("==========================================\n")
            append("Dibuat otomatis via Aplikasi Rekap Proyek AI (Bot Telegram & Armbian Server)")
        }

        ExportReportDialog(
            reportText = reportContent,
            onDismiss = { showExportDialog = false }
        )
    }
}
