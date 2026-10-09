package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.remote.ArmbianServerConfig
import com.example.ui.components.PythonCodeDialog
import com.example.ui.components.SqlSchemaDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.ProjectViewModel

@Composable
fun ArmbianSyncScreen(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val isSyncing by viewModel.isSyncingArmbian.collectAsState()
    val botToken by viewModel.telegramBotToken.collectAsState()
    val chatId by viewModel.telegramChatId.collectAsState()

    var serverUrl by remember { mutableStateOf("http://192.168.1.100:8000") }
    var apiKey by remember { mutableStateOf("armbian_secret_token_2026") }
    var dbName by remember { mutableStateOf("db_rekap_konstruksi") }
    var tempBotToken by remember { mutableStateOf(botToken) }
    var tempChatId by remember { mutableStateOf(chatId) }

    var showSqlDialog by remember { mutableStateOf(false) }
    var showPythonDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("armbian_sync_screen"),
        contentPadding = PaddingValues(16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Armbian Banner
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
                                .background(SuccessGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Server Armbian & MySQL",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Database sentral MySQL pada mini PC / SBC Armbian",
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
                        text = "Pengaturan Server MySQL Armbian",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("URL / IP Server Armbian (misal: http://192.168.1.100:8000)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dbName,
                        onValueChange = { dbName = it },
                        label = { Text("Nama Database MySQL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Secret Key / Token Auth") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.testArmbianConnection(serverUrl, apiKey) },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tes Koneksi")
                        }

                        Button(
                            onClick = {
                                viewModel.armbianConfig.value = ArmbianServerConfig(
                                    serverUrl = serverUrl,
                                    apiKey = apiKey,
                                    databaseName = dbName
                                )
                                viewModel.syncDataToArmbian()
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1.3f)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync...")
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sinkronisasi Data")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = { showSqlDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lihat Skrip DDL MySQL Armbian (.sql)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { showPythonDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lihat Kode Server Python Armbian (app.py)")
                    }
                }
            }
        }

        // Telegram Bot Config Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Konfigurasi Bot Telegram",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Diperoleh dari @BotFather di Telegram",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = tempBotToken,
                        onValueChange = {
                            tempBotToken = it
                            viewModel.telegramBotToken.value = it
                        },
                        label = { Text("Token Bot Telegram (contoh: 123456:ABC-DEF...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = tempChatId,
                        onValueChange = {
                            tempChatId = it
                            viewModel.telegramChatId.value = it
                        },
                        label = { Text("Chat ID / ID Grup Proyek (contoh: -1001234567)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                viewModel.telegramBotToken.value = tempBotToken
                                viewModel.telegramChatId.value = tempChatId
                                viewModel.fetchTelegramUpdates()
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan & Tarik Chat")
                        }
                    }
                }
            }
        }

        // Architecture Workflow Guide Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Arsitektur Sistem Terpadu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 📱 Mandor/Pengawas mengirim chat laporan di Telegram.\n" +
                                "2. 🤖 Bot Telegram menerima pesan lapangan.\n" +
                                "3. 🧠 Otak AI (Gemini 3.5 Flash) memilah nama material, harga satuan, jumlah tukang, dan progress.\n" +
                                "4. 📲 Aplikasi Android menyajikan dashboard rekap, laporan periode, & progress fisik.\n" +
                                "5. 🗄️ Server Armbian menyimpan database MySQL secara tersentralisasi.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }

    if (showSqlDialog) {
        val schema = viewModel.repository.armbianService.generateMySqlSchema(dbName)
        SqlSchemaDialog(
            sqlContent = schema,
            onDismiss = { showSqlDialog = false }
        )
    }

    if (showPythonDialog) {
        val pyCode = viewModel.repository.armbianService.generatePythonServerCode()
        PythonCodeDialog(
            pythonCode = pyCode,
            onDismiss = { showPythonDialog = false }
        )
    }
}
