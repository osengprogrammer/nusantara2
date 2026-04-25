package com.azuratech.azuratime.domain.face.usecase

import android.app.Application
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuraengine.face.RegisterResult
import com.azuratech.azuratime.domain.media.PhotoStorageUtils
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.ml.matcher.FaceEngine
import com.azuratech.azuratime.data.local.FaceCache
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterFaceUseCaseTest {

    @MockK
    lateinit var application: Application

    @MockK
    lateinit var localDataSource: FaceLocalDataSource

    @MockK
    lateinit var remoteDataSource: FaceRemoteDataSource

    @MockK
    lateinit var sessionManager: SessionManager

    @MockK
    lateinit var syncFaces: SyncFacesUseCase

    @MockK
    lateinit var photoStorageUtils: PhotoStorageUtils

    private lateinit var useCase: RegisterFaceUseCase

    private val schoolId = "test_school"
    private val classId = "test_class"
    private val studentId = "student1"
    private val faceId = "$classId--$studentId"
    private val name = "John Doe"
    private val embedding = floatArrayOf(0.1f, 0.2f)
    private val photoBytes = byteArrayOf(1, 2, 3)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = RegisterFaceUseCase(
            application,
            localDataSource,
            remoteDataSource,
            sessionManager,
            syncFaces,
            photoStorageUtils
        )
        
        mockkObject(FaceEngine)
        mockkObject(FaceCache)
        
        every { sessionManager.getActiveSchoolId() } returns schoolId
        coEvery { syncFaces() } returns Result.Success(Unit)
        coEvery { localDataSource.getAllFacesForScanningList(any()) } returns emptyList()
        coEvery { localDataSource.getFaceById(any(), any()) } returns null
        coEvery { photoStorageUtils.saveFacePhoto(any(), any()) } returns "local_url"
        coEvery { remoteDataSource.uploadFacePhoto(any(), any(), any()) } returns Result.Success("remote_url")
        coEvery { localDataSource.upsertFace(any()) } returns Unit
        coEvery { localDataSource.insertAssignment(any()) } returns Unit
        coEvery { remoteDataSource.bulkSyncFaces(any(), any()) } returns Result.Success(Unit)
        coEvery { remoteDataSource.syncFaceAssignment(any()) } returns Result.Success(Unit)
        coEvery { FaceCache.refresh(any(), any()) } returns emptyList()
    }

    @Test
    fun `invoke should return Success when registration and sync are successful`() = runTest {
        // Arrange
        every { FaceEngine.findBestMatch(any(), any(), true) } returns FaceEngine.MatchResult.NoMatch

        // Act
        val result = useCase(studentId, classId, name, embedding, photoBytes)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(RegisterResult.Success, (result as Result.Success).data)
        
        coVerify { localDataSource.upsertFace(any()) }
        coVerify { localDataSource.insertAssignment(any()) }
        coVerify { remoteDataSource.bulkSyncFaces(schoolId, any()) }
        coVerify { remoteDataSource.syncFaceAssignment(any()) }
        coVerify { FaceCache.refresh(application, schoolId) }
    }

    @Test
    fun `invoke should return Duplicate when FaceEngine finds a match`() = runTest {
        // Arrange
        val existingName = "Existing Student"
        every { FaceEngine.findBestMatch(any(), any(), true) } returns FaceEngine.MatchResult.DuplicateFound(existingName, 0.1f)
        coEvery { localDataSource.getAllFacesForScanningList(schoolId) } returns listOf(
            FaceEntity(faceId = "ext1", name = existingName, embedding = floatArrayOf(0.1f), schoolId = schoolId)
        )

        // Act
        val result = useCase(studentId, classId, name, embedding, photoBytes)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(RegisterResult.Duplicate(existingName), (result as Result.Success).data)
        
        coVerify(exactly = 0) { localDataSource.upsertFace(any()) }
    }

    @Test
    fun `invoke should return Duplicate when faceId already exists in local DB`() = runTest {
        // Arrange
        every { FaceEngine.findBestMatch(any(), any(), true) } returns FaceEngine.MatchResult.NoMatch
        coEvery { localDataSource.getFaceById(faceId, schoolId) } returns FaceEntity(faceId = faceId, name = name, schoolId = schoolId)

        // Act
        val result = useCase(studentId, classId, name, embedding, photoBytes)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(RegisterResult.Duplicate(name), (result as Result.Success).data)
        
        coVerify(exactly = 0) { localDataSource.upsertFace(any()) }
    }

    @Test
    fun `invoke should return Success even if cloud sync fails`() = runTest {
        // Arrange
        coEvery { remoteDataSource.bulkSyncFaces(any(), any()) } returns Result.Failure(AppError.Network("Sync failed"))
        every { FaceEngine.findBestMatch(any(), any(), true) } returns FaceEngine.MatchResult.NoMatch

        // Act
        val result = useCase(studentId, classId, name, embedding, photoBytes)

        // Assert
        assertTrue(result is Result.Success)
        assertEquals(RegisterResult.Success, (result as Result.Success).data)
        
        coVerify { localDataSource.upsertFace(any()) } // Should still save locally
    }
}
