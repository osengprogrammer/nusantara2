package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.data.local.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 REPORT REPOSITORY
 * Thin wrapper for Report Data Sources.
 */
@Singleton
class ReportRepository @Inject constructor(
    database: AppDatabase
) {
    private val checkInRecordDao = database.checkInRecordDao()
    private val faceDao = database.faceDao()
    private val classDao = database.classDao()
    private val faceAssignmentDao = database.faceAssignmentDao()

    // Simple delegation
    fun getCheckInRecordDao() = checkInRecordDao
    fun getFaceDao() = faceDao
    fun getClassDao() = classDao
    fun getFaceAssignmentDao() = faceAssignmentDao
}
