package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AIAnalysisEntity
import com.example.data.local.entity.LaborWageEntity
import com.example.data.local.entity.MaterialUsageEntity
import com.example.data.local.entity.ProgressMilestoneEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TelegramMessageEntity
import com.example.data.remote.ArmbianServerConfig
import com.example.data.remote.ParsedChatResult
import com.example.data.remote.SyncResult
import com.example.data.repository.ConstructionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavScreen {
    DASHBOARD,
    LAPORAN,
    RAB_EXPLORER,
    TELEGRAM_HUB,
    AI_ANALYSIS,
    PROYEK,
    MANAJEMEN_USER,
    ARMBIAN_SYNC
}

enum class DatePeriodFilter(val label: String) {
    TODAY("Hari Ini"),
    LAST_7_DAYS("7 Hari"),
    THIS_MONTH("Bulan Ini"),
    ALL_TIME("Semua")
}

data class DashboardKpi(
    val totalMaterialCost: Double = 0.0,
    val totalLaborCost: Double = 0.0,
    val totalOverallCost: Double = 0.0,
    val totalRAB: Double = 0.0,
    val avgProgressPercent: Float = 0f,
    val totalMaterialItems: Int = 0,
    val totalWorkerCount: Int = 0
)

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ConstructionRepository(application)

    // Current Screen
    private val _currentScreen = MutableStateFlow(AppNavScreen.DASHBOARD)
    val currentScreen: StateFlow<AppNavScreen> = _currentScreen.asStateFlow()

    // Selected Project Filter (null = All Projects)
    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    // Selected Date Filter
    private val _selectedPeriod = MutableStateFlow(DatePeriodFilter.ALL_TIME)
    val selectedPeriod: StateFlow<DatePeriodFilter> = _selectedPeriod.asStateFlow()

    // Database Flows
    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val materials: StateFlow<List<MaterialUsageEntity>> = repository.allMaterials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val laborWages: StateFlow<List<LaborWageEntity>> = repository.allLaborWages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val milestones: StateFlow<List<ProgressMilestoneEntity>> = repository.allMilestones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val telegramMessages: StateFlow<List<TelegramMessageEntity>> = repository.allTelegramMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiAnalyses: StateFlow<List<AIAnalysisEntity>> = repository.allAiAnalyses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<com.example.data.local.entity.AppUserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rabItems: StateFlow<List<com.example.data.local.entity.RabItemEntity>> = repository.allRabItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Materials Flow
    val filteredMaterials: StateFlow<List<MaterialUsageEntity>> = combine(
        materials,
        _selectedProjectId,
        _selectedPeriod
    ) { allMats, projId, period ->
        val timeBound = getTimeBoundMillis(period)
        allMats.filter { m ->
            (projId == null || m.projectId == projId) && (m.date >= timeBound)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Labor Wages Flow
    val filteredLaborWages: StateFlow<List<LaborWageEntity>> = combine(
        laborWages,
        _selectedProjectId,
        _selectedPeriod
    ) { allLabors, projId, period ->
        val timeBound = getTimeBoundMillis(period)
        allLabors.filter { l ->
            (projId == null || l.projectId == projId) && (l.date >= timeBound)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed Dashboard KPIs
    val dashboardKpi: StateFlow<DashboardKpi> = combine(
        projects,
        filteredMaterials,
        filteredLaborWages,
        _selectedProjectId
    ) { projs, mats, labors, selectedId ->
        val totalMat = mats.sumOf { it.totalCost }
        val totalWage = labors.sumOf { it.totalWage }
        val overall = totalMat + totalWage

        val relevantProjects = if (selectedId != null) projs.filter { it.id == selectedId } else projs
        val rab = relevantProjects.sumOf { it.budgetRAB }
        val avgProg = if (relevantProjects.isNotEmpty()) {
            relevantProjects.map { it.progressPercent }.average().toFloat()
        } else 0f
        val workerCount = labors.sumOf { it.workerCount }

        DashboardKpi(
            totalMaterialCost = totalMat,
            totalLaborCost = totalWage,
            totalOverallCost = overall,
            totalRAB = rab,
            avgProgressPercent = avgProg,
            totalMaterialItems = mats.size,
            totalWorkerCount = workerCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardKpi())

    // AI & Telegram States
    private val _isAiParsing = MutableStateFlow(false)
    val isAiParsing: StateFlow<Boolean> = _isAiParsing.asStateFlow()

    private val _isAiAnalyzing = MutableStateFlow(false)
    val isAiAnalyzing: StateFlow<Boolean> = _isAiAnalyzing.asStateFlow()

    private val _isFetchingTelegram = MutableStateFlow(false)
    val isFetchingTelegram: StateFlow<Boolean> = _isFetchingTelegram.asStateFlow()

    private val _isSyncingArmbian = MutableStateFlow(false)
    val isSyncingArmbian: StateFlow<Boolean> = _isSyncingArmbian.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _parsedReviewResult = MutableStateFlow<ParsedChatResult?>(null)
    val parsedReviewResult: StateFlow<ParsedChatResult?> = _parsedReviewResult.asStateFlow()

    private val _currentParsedRawText = MutableStateFlow("")
    val currentParsedRawText: StateFlow<String> = _currentParsedRawText.asStateFlow()

    // Configs
    val armbianConfig = MutableStateFlow(ArmbianServerConfig())
    val telegramBotToken = MutableStateFlow("")
    val telegramChatId = MutableStateFlow("")

    fun setNavScreen(screen: AppNavScreen) {
        _currentScreen.value = screen
    }

    fun selectProject(projectId: Long?) {
        _selectedProjectId.value = projectId
    }

    fun setPeriodFilter(period: DatePeriodFilter) {
        _selectedPeriod.value = period
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun parseChatWithAi(rawText: String) {
        if (rawText.isBlank()) {
            _statusMessage.value = "Teks pesan masih kosong"
            return
        }

        viewModelScope.launch {
            _isAiParsing.value = true
            _currentParsedRawText.value = rawText
            try {
                val result = repository.parseIncomingChat(rawText)
                _parsedReviewResult.value = result
                _statusMessage.value = if (result.isParsedViaAi) {
                    "✨ Berhasil dianalisa oleh Gemini AI!"
                } else {
                    "✅ Berhasil diekstrak (Mesin NLP Cepat Lapangan)"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Gagal memproses chat: ${e.localizedMessage}"
            } finally {
                _isAiParsing.value = false
            }
        }
    }

    fun confirmAndSaveParsedResult(targetProjectId: Long) {
        val parsed = _parsedReviewResult.value ?: return
        viewModelScope.launch {
            try {
                repository.commitParsedResult(
                    result = parsed,
                    targetProjectId = targetProjectId,
                    rawText = _currentParsedRawText.value,
                    senderName = "Mandor Lapangan"
                )
                _parsedReviewResult.value = null
                _statusMessage.value = "Data material & upah berhasil disimpan ke database!"
                _currentScreen.value = AppNavScreen.LAPORAN
            } catch (e: Exception) {
                _statusMessage.value = "Gagal menyimpan: ${e.localizedMessage}"
            }
        }
    }

    fun cancelParsedReview() {
        _parsedReviewResult.value = null
    }

    fun fetchTelegramUpdates() {
        val token = telegramBotToken.value
        if (token.isBlank()) {
            _statusMessage.value = "Masukkan Token Bot Telegram terlebih dahulu di Pengaturan"
            return
        }

        viewModelScope.launch {
            _isFetchingTelegram.value = true
            try {
                val res = repository.fetchAndProcessTelegramUpdates(token)
                res.fold(
                    onSuccess = { count ->
                        _statusMessage.value = if (count > 0) {
                            "Berhasil mengambil dan memproses $count pesan baru dari Bot Telegram!"
                        } else {
                            "Tidak ada pesan baru di Telegram bot."
                        }
                    },
                    onFailure = { err ->
                        _statusMessage.value = "Telegram: ${err.localizedMessage}"
                    }
                )
            } finally {
                _isFetchingTelegram.value = false
            }
        }
    }

    fun runAiAnalysis(projectId: Long, mode: String) {
        viewModelScope.launch {
            _isAiAnalyzing.value = true
            try {
                val analysis = repository.runAiProjectAnalysis(projectId, mode)
                _statusMessage.value = "Analisis AI Gemini selesai dibuat!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal membuat analisis AI: ${e.localizedMessage}"
            } finally {
                _isAiAnalyzing.value = false
            }
        }
    }

    fun testArmbianConnection(url: String, key: String) {
        viewModelScope.launch {
            _isSyncingArmbian.value = true
            try {
                val res = repository.armbianService.testConnection(url, key)
                res.fold(
                    onSuccess = { msg -> _statusMessage.value = msg },
                    onFailure = { err -> _statusMessage.value = err.message ?: "Koneksi gagal" }
                )
            } finally {
                _isSyncingArmbian.value = false
            }
        }
    }

    fun syncDataToArmbian() {
        viewModelScope.launch {
            _isSyncingArmbian.value = true
            try {
                val res = repository.syncToArmbian(armbianConfig.value)
                _statusMessage.value = res.message
            } finally {
                _isSyncingArmbian.value = false
            }
        }
    }

    fun createProject(
        name: String,
        codeSpk: String,
        location: String,
        client: String,
        budget: Double,
        foreman: String
    ) {
        viewModelScope.launch {
            try {
                repository.insertProject(
                    ProjectEntity(
                        name = name,
                        codeSpk = codeSpk,
                        location = location,
                        clientName = client,
                        budgetRAB = budget,
                        foremanInCharge = foreman
                    )
                )
                _statusMessage.value = "Proyek '$name' berhasil ditambahkan!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal menambah proyek: ${e.localizedMessage}"
            }
        }
    }

    fun updateProject(project: ProjectEntity) {
        viewModelScope.launch {
            try {
                repository.updateProject(project)
                _statusMessage.value = "Proyek '${project.name}' berhasil diperbarui!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal mengubah proyek: ${e.localizedMessage}"
            }
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            try {
                repository.deleteProject(project)
                if (_selectedProjectId.value == project.id) {
                    _selectedProjectId.value = null
                }
                _statusMessage.value = "Proyek '${project.name}' berhasil dihapus!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal menghapus proyek: ${e.localizedMessage}"
            }
        }
    }

    // --- USER CRUD ---
    fun createUser(
        username: String,
        fullName: String,
        role: String,
        email: String,
        phone: String,
        assignedProject: String
    ) {
        viewModelScope.launch {
            try {
                repository.insertUser(
                    com.example.data.local.entity.AppUserEntity(
                        username = username,
                        fullName = fullName,
                        role = role,
                        email = email,
                        phone = phone,
                        assignedProject = assignedProject
                    )
                )
                _statusMessage.value = "User '$fullName' ($role) berhasil didaftarkan!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal menambah user: ${e.localizedMessage}"
            }
        }
    }

    fun updateUser(user: com.example.data.local.entity.AppUserEntity) {
        viewModelScope.launch {
            try {
                repository.updateUser(user)
                _statusMessage.value = "Data user '${user.fullName}' diperbarui!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal update user: ${e.localizedMessage}"
            }
        }
    }

    fun deleteUser(user: com.example.data.local.entity.AppUserEntity) {
        viewModelScope.launch {
            try {
                repository.deleteUser(user)
                _statusMessage.value = "User '${user.fullName}' berhasil dihapus!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal hapus user: ${e.localizedMessage}"
            }
        }
    }

    // --- RAB ITEM CRUD ---
    fun addRabItem(
        projectId: Long,
        floor: String,
        catCode: String,
        catName: String,
        desc: String,
        volume: Double,
        unit: String,
        price: Double
    ) {
        viewModelScope.launch {
            try {
                val total = volume * price
                repository.insertRabItem(
                    com.example.data.local.entity.RabItemEntity(
                        projectId = projectId,
                        floorLevel = floor,
                        categoryCode = catCode,
                        categoryName = catName,
                        workDescription = desc,
                        volume = volume,
                        unit = unit,
                        unitPrice = price,
                        totalPrice = total
                    )
                )
                _statusMessage.value = "Item RAB '$desc' berhasil ditambahkan!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal menambah RAB: ${e.localizedMessage}"
            }
        }
    }

    fun updateRabItem(item: com.example.data.local.entity.RabItemEntity) {
        viewModelScope.launch {
            try {
                repository.updateRabItem(item)
                _statusMessage.value = "Item RAB diperbarui!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal update RAB: ${e.localizedMessage}"
            }
        }
    }

    fun deleteRabItem(item: com.example.data.local.entity.RabItemEntity) {
        viewModelScope.launch {
            try {
                repository.deleteRabItem(item)
                _statusMessage.value = "Item RAB dihapus!"
            } catch (e: Exception) {
                _statusMessage.value = "Gagal hapus RAB: ${e.localizedMessage}"
            }
        }
    }

    fun updateProgress(projectId: Long, newProgress: Float) {
        viewModelScope.launch {
            repository.updateProjectProgress(projectId, newProgress)
            _statusMessage.value = "Progress proyek diperbarui menjadi ${String.format("%.1f", newProgress)}%"
        }
    }

    fun addManualMaterial(
        projectId: Long,
        name: String,
        category: String,
        qty: Double,
        unit: String,
        unitPrice: Double,
        supplier: String,
        note: String
    ) {
        viewModelScope.launch {
            repository.insertMaterial(
                MaterialUsageEntity(
                    projectId = projectId,
                    materialName = name,
                    category = category,
                    quantity = qty,
                    unit = unit,
                    unitPrice = unitPrice,
                    supplier = supplier,
                    invoiceOrNote = note,
                    source = "Manual Mandor"
                )
            )
            _statusMessage.value = "Material '$name' berhasil dicatat!"
        }
    }

    fun addManualLabor(
        projectId: Long,
        role: String,
        count: Int,
        duration: Double,
        rate: Double,
        desc: String,
        foreman: String
    ) {
        viewModelScope.launch {
            repository.insertLaborWage(
                LaborWageEntity(
                    projectId = projectId,
                    workerRole = role,
                    workerCount = count,
                    durationHOK = duration,
                    wagePerUnit = rate,
                    taskDescription = desc,
                    foremanName = foreman,
                    source = "Manual Mandor"
                )
            )
            _statusMessage.value = "Catatan upah $role ($count orang) berhasil disimpan!"
        }
    }

    fun deleteMaterial(item: MaterialUsageEntity) {
        viewModelScope.launch {
            repository.deleteMaterial(item)
            _statusMessage.value = "Material '${item.materialName}' dihapus"
        }
    }

    fun deleteLaborWage(item: LaborWageEntity) {
        viewModelScope.launch {
            repository.deleteLaborWage(item)
            _statusMessage.value = "Catatan upah '${item.workerRole}' dihapus"
        }
    }

    private fun getTimeBoundMillis(period: DatePeriodFilter): Long {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 3600 * 1000L
        return when (period) {
            DatePeriodFilter.TODAY -> now - oneDay
            DatePeriodFilter.LAST_7_DAYS -> now - (7 * oneDay)
            DatePeriodFilter.THIS_MONTH -> now - (30 * oneDay)
            DatePeriodFilter.ALL_TIME -> 0L
        }
    }
}
