package com.azuratech.azuratime.data.repository

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.core.session.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 FACE ASSIGNMENT REPOSITORY
 * Hilt-ready Singleton.
 */
import com.azuratech.azuratime.domain.assignment.usecase.*

@Singleton
class FaceAssignmentRepository @Inject constructor(
    private val syncAssignmentsUseCase: SyncAssignmentsUseCase,
    private val assignStudentToClassUseCase: AssignStudentToClassUseCase,
    private val removeStudentFromClassUseCase: RemoveStudentFromClassUseCase
) {

    @Deprecated("Route through SyncAssignmentsUseCase")
    suspend fun performAssignmentSync() = syncAssignmentsUseCase()

    @Deprecated("Route through AssignStudentToClassUseCase")
    suspend fun assignToClass(faceId: String, classId: String) = assignStudentToClassUseCase(faceId, classId)

    @Deprecated("Route through RemoveStudentFromClassUseCase")
    suspend fun removeSpecificAssignment(faceId: String, classId: String) = removeStudentFromClassUseCase(faceId, classId)

    @Deprecated("Route through RemoveStudentFromClassUseCase")
    suspend fun removeAllAssignmentsForFace(faceId: String) = removeStudentFromClassUseCase.removeAll(faceId)
}
