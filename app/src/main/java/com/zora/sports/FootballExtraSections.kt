
package com.zora.sports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private const val API_NINJAS_KEY = "juNpvcVMKDsxtsJbOVhZLB3FVbvOdB0xXaQzhozG"

// ======================================================
// KLASEMEN
// ======================================================

@Composable
fun StandingsSection(
    modifier: Modifier = Modifier
) {
    val leagues = listOf(
        StandingsLeagueOption("Premier League", "epl", 39),
        StandingsLeagueOption("La Liga", "laliga", 140),
        StandingsLeagueOption("Serie A", "seriea", 135),
        StandingsLeagueOption("Bundesliga", "bundesliga", 78),
        StandingsLeagueOption("Ligue 1", "ligue1", 61),
        StandingsLeagueOption("Champions League", "ucl"),
        StandingsLeagueOption("Europa League", "uel"),
        StandingsLeagueOption("MLS", "mls"),
        StandingsLeagueOption("Liga 1 Indonesia", footballId = 274)
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

    val currentLeague = selectedLeague

    LaunchedEffect(currentLeague, refresh) {
        loading = true
        error = null
        standings = emptyList()

        try {
            val result = withTimeout(25000L) {
                val apiNinjasId = currentLeague.apiNinjasId

                if (apiNinjasId != null) {
                    val response = fetchNinjasStandings(apiNinjasId)

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
                onClick = { expanded = true },
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
                onDismissRequest = { expanded = false },
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
                        CircularProgressIndicator(color = ExtraCyan)

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

                    TextButton(onClick = { refresh++ }) {
                        Text("COBA LAGI", color = ExtraCyan)
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

// ======================================================
// LINEUP DUA TIM
// ======================================================

private data class PitchPlayerPosition(
    val player: LineupPlayer,
    val x: Float,
    val y: Float
)

private fun createPitchPositions(
    lineup: FixtureLineup,
    teamIndex: Int
): List<PitchPlayerPosition> {

    val players = lineup.startXI.orEmpty()
        .mapNotNull { it.player }

    if (players.isEmpty()) return emptyList()

    val parsed = players.map { player ->
        player.grid
            ?.split(":")
            ?.mapNotNull { it.toIntOrNull() }
            ?.takeIf {
                it.size >= 2 && it[0] > 0 && it[1] > 0
            }
    }

    val rows = players.mapIndexed { index, player ->
        parsed[index]?.get(0)
            ?: when (player.pos?.uppercase()) {
                "G" -> 1
                "D" -> 2
                "M" -> 3
                "F" -> 4
                else -> 3
            }
    }

    return players.indices.map { index ->
        val row = rows[index]

        val sameRow = players.indices
            .filter { rows[it] == row }
            .sortedBy { parsed[it]?.get(1) ?: it }

        val positionInRow = sameRow.indexOf(index)
        val countInRow = sameRow.size

        val y = (positionInRow + 1).toFloat() /
            (countInRow + 1).toFloat()

        val maxRow = maxOf(rows.maxOrNull() ?: 4, 4)

        val progress = (row - 1).toFloat() /
            (maxRow - 1).coerceAtLeast(1)

        val x = if (teamIndex == 0) {
            0.07f + progress * 0.37f
        } else {
            0.93f - progress * 0.37f
        }

        PitchPlayerPosition(
            player = players[index],
            x = x.coerceIn(0.06f, 0.94f),
            y = (0.08f + y * 0.84f).coerceIn(0.06f, 0.94f)
        )
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
        modifier = Modifier.fillMaxWidth(),

        title = {
            Text(
                "SUSUNAN PEMAIN",
                color = ExtraWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },

        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 590.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(35.dp),
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
                        val first = lineups.getOrNull(0)
                        val second = lineups.getOrNull(1)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    ExtraPanel,
                                    RoundedCornerShape(13.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    first?.team?.name ?: "Tim 1",
                                    color = ExtraWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    first?.formation ?: "-",
                                    color = Color(0xFF35B9FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                "VS",
                                color = ExtraMuted,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    second?.team?.name ?: "Tim 2",
                                    color = ExtraWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    second?.formation ?: "-",
                                    color = Color(0xFFFF5555),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        SharedFootballPitch(
                            lineups = lineups.take(2)
                        )

                        if (lineups.size == 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                lineups.forEachIndexed { index, lineup ->
                                    CoachAndSubsCard(
                                        lineup = lineup,
                                        teamIndex = index,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Data susunan pemain tim lawan belum tersedia.",
                                color = ExtraMuted,
                                fontSize = 11.sp
                            )

                            lineups.forEachIndexed { index, lineup ->
                                CoachAndSubsCard(
                                    lineup = lineup,
                                    teamIndex = index,
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                    color = ExtraCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

// ======================================================
// LAPANGAN BERSAMA
// ======================================================

@Composable
private fun SharedFootballPitch(
    lineups: List<FixtureLineup>
) {
    val blue = Color(0xFF168BFF)
    val red = Color(0xFFEF4444)
    val yellow = Color(0xFFFFD21C)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(
                Color(0xFF237B32),
                RoundedCornerShape(14.dp)
            )
    ) {
        val pitchWidth = maxWidth
        val pitchHeight = maxHeight

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val w = size.width
            val h = size.height
            val lineColor = Color.White.copy(alpha = 0.85f)
            val stroke = 2.dp.toPx()

            for (i in 0 until 8) {
                drawRect(
                    color = if (i % 2 == 0) {
                        Color(0xFF287F35)
                    } else {
                        Color(0xFF348C3B)
                    },
                    topLeft = Offset(0f, h * i / 8f),
                    size = Size(w, h / 8f)
                )
            }

            drawRect(
                color = lineColor,
                topLeft = Offset(5f, 5f),
                size = Size(w - 10f, h - 10f),
                style = Stroke(stroke)
            )

            drawLine(
                color = lineColor,
                start = Offset(w / 2f, 5f),
                end = Offset(w / 2f, h - 5f),
                strokeWidth = stroke
            )

            drawCircle(
                color = lineColor,
                radius = h * 0.12f,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(stroke)
            )

            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(w / 2f, h / 2f)
            )

            // Kotak penalti kiri
            drawRect(
                color = lineColor,
                topLeft = Offset(5f, h * 0.34f),
                size = Size(w * 0.13f, h * 0.32f),
                style = Stroke(stroke)
            )

            drawRect(
                color = lineColor,
                topLeft = Offset(5f, h * 0.42f),
                size = Size(w * 0.055f, h * 0.16f),
                style = Stroke(stroke)
            )

            // Kotak penalti kanan
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.87f, h * 0.34f),
                size = Size(w * 0.13f - 5f, h * 0.32f),
                style = Stroke(stroke)
            )

            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.945f, h * 0.42f),
                size = Size(w * 0.055f - 5f, h * 0.16f),
                style = Stroke(stroke)
            )
        }

        lineups.forEachIndexed { teamIndex, lineup ->
            val positions = createPitchPositions(
                lineup = lineup,
                teamIndex = teamIndex
            )

            val teamColor = if (teamIndex == 0) blue else red

            positions.forEach { position ->
                val player = position.player

                val isGoalkeeper =
                    player.pos?.uppercase() == "G" ||
                    player.pos?.uppercase() == "GK"

                val playerColor = if (isGoalkeeper) {
                    yellow
                } else {
                    teamColor
                }

                Box(
                    modifier = Modifier
                        .offset(
                            x = pitchWidth * position.x - 30.dp,
                            y = pitchHeight * position.y - 22.dp
                        )
                        .width(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Color.White,
                                    CircleShape
                                )
                                .padding(2.dp)
                                .background(
                                    playerColor,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                player.number?.toString() ?: "-",
                                color = if (isGoalkeeper) {
                                    Color.Black
                                } else {
                                    Color.White
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            player.name ?: "Pemain",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ======================================================
// PELATIH DAN PEMAIN CADANGAN
// ======================================================

@Composable
private fun CoachAndSubsCard(
    lineup: FixtureLineup,
    teamIndex: Int,
    modifier: Modifier = Modifier
) {
    val teamColor = if (teamIndex == 0) {
        Color(0xFF35B9FF)
    } else {
        Color(0xFFFF5555)
    }

    val substitutes = lineup.substitutes.orEmpty()
        .mapNotNull { it.player }

    Column(
        modifier = modifier
            .background(
                ExtraPanel,
                RoundedCornerShape(12.dp)
            )
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(
            "PELATIH KEPALA",
            color = teamColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            lineup.coach?.name ?: "Belum tersedia",
            color = ExtraWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider(
            color = ExtraBorder
        )

        Text(
            "PEMAIN CADANGAN",
            color = ExtraWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        if (substitutes.isEmpty()) {
            Text(
                "Belum tersedia",
                color = ExtraMuted,
                fontSize = 10.sp
            )
        } else {
            substitutes.forEach { player ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        player.number?.toString() ?: "-",
                        color = teamColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.width(23.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            player.name ?: "Pemain",
                            color = ExtraWhite,
                            fontSize = 9.sp,
                            maxLines = 2
                        )

                        Text(
                            player.pos ?: "-",
                            color = ExtraMuted,
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}
