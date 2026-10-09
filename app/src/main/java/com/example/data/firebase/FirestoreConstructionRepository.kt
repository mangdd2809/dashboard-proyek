package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreConstructionRepository(
    private val firestore: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // --- Projects ---
    fun observeProjects(userId: String): Flow<List<CloudProject>> {
        return firestore.collection("projects")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CloudProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?.copy(id = doc.id)
                }
            }
    }

    suspend fun saveProject(userId: String, project: CloudProject): Result<String> = runCatching {
        val col = firestore.collection("projects")
        val docRef = if (project.id.isNotBlank()) col.document(project.id) else col.document()
        val data = mapOf(
            "userId" to userId,
            "name" to project.name,
            "codeSpk" to project.codeSpk,
            "location" to project.location,
            "clientName" to project.clientName,
            "budgetRAB" to project.budgetRAB,
            "progressPercent" to project.progressPercent,
            "status" to project.status,
            "foremanInCharge" to project.foremanInCharge,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        docRef.id
    }

    suspend fun deleteProject(projectId: String): Result<Unit> = runCatching {
        firestore.collection("projects").document(projectId).delete().await()
    }

    // --- RAB Items ---
    fun observeRabItems(userId: String): Flow<List<CloudRabItem>> {
        return firestore.collection("rab_items")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CloudRabItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?.copy(id = doc.id)
                }
            }
    }

    suspend fun saveRabItem(userId: String, item: CloudRabItem): Result<String> = runCatching {
        val col = firestore.collection("rab_items")
        val docRef = if (item.id.isNotBlank()) col.document(item.id) else col.document()
        val data = mapOf(
            "userId" to userId,
            "projectId" to item.projectId,
            "floorLevel" to item.floorLevel,
            "categoryCode" to item.categoryCode,
            "categoryName" to item.categoryName,
            "itemNumber" to item.itemNumber,
            "workDescription" to item.workDescription,
            "volume" to item.volume,
            "unit" to item.unit,
            "unitPrice" to item.unitPrice,
            "totalPrice" to item.totalPrice,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        docRef.id
    }

    suspend fun deleteRabItem(itemId: String): Result<Unit> = runCatching {
        firestore.collection("rab_items").document(itemId).delete().await()
    }

    // --- User Profile ---
    fun observeUserProfile(userId: String): Flow<CloudUserProfile?> {
        return firestore.collection("users").document(userId)
            .snapshots()
            .map { doc ->
                if (doc.exists()) {
                    doc.toObject(CloudUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
    }

    suspend fun saveUserProfile(userId: String, profile: CloudUserProfile): Result<Unit> = runCatching {
        val docRef = firestore.collection("users").document(userId)
        val data = mapOf(
            "userId" to userId,
            "email" to profile.email,
            "fullName" to profile.fullName,
            "role" to profile.role,
            "phone" to profile.phone,
            "assignedProject" to profile.assignedProject,
            "isActive" to profile.isActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
    }
}
