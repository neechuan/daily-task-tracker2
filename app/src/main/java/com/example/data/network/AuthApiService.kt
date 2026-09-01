package com.example.data.network

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class AuthApiRequest(
    val title: String,
    val body: String,
    val userId: Int = 1
)

@JsonClass(generateAdapter = true)
data class AuthApiResponse(
    val id: Int = 0,
    val title: String = "",
    val body: String = "",
    val userId: Int = 1
)

interface AuthApiService {
    @POST("posts")
    suspend fun mockLogin(@Body request: AuthApiRequest): AuthApiResponse

    @POST("posts")
    suspend fun mockSignUp(@Body request: AuthApiRequest): AuthApiResponse

    @POST("posts")
    suspend fun mockLogout(@Body request: AuthApiRequest): AuthApiResponse
}

object AuthNetworkClient {
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}
