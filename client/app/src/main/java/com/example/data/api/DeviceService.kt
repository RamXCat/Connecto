package com.example.data.api

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface DeviceService {
    @GET
    suspend fun checkStatus(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): Response<StatusResponse>

    @POST
    suspend fun shutdown(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): Response<Unit>

    @POST
    suspend fun sleep(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): Response<Unit>

    @POST
    suspend fun restart(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): Response<Unit>

    @POST
    suspend fun lock(
        @Url url: String,
        @Header("Authorization") authHeader: String
    ): Response<Unit>
}

@JsonClass(generateAdapter = true)
data class StatusResponse(
    val status: String = "online",
    val hostname: String? = null,
    val platform: String? = null
)
