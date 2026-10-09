package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.formatDate
import com.example.ui.theme.*
import com.example.ui.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAnalysisScreen(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val isAnalyzing by viewModel.isAiAnalyzing.collectAsState()
    val aiAnalyses by viewModel.aiAnalyses.collectAsState()

    var selectedProjectId by remember { mutableStateOf(projects.firstOrNull()?.id ?: 1L) }
    val modes = listOf(
        "Evaluasi Efisiensi & RAB" to "Memeriksa keselarasan penyerapan dana vs progress fisik bangunan.",
        "Audit Pemborosan Material" to "Menganalisis pengeluaran semen, besi, pasir dan sisa stok.",
        "Produktivitas Upah & HOK" to "Mengevaluasi rasio tukang vs kenek dan efisiensi lembur.",
        "Prediksi Sisa Anggaran" to "Estimasi biaya penyelesaian hingga target serah terima."
    )
    var selectedModeIndex by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copiedMsg by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_analysis_screen"),
        contentPadding = PaddingValues(16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // AI Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BlueprintNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SafetyAmber),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = BlueprintNavy, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Otak AI Analisis Proyek (Gemini)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Estimator finansial & Quantity Surveyor otomatis",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pilih Proyek & Modus Analisa:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Project selector
                    Text("Proyek Target:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(6.dp)
                    ) {
                        projects.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedProjectId == p.id,
                                    onClick = { selectedProjectId = p.id }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Progress: ${p.progressPercent}% • Mandor: ${p.foremanInCharge}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Selection
                    Text("Fokus Analisa AI:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    modes.forEachIndexed { idx, (title, desc) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedModeIndex == idx) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            onClick = { selectedModeIndex = idx }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedModeIndex == idx, onClick = { selectedModeIndex = idx })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.runAiAnalysis(selectedProjectId, modes[selectedModeIndex].first) },
                        enabled = !isAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("run_ai_analysis_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sedang Menganalisis dengan Gemini...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SafetyAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Jalankan Analisa AI Sekarang")
                        }
                    }
                }
            }
        }

        // Section Title: Hasil Analisis AI
        item {
            Text(
                text = "Laporan Analisis Tersimpan (${aiAnalyses.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (aiAnalyses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Belum ada laporan analisis AI. Pilih proyek di atas dan klik 'Jalankan Analisa AI Sekarang'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(aiAnalyses) { analysis ->
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
                                    text = analysis.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${analysis.projectName} • ${formatDate(analysis.generatedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            AssistChip(
                                onClick = { },
                                label = { Text(analysis.riskLevel, fontSize = 10.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (analysis.riskLevel == "Tinggi") AlertRed else SuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = analysis.contentMarkdown,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(analysis.contentMarkdown))
                                    copiedMsg = "Tersalin ke Clipboard"
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, analysis.contentMarkdown)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Bagikan Analisis AI")
                                    context.startActivity(shareIntent)
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kirim / Share", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
