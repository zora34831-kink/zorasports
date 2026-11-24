
package com.zora.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class LeagueOption(
    val name: String,
    val apiId: String
)

private data class StandingTeam(
    val position: Int,
    val name: String,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val points: Int
)

private val availableLeagues = listOf(
    LeagueOption("Premier League", "epl"),
    LeagueOption("La Liga", "laliga"),
    LeagueOption("Bundesliga", "bundesliga"),
    LeagueOption("Serie A", "seriea"),
    LeagueOption("Ligue 1", "ligue1"),
    LeagueOption("Champions League", "ucl"),
    LeagueOption("Europa League", "uel"),
    LeagueOption("MLS", "mls")
)

private val standingsBackground = Color(0xFF071321)
private val standingsPanel = Color(0xFF102337)
private val standingsAccent = Color(0xFF35C7E8)
private val standingsText = Color(0xFFF3F7FA)
private val standingsMuted = Color(0xFF91A5B7)

@Composable
fun StandingsSection(
    modifier: Modifier = Modifier
) {
    var selectedLeague by remember {
        mutableStateOf(availableLeagues.first())
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var teams by remember {
        mutableStateOf<List<StandingTeam>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var seasonLabel by remember {
        mutableStateOf("")
    }

    val scope = rememberCoroutineScope()

    fun loadStandings() {
        scope.launch {
            loading = true
            errorMessage = ""
            teams = emptyList()

            try {
                val result = withContext(Dispatchers.IO) {
                    fetchApiNinjasStandings(selectedLeague.apiId)
                }

                teams = result.first
                seasonLabel = result.second

                if (result.first.isEmpty()) {
                    errorMessage = "Data klasemen belum tersedia."
                }
            } catch (e: Exception) {
                errorMessage = e.message
                    ?: "Gagal mengambil data klasemen."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(selectedLeague.apiId) {
        loadStandings()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(standingsBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "KLASEMEN LIGA",
            color = standingsText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Posisi dan statistik setiap tim",
            color = standingsMuted,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(18.dp))

        Box {
            Surface(
                onClick = { expanded = true },
                color = standingsPanel,
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 15.dp,
                        vertical = 16.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "PILIH KOMPETISI",
                            color = standingsMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            selectedLeague.name,
                            color = standingsText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "⌄",
                        color = standingsAccent,
                        fontSize = 23.sp
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(standingsPanel)
                    .heightIn(max = 350.dp)
            ) {
                availableLeagues.forEach { league ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                league.name,
                                color = standingsText
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

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                seasonLabel.ifBlank { "Klasemen musim berjalan" },
                color = standingsMuted,
                fontSize = 10.sp
            )

            IconButton(
                onClick = { loadStandings() },
                enabled = !loading
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Muat ulang",
                    tint = standingsAccent
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = standingsAccent
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Mengambil data klasemen...",
                        color = standingsMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else if (errorMessage.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = standingsMuted,
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        errorMessage,
                        color = standingsText,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { loadStandings() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = standingsAccent,
                            contentColor = standingsBackground
                        )
                    ) {
                        Text("COBA LAGI")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                StandingTableHeader()

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(
                        teams,
                        key = { index, team ->
                            "${team.name}_$index"
                        }
                    ) { index, team ->
                        StandingTeamRow(
                            team = team,
                            index = index
                        )
                    }
                }
            }
        }

        Text(
            "DATA KLASemen • API NINJAS",
            color = standingsMuted,
            fontSize = 9.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun StandingTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                standingsPanel,
                RoundedCornerShape(10.dp)
            )
            .padding(
                horizontal = 9.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "POS",
            color = standingsMuted,
            modifier = Modifier.width(30.dp),
            fontSize = 9.sp
        )

        Text(
            "KLUB",
            color = standingsMuted,
            modifier = Modifier.weight(1f),
            fontSize = 9.sp
        )

        listOf("M", "M", "S", "K", "SG", "P").forEachIndexed { index, label ->
            Text(
                label,
                color = standingsMuted,
                modifier = Modifier.width(
                    if (index == 4) 35.dp else 25.dp
                ),
                fontSize = 9.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun StandingTeamRow(
    team: StandingTeam,
    index: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                standingsPanel,
                RoundedCornerShape(10.dp)
            )
            .padding(
                horizontal = 9.dp,
                vertical = 13.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            team.position.toString(),
            color = when (team.position) {
                in 1..4 -> standingsAccent
                else -> standingsMuted
            },
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.width(30.dp)
        )

        Text(
            team.name,
            color = standingsText,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )

        listOf(
            team.played,
            team.won,
            team.drawn,
            team.lost,
            team.goalDifference,
            team.points
        ).forEachIndexed { column, value ->
            Text(
                value.toString(),
                color = if (column == 5) {
                    standingsAccent
                } else {
                    standingsText
                },
                fontSize = 9.sp,
                fontWeight = if (column == 5) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
                modifier = Modifier.width(
                    if (column == 4) 35.dp else 25.dp
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private suspend fun fetchApiNinjasStandings(
    leagueId: String
): Pair<List<StandingTeam>, String> {

    // Untuk pengujian saja. Jangan simpan API key produksi di APK.
    val apiKey = "MASUKKAN_API_KEY_DI_SINI"

    if (
        apiKey.isBlank() ||
        apiKey == "MASUKKAN_API_KEY_DI_SINI"
    ) {
        throw Exception("API Key belum diatur.")
    }

    val url = URL(
        "https://api.api-ninjas.com/v1/soccerstandings?league=$leagueId"
    )

    val connection = url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.setRequestProperty("X-Api-Key", apiKey)
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        val responseCode = connection.responseCode

        val response = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use {
                it.readText()
            }
        } else {
            val message = connection.errorStream
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()

            throw Exception(
                when (responseCode) {
                    401, 403 -> "API Key tidak valid atau akses ditolak."
                    429 -> "Batas penggunaan API tercapai."
                    else -> "Gagal mengambil data. Kode: $responseCode $message"
                }
            )
        }

        val jsonArray = when {
            response.trimStart().startsWith("[") ->
                JSONArray(response)

            else -> {
                val obj = JSONObject(response)
                when {
                    obj.has("standings") ->
                        obj.getJSONArray("standings")
                    obj.has("data") ->
                        obj.getJSONArray("data")
                    else -> JSONArray()
                }
            }
        }

        val result = mutableListOf<StandingTeam>()
        var season = ""

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue

            if (season.isBlank()) {
                season = item.optString(
                    "season_label",
                    item.optString("season", "")
                )
            }

            val teamObject = item.optJSONObject("team")
            val teamName = if (teamObject != null) {
                teamObject.optString("name", "Tim tidak diketahui")
            } else {
                item.optString("team", "Tim tidak diketahui")
            }

            result.add(
                StandingTeam(
                    position = item.optInt(
                        "league_rank",
                        i + 1
                    ),
                    name = teamName,
                    played = item.optInt("games_played"),
                    won = item.optInt("wins"),
                    drawn = item.optInt("draws"),
                    lost = item.optInt("losses"),
                    goalsFor = item.optInt("goals_for"),
                    goalsAgainst = item.optInt("goals_against"),
                    goalDifference = item.optInt("goal_difference"),
                    points = item.optInt("points")
                )
            )
        }

        return result.sortedBy { it.position } to season

    } finally {
        connection.disconnect()
    }
}
