
package com.zora.sports

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class FootballResponse<T>(
    val response: List<T>? = emptyList(),
    val errors: Any? = null
)

data class FixtureItem(
    val fixture: FixtureInfo? = null,
    val league: LeagueInfo? = null,
    val teams: TeamsInfo? = null,
    val goals: GoalsInfo? = null
)

data class FixtureInfo(
    val id: Int? = null,
    val date: String? = null,
    val status: StatusInfo? = null
)

data class StatusInfo(
    val short: String? = null,
    val elapsed: Int? = null
)

data class LeagueInfo(
    val id: Int? = null,
    val name: String? = null,
    val logo: String? = null
)

data class TeamsInfo(
    val home: TeamInfo? = null,
    val away: TeamInfo? = null
)

data class TeamInfo(
    val id: Int? = null,
    val name: String? = null,
    val logo: String? = null
)

data class GoalsInfo(
    val home: Int? = null,
    val away: Int? = null
)

data class StandingResponse(
    val league: StandingLeague? = null
)

data class StandingLeague(
    val name: String? = null,
    val standings: List<List<StandingTeam>>? = null
)

data class StandingTeam(
    val rank: Int = 0,
    val team: TeamInfo? = null,
    val points: Int = 0,
    val goalsDiff: Int = 0,
    val all: StandingStats? = null
)

data class StandingStats(
    val played: Int = 0,
    val win: Int = 0,
    val draw: Int = 0,
    val lose: Int = 0
)

// LINEUP MODELS

data class FixtureLineup(
    val team: TeamInfo? = null,
    val formation: String? = null,
    val startXI: List<LineupPlayerWrapper>? = emptyList(),
    val substitutes: List<LineupPlayerWrapper>? = emptyList(),
    val coach: CoachInfo? = null
)

data class LineupPlayerWrapper(
    val player: LineupPlayer? = null
)

data class LineupPlayer(
    val id: Int? = null,
    val name: String? = null,
    val number: Int? = null,
    val pos: String? = null,
    val grid: String? = null
)

data class CoachInfo(
    val id: Int? = null,
    val name: String? = null,
    val photo: String? = null
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

    @GET("fixtures/lineups")
    suspend fun getLineups(
        @Query("fixture") fixture: Int
    ): FootballResponse<FixtureLineup>
}

object FootballApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
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
