package com.azuratech.azuratime.core.domain.repository

import com.azuratech.azuratime.core.result.Result

interface SecurityRepository {
    suspend fun validateSecurityEnvelope(): Result<Int>
    suspend fun refreshIsoKeyFromServer(): Result<String>
}
