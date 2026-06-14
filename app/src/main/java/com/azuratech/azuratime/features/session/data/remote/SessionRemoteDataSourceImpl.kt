package com.azuratech.azuratime.features.session.data.remote

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.SubjectEntity
import com.azuratech.azuratime.features.session.data.local.toClassSessionEntity
import com.azuratech.azuratime.features.session.data.local.toSubjectEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore,
) : SessionRemoteDataSource {

    override suspend fun getSubjectUpdates(schoolId: String): Result<List<SubjectEntity>> {
        return try {
            val snapshot = db.collection("schools").document(schoolId)
                .collection("subjects")
                .get()
                .await()

            val subjects = snapshot.documents.mapNotNull { it.toSubjectEntity(schoolId) }
            Result.Success(subjects)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun syncSubject(subject: SubjectEntity): Result<Unit> {
        return try {
            db.collection("schools").document(subject.schoolId)
                .collection("subjects").document(subject.subjectId)
                .set(subject.toFirestoreMap(), SetOptions.merge())
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteSubject(schoolId: String, subjectId: String): Result<Unit> {
        return try {
            db.collection("schools").document(schoolId)
                .collection("subjects").document(subjectId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun getSessionUpdates(schoolId: String): Result<List<ClassSessionEntity>> {
        return try {
            val snapshot = db.collection("schools").document(schoolId)
                .collection("class_sessions")
                .get()
                .await()

            val sessions = snapshot.documents.mapNotNull { it.toClassSessionEntity(schoolId) }
            Result.Success(sessions)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun syncSession(session: ClassSessionEntity): Result<Unit> {
        return try {
            db.collection("schools").document(session.schoolId)
                .collection("class_sessions").document(session.sessionId)
                .set(session.toFirestoreMap(), SetOptions.merge())
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteSession(schoolId: String, sessionId: String): Result<Unit> {
        return try {
            db.collection("schools").document(schoolId)
                .collection("class_sessions").document(sessionId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
