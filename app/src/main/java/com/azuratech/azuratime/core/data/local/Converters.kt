package com.azuratech.azuratime.core.data.local

import androidx.room.TypeConverter
import com.azuratech.azuratime.features.account.data.local.SchoolMembership
import com.azuratech.azuratime.features.account.domain.model.AccessRequestStatus
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.features.session.domain.model.SessionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Instant
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromFloatArray(array: FloatArray?): String? {
        if (array == null) return null
        val buffer = ByteBuffer.allocate(array.size * 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (value in array) { buffer.putFloat(value) }
        return Base64.encode(buffer.array())
    }

    @TypeConverter
    fun toFloatArray(base64Str: String?): FloatArray? {
        if (base64Str.isNullOrEmpty()) return null
        return try {
            val bytes = Base64.decode(base64Str)
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val array = FloatArray(bytes.size / 4)
            for (i in array.indices) { array[i] = buffer.float }
            array
        } catch (e: Exception) { null }
    }

    @TypeConverter
    fun toLocalDateTimeString(date: LocalDateTime?): String? = date?.toString()

    @TypeConverter
    fun fromLocalDateTimeString(value: String?): LocalDateTime? = value?.let {
        try { LocalDateTime.parse(it) } catch (e: Exception) { null }
    }

    @TypeConverter
    fun toLocalDateString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun fromLocalDateString(value: String?): LocalDate? = value?.let {
        try { LocalDate.parse(it) } catch (e: Exception) { null }
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return try { gson.fromJson(value, type) } catch (e: Exception) { emptyList() }
    }

    @TypeConverter
    fun fromMembershipsMap(map: Map<String, SchoolMembership>?): String? {
        if (map == null) return null
        val type = object : TypeToken<Map<String, SchoolMembership>>() {}.type
        return gson.toJson(map, type)
    }

    @TypeConverter
    fun toMembershipsMap(value: String?): Map<String, SchoolMembership>? {
        if (value.isNullOrEmpty()) return emptyMap()
        val type = object : TypeToken<Map<String, SchoolMembership>>() {}.type
        return try { gson.fromJson(value, type) } catch (e: Exception) { emptyMap() }
    }

    @TypeConverter
    fun fromAccessRequestStatus(status: AccessRequestStatus): String = status.name

    @TypeConverter
    fun toAccessRequestStatus(value: String): AccessRequestStatus = AccessRequestStatus.valueOf(value)

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromSessionType(type: SessionType): String = type.name

    @TypeConverter
    fun toSessionType(value: String): SessionType = try {
        SessionType.valueOf(value)
    } catch (e: Exception) {
        SessionType.ACADEMIC
    }

    @TypeConverter
    fun fromAccountRole(value: com.azuratech.azuratime.core.domain.model.AccountRole): String = value.name

    @TypeConverter
    fun toAccountRole(value: String): com.azuratech.azuratime.core.domain.model.AccountRole? = try { com.azuratech.azuratime.core.domain.model.AccountRole.valueOf(value) } catch (e: Exception) { null }

    @TypeConverter
    fun fromAuthStatus(value: com.azuratech.azuratime.features.auth.ui.AuthStatus): String = value.name

    @TypeConverter
    fun toAuthStatus(value: String): com.azuratech.azuratime.features.auth.ui.AuthStatus? = try { com.azuratech.azuratime.features.auth.ui.AuthStatus.valueOf(value) } catch (e: Exception) { null }

    @TypeConverter
    fun fromMembershipStatus(value: com.azuratech.azuratime.features.account.domain.model.MembershipStatus): String = value.name

    @TypeConverter
    fun toMembershipStatus(value: String): com.azuratech.azuratime.features.account.domain.model.MembershipStatus? = try { com.azuratech.azuratime.features.account.domain.model.MembershipStatus.valueOf(value) } catch (e: Exception) { null }

    @TypeConverter
    fun fromReportTab(value: com.azuratech.azuratime.features.reporting.ui.ReportTab): String = value.name

    @TypeConverter
    fun toReportTab(value: String): com.azuratech.azuratime.features.reporting.ui.ReportTab? = try { com.azuratech.azuratime.features.reporting.ui.ReportTab.valueOf(value) } catch (e: Exception) { null }

    @TypeConverter
    fun fromChatRole(value: com.azuratech.azuratime.features.ai.ui.ChatRole): String = value.name

    @TypeConverter
    fun toChatRole(value: String): com.azuratech.azuratime.features.ai.ui.ChatRole? = try { com.azuratech.azuratime.features.ai.ui.ChatRole.valueOf(value) } catch (e: Exception) { null }

    @TypeConverter
    fun fromEnrollmentStatus(value: com.azuratech.azuratime.features.biometric.ui.enroll.EnrollmentStatus): String = value.name

    @TypeConverter
    fun toEnrollmentStatus(value: String): com.azuratech.azuratime.features.biometric.ui.enroll.EnrollmentStatus? = try { com.azuratech.azuratime.features.biometric.ui.enroll.EnrollmentStatus.valueOf(value) } catch (e: Exception) { null }

    @TypeConverter
    fun fromBiometricStatus(value: com.azuratech.azuratime.features.student.ui.form.BiometricStatus): String = value.name

    @TypeConverter
    fun toBiometricStatus(value: String): com.azuratech.azuratime.features.student.ui.form.BiometricStatus? = try { com.azuratech.azuratime.features.student.ui.form.BiometricStatus.valueOf(value) } catch (e: Exception) { null }

    @TypeConverter
    fun fromAttendanceStatus(value: com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus): String = value.name

    @TypeConverter
    fun toAttendanceStatus(value: String): com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus? = try { com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus.valueOf(value) } catch (e: Exception) { null }

    @TypeConverter
    fun fromScanMode(value: com.azuratech.azuratime.features.attendance.ui.capture.ScanMode): String = value.name

    @TypeConverter
    fun toScanMode(value: String): com.azuratech.azuratime.features.attendance.ui.capture.ScanMode? = try { com.azuratech.azuratime.features.attendance.ui.capture.ScanMode.valueOf(value) } catch (e: Exception) { null }

    @TypeConverter
    fun fromMLDelegateType(value: com.azuratech.azuratime.features.edge.domain.model.MLDelegateType): String = value.name

    @TypeConverter
    fun toMLDelegateType(value: String): com.azuratech.azuratime.features.edge.domain.model.MLDelegateType? = try { com.azuratech.azuratime.features.edge.domain.model.MLDelegateType.valueOf(value) } catch (e: Exception) { null }
}
