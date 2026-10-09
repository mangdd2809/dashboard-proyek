package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.ui.components.ExtractedItemReviewDialog
import com.example.ui.components.formatDate
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppNavScreen
import com.example.ui.viewmodel.ProjectViewModel

@Composable
fun TelegramHubScreen(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val telegramMessages by viewModel.telegramMessages.collectAsState()
    val isFetching by viewModel.isFetchingTelegram.collectAsState()
    val isAiParsing by viewModel.isAiParsing.collectAsState()
    val botToken by viewModel.telegramBotToken.collectAsState()
    val parsedResult by viewModel.parsedReviewResult.collectAsState()
    val projects by viewModel.projects.collectAsState()

    var inputChatText by remember { mutableStateOf("") }
    val simulationTemplates = remember { viewModel.repository.telegramService.getSimulationTemplates() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("telegram_hub_screen"),
        contentPadding = PaddingValues(16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Telegram Bot Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BlueprintNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(InfoSky),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Integrasi Bot Telegram & AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Otomatisasi analisa laporan mandor lapangan",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.fetchTelegramUpdates() },
                            enabled = !isFetching,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fetch_telegram_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyAmberDark)
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Menarik...")
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tarik Chat Bot")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.setNavScreen(AppNavScreen.ARMBIAN_SYNC) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Setup Token")
                        }
                    }
                }
            }
        }

        // Live Chat Parser Input Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Input & Analisa Chat Mandor (AI)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Paste teks chat Telegram mandor untuk diekstrak otomatis menjadi data material & upah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset template chips
                    Text(
                        text = "Contoh Laporan Mandor Lapangan (1-Klik):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        simulationTemplates.take(2).forEach { (label, text) ->
                            SuggestionChip(
                                onClick = { inputChatText = text },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        simulationTemplates.drop(2).take(2).forEach { (label, text) ->
                            SuggestionChip(
                                onClick = { inputChatText = text },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inputChatText,
                        onValueChange = { inputChatText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 150.dp)
                            .testTag("telegram_chat_input"),
                        placeholder = { Text("Contoh: Hari ini semen masuk 40 sak @ 68rb, tukang 4 org @ 130rb...") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.parseChatWithAi(inputChatText) },
                        enabled = inputChatText.isNotBlank() && !isAiParsing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parse_with_ai_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight)
                    ) {
                        if (isAiParsing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sedang Menganalisis dengan Gemini AI...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SafetyAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analisa dengan AI (Gemini)")
                        }
                    }
                }
            }
        }

        // Section Title: Log Chat Telegram yang Diterima
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Riwayat Chat & Hasil Ekstraksi (${telegramMessages.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (telegramMessages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Belum ada riwayat pesan Telegram yang masuk. Coba ketik teks atau klik contoh simulasi di atas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(telegramMessages) { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = msg.senderName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatDate(msg.receivedAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            AssistChip(
                                onClick = { },
                                label = { Text(msg.detectedProjectName ?: "Proyek", fontSize = 10.sp) },
                                modifier = Modifier.height(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg.rawText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!msg.parsedSummary.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SafetyAmberDark, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = msg.parsedSummary ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Review Dialog
    parsedResult?.let { result ->
        ExtractedItemReviewDialog(
            result = result,
            projects = projects,
            onConfirm = { targetProjId ->
                viewModel.confirmAndSaveParsedResult(targetProjId)
            },
            onDismiss = { viewModel.cancelParsedReview() }
        )
    }
}
