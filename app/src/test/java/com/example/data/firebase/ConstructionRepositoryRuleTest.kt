package com.example.data.firebase

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstructionRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createProject_validPayload_createsDocumentAndReturnsId() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreConstructionRepository(firestore)

        val project = CloudProject(
            userId = uid,
            name = "Pembangunan Gedung Rektorat Subang",
            budgetRAB = 1500000000.0,
            progressPercent = 12.0,
            status = "Aktif"
        )
        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveProject(uid, project) }
        assertTrue(createResult.isSuccess)
        val projId = createResult.getOrThrow()
        assertNotNull(projId)
    }

    @Test
    fun observeProjects_authenticatedOwner_returnsMatchingProjects() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreConstructionRepository(firestore)

        val project = CloudProject(
            userId = uid,
            name = "Pekerjaan Struktur As-Syifa",
            budgetRAB = 500000000.0,
            progressPercent = 5.0,
            status = "Aktif"
        )
        val projId = repository.saveProject(uid, project).getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProjects(uid).first { list: List<CloudProject> -> list.any { it.id == projId } }
        }
        assertTrue(emitted.any { it.id == projId })
    }

    @Test
    fun saveRabItem_authenticatedOwner_savesSuccessfully() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreConstructionRepository(firestore)

        val rabItem = CloudRabItem(
            userId = uid,
            projectId = "proj_test_123",
            floorLevel = "LANTAI 1",
            categoryCode = "A",
            categoryName = "Pekerjaan Struktur",
            workDescription = "Pek. Beton Bertulang K-300",
            volume = 45.0,
            unit = "m3",
            unitPrice = 1250000.0,
            totalPrice = 56250000.0
        )
        val saveResult = repository.saveRabItem(uid, rabItem)
        assertTrue(saveResult.isSuccess)
    }

    @Test
    fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        var caughtException: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("projects").get().await()
            }
        } catch (e: FirebaseFirestoreException) {
            caughtException = e
        }
        assertNotNull(caughtException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caughtException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@example.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
