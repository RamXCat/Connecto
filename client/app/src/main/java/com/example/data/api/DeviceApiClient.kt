package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object DeviceApiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    // Retrofit requires a base URL even if using dynamic @Url paths.
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: DeviceService = retrofit.create(DeviceService::class.java)

    /**
     * Helper to gracefully normalize user IP or URL inputs.
     * Appends port 5000 if not specified, and ensures http:// or https:// prefix.
     */
    fun buildUrl(ipOrUrl: String, path: String): String {
        val raw = ipOrUrl.trim()
        val prefix = if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            "http://"
        } else {
            ""
        }
        val base = raw.removeSuffix("/")
        val withPort = if (base.contains(":") || base.contains(".io") || base.contains(".net") || base.contains(".com")) {
            base
        } else {
            "$base:5000" // Append default laptop agent port
        }
        return "$prefix$withPort$path"
    }

    fun buildAuthHeader(token: String): String {
        return "Bearer ${token.trim()}"
    }
}
