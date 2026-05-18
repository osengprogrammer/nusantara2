package com.azuratech.azuratime.core.domain.repository

import com.azuratech.azuraengine.result.Result

interface SecurityRepository {
    suspend fun validateSecurityEnvelope(): Result<Int>
    suspend fun refreshIsoKeyFromServer(): Result<String>
}
