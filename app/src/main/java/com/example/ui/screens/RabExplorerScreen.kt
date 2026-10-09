package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import com.example.data.local.entity.RabItemEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.ProjectViewModel

@Composable
fun RabExplorerScreen(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val rabItems by viewModel.rabItems.collectAsState()
    val selectedProjId by viewModel.selectedProjectId.collectAsState()

    var activeFloor by remember { mutableStateOf("SEMUA") }
    var searchQuery by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<RabItemEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<RabItemEntity?>(null) }

    val activeProject = remember(projects, selectedProjId) {
        if (selectedProjId != null) projects.firstOrNull { it.id == selectedProjId }
        else projects.firstOrNull { it.id == 1L } ?: projects.firstOrNull()
    }

    val targetProjectId = activeProject?.id ?: 1L

    val filteredItems = remember(rabItems, targetProjectId, activeFloor, searchQuery) {
        rabItems.filter { item ->
            item.projectId == targetProjectId &&
                    (activeFloor == "SEMUA" || item.floorLevel.equals(activeFloor, ignoreCase = true)) &&
                    (searchQuery.isBlank() || item.workDescription.contains(searchQuery, ignoreCase = true) || item.categoryName.contains(searchQuery, ignoreCase = true))
        }
    }

    val totalCost = remember(filteredItems) { filteredItems.sumOf { it.totalPrice } }
    val totalProjectRAB = activeProject?.budgetRAB ?: 14_930_200_000.0

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SafetyAmberDark,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tambah Item RAB", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_rab_item_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("rab_explorer_screen"),
            contentPadding = PaddingValues(16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Card: Dokumen EE As-Syifa Banner
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
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = BlueprintNavy, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Rencana Anggaran Biaya (RAB)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Estimate Engineer (EE) • Fardan Architecture",
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
                                        text = activeProject?.codeSpk ?: "SPK-ASSYIFA-01",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SafetyAmber,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = activeProject?.name ?: "Pembangunan Universitas As-Syifa Subang",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Pemilik: ${activeProject?.clientName?.ifBlank { "Yayasan As-Syifa Al Qoeriyyah" } ?: "Yayasan As-Syifa Al Qoeriyyah"} • ${activeProject?.location ?: "Subang, Jawa Barat"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0x22FFFFFF))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("TOTAL ESTIMATE ENGINEER (EE)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                    Text(formatRupiah(totalProjectRAB), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SafetyAmber)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("TERFILTER (${filteredItems.size} ITEM)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                    Text(formatRupiah(totalCost), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Project Selector Spinner & Floor Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_rab_input"),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Cari item pekerjaan (beton, tangga, pintu, AC, dll)...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus")
                                }
                            }
                        } else null,
                        singleLine = true
                    )

                    // Floor Tab Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SEMUA", "LANTAI 1", "LANTAI 2", "LANTAI 3").forEach { floor ->
                            FilterChip(
                                selected = activeFloor == floor,
                                onClick = { activeFloor = floor },
                                label = {
                                    Text(
                                        text = floor,
                                        fontSize = 11.sp,
                                        fontWeight = if (activeFloor == floor) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (activeFloor == floor) {
                                    { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // Items List
            if (filteredItems.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        message = "Belum ada item RAB untuk kategori atau pencarian ini."
                    )
                }
            } else {
                items(filteredItems) { item ->
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
                                Surface(
                                    color = when (item.floorLevel) {
                                        "LANTAI 1" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                        "LANTAI 2" -> SafetyAmber.copy(alpha = 0.18f)
                                        else -> SuccessGreen.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${item.floorLevel} • KODE ${item.categoryCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (item.floorLevel) {
                                            "LANTAI 1" -> Color(0xFF1D4ED8)
                                            "LANTAI 2" -> SafetyAmberDark
                                            else -> Color(0xFF047857)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { itemToEdit = item },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { itemToDelete = item },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = AlertRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.categoryName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = item.workDescription,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.volume > 0.0) {
                                    Text(
                                        text = "Volume: ${item.volume} ${item.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "Satuan: ${item.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatRupiah(item.totalPrice),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueprintNavy
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add RAB Item Dialog
    if (showAddDialog) {
        RabItemFormDialog(
            title = "Tambah Item RAB Baru",
            initialProject = activeProject?.id ?: 1L,
            initialFloor = if (activeFloor == "SEMUA") "LANTAI 1" else activeFloor,
            initialCode = "A",
            initialCategory = "",
            initialDescription = "",
            initialVolume = 1.0,
            initialUnit = "ls",
            initialPrice = 0.0,
            onDismiss = { showAddDialog = false },
            onConfirm = { floor, code, cat, desc, vol, unit, price ->
                viewModel.addRabItem(targetProjectId, floor, code, cat, desc, vol, unit, price)
                showAddDialog = false
            }
        )
    }

    // Edit RAB Item Dialog
    itemToEdit?.let { item ->
        RabItemFormDialog(
            title = "Edit Item RAB",
            initialProject = item.projectId,
            initialFloor = item.floorLevel,
            initialCode = item.categoryCode,
            initialCategory = item.categoryName,
            initialDescription = item.workDescription,
            initialVolume = item.volume,
            initialUnit = item.unit,
            initialPrice = item.unitPrice,
            onDismiss = { itemToEdit = null },
            onConfirm = { floor, code, cat, desc, vol, unit, price ->
                val updated = item.copy(
                    floorLevel = floor,
                    categoryCode = code,
                    categoryName = cat,
                    workDescription = desc,
                    volume = vol,
                    unit = unit,
                    unitPrice = price,
                    totalPrice = vol * price
                )
                viewModel.updateRabItem(updated)
                itemToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Hapus Item RAB?", fontWeight = FontWeight.Bold) },
            text = { Text("Anda yakin ingin menghapus '${item.categoryName} - ${item.workDescription}' dari daftar anggaran?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRabItem(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun RabItemFormDialog(
    title: String,
    initialProject: Long,
    initialFloor: String,
    initialCode: String,
    initialCategory: String,
    initialDescription: String,
    initialVolume: Double,
    initialUnit: String,
    initialPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (floor: String, code: String, cat: String, desc: String, vol: Double, unit: String, price: Double) -> Unit
) {
    var floor by remember { mutableStateOf(initialFloor) }
    var code by remember { mutableStateOf(initialCode) }
    var category by remember { mutableStateOf(initialCategory) }
    var description by remember { mutableStateOf(initialDescription) }
    var volumeStr by remember { mutableStateOf(if (initialVolume > 0) initialVolume.toString() else "1") }
    var unit by remember { mutableStateOf(initialUnit.ifBlank { "ls" }) }
    var priceStr by remember { mutableStateOf(if (initialPrice > 0) initialPrice.toLong().toString() else "") }

    val floors = listOf("LANTAI 1", "LANTAI 2", "LANTAI 3")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("rab_item_form_dialog"),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                // Floor selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    floors.forEach { f ->
                        FilterChip(
                            selected = floor == f,
                            onClick = { floor = f },
                            label = { Text(f, fontSize = 10.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Kode (A-M)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori (e.g. BETON)") },
                        modifier = Modifier.weight(2.5f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Uraian Pekerjaan / Material") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = volumeStr,
                        onValueChange = { volumeStr = it },
                        label = { Text("Volume") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan (m3, kg, ls)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Harga Satuan (Rp)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val vol = volumeStr.toDoubleOrNull() ?: 1.0
                            val prc = priceStr.replace(".", "").toDoubleOrNull() ?: 0.0
                            onConfirm(floor, code, category, description, vol, unit, prc)
                        },
                        enabled = description.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
