
package com.zora.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

private val ExtraNavy
    get() = ZoraThemeColors.background

private val ExtraPanel
    get() = ZoraThemeColors.panel

private val ExtraCyan
    get() = ZoraThemeColors.accent

private val ExtraWhite
    get() = ZoraThemeColors.text

private val ExtraMuted
    get() = ZoraThemeColors.muted

private val ExtraBorder
    get() = ZoraThemeColors.border

private data class StandingsLeagueOption(
    val name: String,
    val apiNinjasId: String? = null,
    val footballId: Int? = null
)

private data class StandingsDisplayRow(
    val rank: Int,
    val teamName: String,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsDiff: Int,
    val points: Int
)

private data class NinjasStandingsResult(
    val teams: List<StandingsDisplayRow>,
    val season: String
)

// Masukkan API Key API Ninjas milik lu di sini.
// Jangan bagikan API Key ke orang lain.
private const val API_NINJAS_KEY = "juNpvcVMKDsxtsJbOVhZLB3FVbvOdB0xXaQzhozG"

@Composable
fun StandingsSection(
    modifier: Modifier = Modifier
) {
    val leagues = listOf(
        StandingsLeagueOption(
            name = "Premier League",
            apiNinjasId = "epl",
            footballId = 39
        ),
        StandingsLeagueOption(
            name = "La Liga",
            apiNinjasId = "laliga",
            footballId = 140
        ),
        StandingsLeagueOption(
            name = "Serie A",
            apiNinjasId = "seriea",
            footballId = 135
        ),
        StandingsLeagueOption(
            name = "Bundesliga",
            apiNinjasId = "bundesliga",
            footballId = 78
        ),
        StandingsLeagueOption(
            name = "Ligue 1",
            apiNinjasId = "ligue1",
            footballId = 61
        ),
        StandingsLeagueOption(
            name = "Champions League",
            apiNinjasId = "ucl"
        ),
        StandingsLeagueOption(
            name = "Europa League",
            apiNinjasId = "uel"
        ),
        StandingsLeagueOption(
            name = "MLS",
            apiNinjasId = "mls"
        ),
        StandingsLeagueOption(
            name = "Liga 1 Indonesia",
            footballId = 274
        )
    )

    var selectedLeague by remember {
        mutableStateOf(leagues[0])
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var standings by remember {
        mutableStateOf<List<StandingsDisplayRow>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var seasonLabel by remember {
        mutableStateOf("MUSIM BERJALAN")
    }

    var refresh by remember {
        mutableIntStateOf(0)
    }

    // Salin delegated state ke variabel lokal.
    // Ini memperbaiki error smart cast Kotlin.
    val currentLeague = selectedLeague

    LaunchedEffect(currentLeague, refresh) {
        loading = true
        error = null
        standings = emptyList()

        try {
            val result = withTimeout(25000L) {

                // Variabel lokal agar smart cast bisa dilakukan.
                val apiNinjasId = currentLeague.apiNinjasId

                if (apiNinjasId != null) {

                    val response = fetchNinjasStandings(
                        apiNinjasId
                    )

                    seasonLabel = response.season.ifBlank {
                        "MUSIM BERJALAN"
                    }

                    response.teams

                } else {

                    val footballId = currentLeague.footballId
                        ?: throw Exception("ID liga tidak tersedia.")

                    val response = FootballRepository.getStandings(
                        leagueId = footballId,
                        season = 2026
                    )

                    seasonLabel = "MUSIM 2026"

                    response.map { item ->
                        StandingsDisplayRow(
                            rank = item.rank,
                            teamName = item.team?.name ?: "Klub",
                            played = item.all?.played ?: 0,
                            won = item.all?.win ?: 0,
                            drawn = item.all?.draw ?: 0,
                            lost = item.all?.lose ?: 0,
                            goalsDiff = item.goalsDiff,
                            points = item.points
                        )
                    }
                }
            }

            standings = result

            if (result.isEmpty()) {
                error = "Data klasemen belum tersedia untuk kompetisi ini."
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            error = e.message ?: "Gagal mengambil data klasemen."

        } finally {
            loading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ExtraNavy)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                "KLASEMEN",
                color = ExtraWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Posisi klub dalam kompetisi",
                color = ExtraMuted,
                fontSize = 12.sp
            )
        }

        Box {
            OutlinedButton(
                onClick = {
                    expanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ExtraCyan
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    ExtraBorder
                )
            ) {
                Text(
                    currentLeague.name,
                    modifier = Modifier.weight(1f)
                )

                Text("⌄")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                containerColor = ExtraPanel
            ) {
                leagues.forEach { league ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                league.name,
                                color = ExtraWhite
                            )
                        },
                        onClick = {
                            selectedLeague = league
                            expanded = false
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                seasonLabel,
                color = ExtraCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = { refresh++ },
                enabled = !loading
            ) {
                Text(
                    "MUAT ULANG",
                    color = ExtraCyan,
                    fontSize = 10.sp
                )
            }
        }

        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(35.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = ExtraCyan
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Mengambil data klasemen...",
                            color = ExtraMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            ExtraPanel,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        error ?: "Terjadi kesalahan",
                        color = ExtraMuted,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    TextButton(
                        onClick = { refresh++ }
                    ) {
                        Text(
                            "COBA LAGI",
                            color = ExtraCyan
                        )
                    }
                }
            }

            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            ExtraPanel,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "POS",
                        color = ExtraMuted,
                        modifier = Modifier.width(34.dp),
                        fontSize = 10.sp
                    )

                    Text(
                        "KLUB",
                        color = ExtraMuted,
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp
                    )

                    Text(
                        "M",
                        color = ExtraMuted,
                        modifier = Modifier.width(25.dp),
                        fontSize = 10.sp
                    )

                    Text(
                        "SG",
                        color = ExtraMuted,
                        modifier = Modifier.width(30.dp),
                        fontSize = 10.sp
                    )

                    Text(
                        "POIN",
                        color = ExtraCyan,
                        modifier = Modifier.width(38.dp),
                        fontSize = 10.sp
                    )
                }

                standings.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                ExtraPanel,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 14.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.rank.toString(),
                            color = ExtraCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(34.dp)
                        )

                        Text(
                            item.teamName,
                            color = ExtraWhite,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            item.played.toString(),
                            color = ExtraMuted,
                            modifier = Modifier.width(25.dp),
                            fontSize = 11.sp
                        )

                        Text(
                            item.goalsDiff.toString(),
                            color = ExtraMuted,
                            modifier = Modifier.width(30.dp),
                            fontSize = 11.sp
                        )

                        Text(
                            item.points.toString(),
                            color = ExtraCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(38.dp),
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    "M = Main | SG = Selisih gol",
                    color = ExtraMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private suspend fun fetchNinjasStandings(
    leagueId: String
): NinjasStandingsResult = withContext(Dispatchers.IO) {

    if (
        API_NINJAS_KEY.isBlank() ||
        API_NINJAS_KEY == "juNpvcVMKDsxtsJbOVhZLB3FVbvOdB0xXaQzhozG"
    ) {
        throw Exception("API Key API Ninjas belum dimasukkan.")
    }

    val url = URL(
        "https://api.api-ninjas.com/v1/soccerstandings?league=$leagueId"
    )

    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"

        connection.setRequestProperty(
            "X-Api-Key",
            API_NINJAS_KEY
        )

        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        val responseCode = connection.responseCode

        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            throw Exception(
                when (responseCode) {
                    401, 403 ->
                        "API Key tidak valid atau akses ditolak."

                    429 ->
                        "Batas penggunaan API tercapai."

                    else ->
                        "API Ninjas gagal. Kode: $responseCode $errorBody"
                }
            )
        }

        val response = connection.inputStream
            .bufferedReader()
            .use { it.readText() }

        val array = JSONArray(response)

        val teams = mutableListOf<StandingsDisplayRow>()
        var season = ""

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue

            if (season.isBlank()) {
                season = item.optString(
                    "season_label",
                    item.optString("season", "")
                )
            }

            val teamObject = item.optJSONObject("team")

            val teamName = teamObject?.optString("name")
                ?: item.optString("team", "Klub")

            teams.add(
                StandingsDisplayRow(
                    rank = item.optInt("league_rank", i + 1),
                    teamName = teamName,
                    played = item.optInt("games_played"),
                    won = item.optInt("wins"),
                    drawn = item.optInt("draws"),
                    lost = item.optInt("losses"),
                    goalsDiff = item.optInt("goal_difference"),
                    points = item.optInt("points")
                )
            )
        }

        NinjasStandingsResult(
            teams = teams.sortedBy { it.rank },
            season = season
        )

    } finally {
        connection.disconnect()
    }
}

@Composable
fun LineupDialog(
    fixtureId: Int,
    onDismiss: () -> Unit
) {
    var lineups by remember(fixtureId) {
        mutableStateOf<List<FixtureLineup>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(fixtureId) {
        loading = true
        error = null

        try {
            lineups = withTimeout(20000L) {
                FootballRepository.getLineups(fixtureId)
            }

            if (lineups.isEmpty()) {
                error = "Susunan pemain belum tersedia."
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            error = e.message ?: "Gagal mengambil susunan pemain."

        } finally {
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ExtraNavy,

        title = {
            Text(
                "SUSUNAN PEMAIN",
                color = ExtraWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },

        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(25.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = ExtraCyan
                            )
                        }
                    }

                    error != null -> {
                        Text(
                            error ?: "Data tidak tersedia",
                            color = ExtraMuted,
                            fontSize = 12.sp
                        )
                    }

                    else -> {
                        lineups.forEach { lineup ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        ExtraPanel,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(13.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    lineup.team?.name ?: "Tim",
                                    color = ExtraCyan,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "Formasi: ${lineup.formation ?: "-"}",
                                    color = ExtraMuted,
                                    fontSize = 11.sp
                                )

                                Text(
                                    "Pelatih: ${lineup.coach?.name ?: "-"}",
                                    color = ExtraMuted,
                                    fontSize = 11.sp
                                )

                                HorizontalDivider(
                                    color = ExtraBorder.copy(alpha = 0.7f)
                                )

                                Text(
                                    "SEBELAS PEMAIN UTAMA",
                                    color = ExtraWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )

                                lineup.startXI.orEmpty().forEach {
                                    PlayerLine(it.player)
                                }

                                HorizontalDivider(
                                    color = ExtraBorder.copy(alpha = 0.7f)
                                )

                                Text(
                                    "PEMAIN CADANGAN",
                                    color = ExtraWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )

                                lineup.substitutes.orEmpty().forEach {
                                    PlayerLine(it.player)
                                }
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "TUTUP",
                    color = ExtraCyan
                )
            }
        }
    )
}

@Composable
private fun PlayerLine(
    player: LineupPlayer?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "${player?.number ?: "-"}  ${player?.name ?: "Pemain"}",
            color = ExtraWhite,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            player?.pos ?: "-",
            color = ExtraMuted,
            fontSize = 10.sp
        )
    }
}
