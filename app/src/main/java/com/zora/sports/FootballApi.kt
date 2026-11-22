
package com.zora.sports

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class FootballResponse<T>(
    val response: List<T> = emptyList(),
    val errors: Any? = null
)

data class FixtureItem(
    val fixture: FixtureInfo,
    val league: LeagueInfo,
    val teams: TeamsInfo,
    val goals: GoalsInfo
)

data class FixtureInfo(
    val id: Int,
    val date: String,
    val status: StatusInfo
)

data class StatusInfo(
    val short: String,
    val elapsed: Int?
)

data class LeagueInfo(
    val id: Int,
    val name: String,
    val logo: String?
)

data class TeamsInfo(
    val home: TeamInfo,
    val away: TeamInfo
)

data class TeamInfo(
    val id: Int,
    val name: String,
    val logo: String?
)

data class GoalsInfo(
    val home: Int?,
    val away: Int?
)

data class StandingResponse(
    val league: StandingLeague
)

data class StandingLeague(
    val name: String,
    val standings: List<List<StandingTeam>>
)

data class StandingTeam(
    val rank: Int,
    val team: TeamInfo,
    val points: Int,
    val goalsDiff: Int,
    val all: StandingStats
)

data class StandingStats(
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int
)

interface FootballApi {
    @GET("fixtures")
    suspend fun getFixtures(
        @Query("date") date: String,
        @Query("timezone") timezone: String = "Asia/Jakarta"
    ): FootballResponse<FixtureItem>

    @GET("fixtures")
    suspend fun getLiveFixtures(
        @Query("live") live: String = "all",
        @Query("timezone") timezone: String = "Asia/Jakarta"
    ): FootballResponse<FixtureItem>

    @GET("standings")
    suspend fun getStandings(
        @Query("league") league: Int,
        @Query("season") season: Int
    ): FootballResponse<StandingResponse>
}

object FootballApiClient {

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
                .newBuilder()
                .addHeader(
                    "x-apisports-key",
                    BuildConfig.FOOTBALL_API_KEY
                )
                .build()

            chain.proceed(request)
        }
        .build()

    val api: FootballApi = Retrofit.Builder()
        .baseUrl("https://v3.football.api-sports.io/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FootballApi::class.java)
}
