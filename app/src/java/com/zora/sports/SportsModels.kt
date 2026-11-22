
package com.zora.sports

data class FootballResponse(
    val response: List<FixtureItem> = emptyList()
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
    val name: String,
    val country: String,
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
