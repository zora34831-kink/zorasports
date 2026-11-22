
package com.zora.sports

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.Interceptor

interface FootballApi {
    @GET("fixtures")
    suspend fun getFixtures(
        @Query("date") date: String
    ): FootballResponse

    @GET("fixtures")
    suspend fun getLiveFixtures(
        @Query("live") live: String = "all"
    ): FootballResponse
}

object FootballRepository {

    private val client = OkHttpClient.Builder()
        .addInterceptor(
            Interceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .addHeader(
                        "x-apisports-key",
                        BuildConfig.FOOTBALL_API_KEY
                    )
                    .build()

                chain.proceed(request)
            }
        )
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://v3.football.api-sports.io/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FootballApi::class.java)

    suspend fun today(): List<FixtureItem> {
        val date = java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.US
        ).format(java.util.Date())

        return api.getFixtures(date).response
    }

    suspend fun live(): List<FixtureItem> {
        return api.getLiveFixtures().response
    }
}
