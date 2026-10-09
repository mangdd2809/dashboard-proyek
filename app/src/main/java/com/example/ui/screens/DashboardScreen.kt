package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.example.ui.viewmodel.AppNavScreen
import com.example.ui.viewmodel.DatePeriodFilter
import com.example.ui.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ProjectViewModel,
    onOpenAddMaterial: () -> Unit,
    onOpenAddLabor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val selectedProjId by viewModel.selectedProjectId.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val kpi by viewModel.dashboardKpi.collectAsState()
    val materials by viewModel.filteredMaterials.collectAsState()
    val laborWages by viewModel.filteredLaborWages.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Project Filter Chips
        item {
            ProjectSelectorChipRow(
                projects = projects,
                selectedProjectId = selectedProjId,
                onSelectProject = { viewModel.selectProject(it) }
            )
        }

        // Period Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DatePeriodFilter.values().forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.setPeriodFilter(period) },
                        label = { Text(period.label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("period_chip_${period.name.lowercase()}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // KPI Stat Cards Grid (2x2)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Biaya Material",
                        value = formatRupiah(kpi.totalMaterialCost),
                        subtitle = "${kpi.totalMaterialItems} transaksi material",
                        icon = Icons.Default.Category,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Upah Tenaga Kerja",
                        value = formatRupiah(kpi.totalLaborCost),
                        subtitle = "${kpi.totalWorkerCount} total pekerja/HOK",
                        icon = Icons.Default.Engineering,
                        containerColor = SecondaryContainerLight,
                        contentColor = OnSecondaryContainerLight,
                        iconBgColor = SecondaryLight.copy(alpha = 0.2f),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Total Pengeluaran",
                        value = formatRupiah(kpi.totalOverallCost),
                        subtitle = "Akumulasi material & upah",
                        icon = Icons.Default.AccountBalanceWallet,
                        containerColor = BlueprintNavy,
                        contentColor = Color.White,
                        iconBgColor = SafetyAmberDark,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Progress Fisik",
                        value = "${String.format("%.1f", kpi.avgProgressPercent)}%",
                        subtitle = "Rata-rata progres proyek",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        containerColor = TertiaryContainerLight,
                        contentColor = OnTertiaryContainerLight,
                        iconBgColor = SuccessGreen.copy(alpha = 0.3f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Budget Realization vs RAB
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                BudgetProgressBar(
                    rab = kpi.totalRAB,
                    actualCost = kpi.totalOverallCost,
                    progressPercent = kpi.avgProgressPercent
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Quick Actions Row
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Aksi Cepat Mandor & AI",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedButton(
                        onClick = { viewModel.setNavScreen(AppNavScreen.TELEGRAM_HUB) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_telegram_button"),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("AI Telegram", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    ElevatedButton(
                        onClick = onOpenAddMaterial,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_material_button"),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("+ Material", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    ElevatedButton(
                        onClick = onOpenAddLabor,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_labor_button"),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("+ Upah", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    ElevatedButton(
                        onClick = { viewModel.setNavScreen(AppNavScreen.AI_ANALYSIS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_ai_analysis_button"),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SafetyAmberDark,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Analisa AI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Recent Activity Feed Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aktivitas Rekap Terbaru",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.setNavScreen(AppNavScreen.LAPORAN) }) {
                    Text("Lihat Semua Rekap", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Combined Recent items (Latest 6 items)
        val recentMaterials = materials.take(4)
        if (recentMaterials.isEmpty() && laborWages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada rekap pengeluaran.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(recentMaterials) { mat ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mat.materialName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${mat.quantity} ${mat.unit} • ${mat.category} • ${mat.source}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatRupiah(mat.totalCost),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatDateShort(mat.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        val recentWages = laborWages.take(3)
        items(recentWages) { wage ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SecondaryContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = OnSecondaryContainerLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${wage.workerRole} (${wage.workerCount} org)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = wage.taskDescription.take(28) + if (wage.taskDescription.length > 28) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatRupiah(wage.totalWage),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryLight
                        )
                        Text(
                            text = formatDateShort(wage.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
