package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AIAnalysisEntity
import com.example.data.local.entity.LaborWageEntity
import com.example.data.local.entity.MaterialUsageEntity
import com.example.data.local.entity.ProgressMilestoneEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TelegramMessageEntity
import com.example.data.remote.ArmbianServerConfig
import com.example.data.remote.ArmbianServerService
import com.example.data.remote.GeminiService
import com.example.data.remote.ParsedChatResult
import com.example.data.remote.SyncResult
import com.example.data.remote.TelegramBotService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ConstructionRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val projectDao = db.projectDao()
    private val materialDao = db.materialDao()
    private val laborDao = db.laborDao()
    private val progressDao = db.progressDao()
    private val telegramDao = db.telegramDao()
    private val aiAnalysisDao = db.aiAnalysisDao()
    private val appUserDao = db.appUserDao()
    private val rabItemDao = db.rabItemDao()

    val geminiService = GeminiService()
    val telegramService = TelegramBotService()
    val armbianService = ArmbianServerService()

    // Observables
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjectsFlow()
    val allMaterials: Flow<List<MaterialUsageEntity>> = materialDao.getAllMaterialsFlow()
    val allLaborWages: Flow<List<LaborWageEntity>> = laborDao.getAllLaborWagesFlow()
    val allMilestones: Flow<List<ProgressMilestoneEntity>> = progressDao.getAllMilestonesFlow()
    val allTelegramMessages: Flow<List<TelegramMessageEntity>> = telegramDao.getAllMessagesFlow()
    val allAiAnalyses: Flow<List<AIAnalysisEntity>> = aiAnalysisDao.getAllAnalysesFlow()
    val allUsers: Flow<List<com.example.data.local.entity.AppUserEntity>> = appUserDao.getAllUsersFlow()
    val allRabItems: Flow<List<com.example.data.local.entity.RabItemEntity>> = rabItemDao.getAllRabItemsFlow()

    fun getRabItemsByProject(projectId: Long): Flow<List<com.example.data.local.entity.RabItemEntity>> =
        rabItemDao.getRabItemsByProjectFlow(projectId)

    fun getRabItemsByProjectAndFloor(projectId: Long, floor: String): Flow<List<com.example.data.local.entity.RabItemEntity>> =
        rabItemDao.getRabItemsByProjectAndFloorFlow(projectId, floor)

    // User CRUD
    suspend fun insertUser(user: com.example.data.local.entity.AppUserEntity): Long = appUserDao.insertUser(user)
    suspend fun updateUser(user: com.example.data.local.entity.AppUserEntity) = appUserDao.updateUser(user)
    suspend fun deleteUser(user: com.example.data.local.entity.AppUserEntity) = appUserDao.deleteUser(user)

    // Project CRUD
    suspend fun deleteProject(project: ProjectEntity) = projectDao.deleteProject(project)

    // RAB CRUD
    suspend fun insertRabItem(item: com.example.data.local.entity.RabItemEntity): Long = rabItemDao.insertRabItem(item)
    suspend fun updateRabItem(item: com.example.data.local.entity.RabItemEntity) = rabItemDao.updateRabItem(item)
    suspend fun deleteRabItem(item: com.example.data.local.entity.RabItemEntity) = rabItemDao.deleteRabItem(item)

    fun getMaterialsByProject(projectId: Long): Flow<List<MaterialUsageEntity>> =
        materialDao.getMaterialsByProjectFlow(projectId)

    fun getLaborWagesByProject(projectId: Long): Flow<List<LaborWageEntity>> =
        laborDao.getLaborWagesByProjectFlow(projectId)

    suspend fun insertProject(project: ProjectEntity): Long = projectDao.insertProject(project)

    suspend fun updateProject(project: ProjectEntity) = projectDao.updateProject(project)

    suspend fun updateProjectProgress(projectId: Long, progress: Float) =
        projectDao.updateProgress(projectId, progress)

    suspend fun insertMaterial(material: MaterialUsageEntity): Long =
        materialDao.insertMaterial(material)

    suspend fun deleteMaterial(material: MaterialUsageEntity) = materialDao.deleteMaterial(material)

    suspend fun insertLaborWage(wage: LaborWageEntity): Long = laborDao.insertLaborWage(wage)

    suspend fun deleteLaborWage(wage: LaborWageEntity) = laborDao.deleteLaborWage(wage)

    suspend fun insertMilestone(milestone: ProgressMilestoneEntity): Long =
        progressDao.insertMilestone(milestone)

    /**
     * Executes AI or local parser on raw text chat
     */
    suspend fun parseIncomingChat(rawText: String): ParsedChatResult {
        val currentProjects = allProjects.firstOrNull() ?: emptyList()
        val projectNames = currentProjects.map { it.name }
        val result = geminiService.parseConstructionMessage(rawText, projectNames)

        // Find match id
        val matched = currentProjects.firstOrNull { p ->
            result.projectName != null && (p.name.contains(result.projectName, ignoreCase = true) || result.projectName.contains(p.name, ignoreCase = true))
        }

        return result.copy(matchedProjectId = matched?.id ?: currentProjects.firstOrNull()?.id)
    }

    /**
     * Commits parsed AI result to Room database
     */
    suspend fun commitParsedResult(
        result: ParsedChatResult,
        targetProjectId: Long,
        rawText: String,
        senderName: String = "Mandor Lapangan"
    ): Long {
        val now = System.currentTimeMillis()

        // 1. Insert materials
        val matEntities = result.materials.map { m ->
            MaterialUsageEntity(
                projectId = targetProjectId,
                date = now,
                materialName = m.materialName,
                category = m.category,
                quantity = m.quantity,
                unit = m.unit,
                unitPrice = m.unitPrice,
                totalCost = m.totalCost,
                supplier = m.supplier,
                invoiceOrNote = m.notes.ifBlank { "Dari Chat Telegram Mandor" },
                source = if (result.isParsedViaAi) "AI (Gemini)" else "Telegram Bot"
            )
        }
        if (matEntities.isNotEmpty()) {
            materialDao.insertMaterials(matEntities)
        }

        // 2. Insert labor wages
        val laborEntities = result.labors.map { l ->
            LaborWageEntity(
                projectId = targetProjectId,
                date = now,
                workerRole = l.workerRole,
                workerCount = l.workerCount,
                durationHOK = l.durationHOK,
                wagePerUnit = l.wagePerUnit,
                totalWage = l.totalWage,
                taskDescription = l.taskDescription.ifBlank { "Pekerjaan lapangan harian" },
                foremanName = senderName,
                source = if (result.isParsedViaAi) "AI (Gemini)" else "Telegram Bot"
            )
        }
        if (laborEntities.isNotEmpty()) {
            laborDao.insertLaborWages(laborEntities)
        }

        // 3. Update project progress if milestone provided
        result.progressPercent?.let { newProg ->
            projectDao.updateProgress(targetProjectId, newProg, now)
            progressDao.insertMilestone(
                ProgressMilestoneEntity(
                    projectId = targetProjectId,
                    date = now,
                    progressPercent = newProg,
                    milestoneTitle = result.workNotes.take(60).ifBlank { "Update Progress Lapangan" },
                    workNotes = result.workNotes,
                    weatherCondition = result.weatherCondition,
                    recordedBy = senderName
                )
            )
        }

        // 4. Record Telegram log
        val projects = allProjects.firstOrNull() ?: emptyList()
        val pName = projects.firstOrNull { it.id == targetProjectId }?.name ?: "Proyek #$targetProjectId"
        val logId = telegramDao.insertMessage(
            TelegramMessageEntity(
                senderName = senderName,
                rawText = rawText,
                receivedAt = now,
                parseStatus = "SUCCESS",
                detectedProjectId = targetProjectId,
                detectedProjectName = pName,
                parsedSummary = "Material: ${result.materials.size} item, Upah: ${result.labors.size} kelompok, Progress: ${result.progressPercent?.let { "$it%" } ?: "-"}",
                materialCountExtracted = result.materials.size,
                laborCountExtracted = result.labors.size
            )
        )

        return logId
    }

    /**
     * Poll Telegram Bot updates and parse new ones
     */
    suspend fun fetchAndProcessTelegramUpdates(botToken: String): Result<Int> {
        val result = telegramService.getUpdates(botToken)
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull() ?: Exception("Gagal mengambil update Telegram"))
        }

        val messages = result.getOrNull() ?: emptyList()
        if (messages.isEmpty()) {
            return Result.success(0)
        }

        val projects = allProjects.firstOrNull() ?: emptyList()
        var processedCount = 0

        for (msg in messages) {
            val parsed = parseIncomingChat(msg.text)
            val projId = parsed.matchedProjectId ?: projects.firstOrNull()?.id ?: 1L
            commitParsedResult(
                result = parsed,
                targetProjectId = projId,
                rawText = msg.text,
                senderName = msg.senderName
            )
            processedCount++
        }

        return Result.success(processedCount)
    }

    /**
     * Run in-depth AI Analysis on selected project
     */
    suspend fun runAiProjectAnalysis(
        projectId: Long,
        analysisMode: String
    ): AIAnalysisEntity {
        val projects = allProjects.firstOrNull() ?: emptyList()
        val project = projects.firstOrNull { it.id == projectId }
            ?: throw IllegalArgumentException("Proyek tidak ditemukan")

        val materials = materialDao.getMaterialsByProjectFlow(projectId).firstOrNull() ?: emptyList()
        val wages = laborDao.getLaborWagesByProjectFlow(projectId).firstOrNull() ?: emptyList()

        val totalMat = materials.sumOf { it.totalCost }
        val totalWage = wages.sumOf { it.totalWage }

        val matsSummary = if (materials.isEmpty()) "Belum ada catatan material." else {
            materials.take(8).joinToString("\n") { m ->
                "- ${m.materialName}: ${m.quantity} ${m.unit} @ Rp ${String.format("%,.0f", m.unitPrice)} = Rp ${String.format("%,.0f", m.totalCost)} (${m.category})"
            }
        }

        val wagesSummary = if (wages.isEmpty()) "Belum ada catatan upah." else {
            wages.take(8).joinToString("\n") { w ->
                "- ${w.workerRole} (${w.workerCount} orang): Rp ${String.format("%,.0f", w.totalWage)} - ${w.taskDescription}"
            }
        }

        val markdownOutput = geminiService.generateDeepProjectAnalysis(
            projectName = project.name,
            budgetRAB = project.budgetRAB,
            currentProgress = project.progressPercent,
            totalMaterialCost = totalMat,
            totalLaborCost = totalWage,
            materialsSummary = matsSummary,
            laborSummary = wagesSummary,
            analysisMode = analysisMode
        )

        val entity = AIAnalysisEntity(
            projectId = projectId,
            projectName = project.name,
            generatedAt = System.currentTimeMillis(),
            period = "Mingguan",
            analysisType = analysisMode,
            title = "Analisa $analysisMode: ${project.name}",
            contentMarkdown = markdownOutput,
            costEfficiencyScore = if (totalMat + totalWage > project.budgetRAB) 58 else 88,
            riskLevel = if (totalMat + totalWage > project.budgetRAB * 0.9) "Tinggi" else "Rendah"
        )

        aiAnalysisDao.insertAnalysis(entity)
        return entity
    }

    /**
     * Sync with Armbian MySQL server
     */
    suspend fun syncToArmbian(config: ArmbianServerConfig): SyncResult {
        val projects = allProjects.firstOrNull() ?: emptyList()
        val materials = allMaterials.firstOrNull() ?: emptyList()
        val wages = allLaborWages.firstOrNull() ?: emptyList()

        return armbianService.pushDataToArmbian(config, projects, materials, wages)
    }
}
