
package com.zora.sports

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FootballRepository {

    private fun today(): String {
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        )

        formatter.timeZone =
            TimeZone.getTimeZone("Asia/Jakarta")

        return formatter.format(Date())
    }

    private fun checkApiError(errors: Any?) {
        if (errors == null) return

        val message = errors.toString()

        if (
            message != "[]" &&
            message != "{}" &&
            message.isNotBlank()
        ) {
            throw IllegalStateException(
                "Kesalahan API: $message"
            )
        }
    }

    private fun validMatches(
        response: FootballResponse<FixtureItem>
    ): List<FixtureItem> {
        checkApiError(response.errors)

        return response.response.orEmpty().filter { match ->
            match.fixture?.status?.short != null &&
            match.league?.name != null &&
            match.teams?.home?.name != null &&
            match.teams?.away?.name != null
        }
    }

    suspend fun getTodayMatches(): List<FixtureItem> {
        val result = FootballApiClient.api.getFixtures(today())
        return validMatches(result)
    }

    suspend fun getLiveMatches(): List<FixtureItem> {
        val result = FootballApiClient.api.getLiveFixtures()
        return validMatches(result)
    }

    suspend fun getStandings(
        leagueId: Int,
        season: Int
    ): List<StandingTeam> {

        val result = FootballApiClient.api.getStandings(
            leagueId,
            season
        )

        checkApiError(result.errors)

        val league = result.response.orEmpty().firstOrNull()
            ?: return emptyList()

        return league.league
            ?.standings
            ?.firstOrNull()
            .orEmpty()
    }

    suspend fun getLineups(
        fixtureId: Int
    ): List<FixtureLineup> {
        val result =
            FootballApiClient.api.getLineups(fixtureId)

        checkApiError(result.errors)

        return result.response.orEmpty()
    }
}
