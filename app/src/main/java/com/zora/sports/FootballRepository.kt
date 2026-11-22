
package com.zora.sports

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FootballRepository {

    private fun today(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date())
    }

    suspend fun getTodayMatches(): List<FixtureItem> {
        return FootballApiClient.api
            .getFixtures(today())
            .response
    }

    suspend fun getLiveMatches(): List<FixtureItem> {
        return FootballApiClient.api
            .getLiveFixtures()
            .response
    }

    suspend fun getStandings(
        leagueId: Int,
        season: Int
    ): List<StandingTeam> {
        val result = FootballApiClient.api
            .getStandings(leagueId, season)

        return result.response
            .firstOrNull()
            ?.league
            ?.standings
            ?.firstOrNull()
            ?: emptyList()
    }
}
