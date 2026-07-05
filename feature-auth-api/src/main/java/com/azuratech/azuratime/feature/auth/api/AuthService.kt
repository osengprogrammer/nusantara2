package com.azuratech.azuratime.feature.auth.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("status")
    suspend fun getStatus(@Query("userId") userId: String): AuthStatusResponse
}
