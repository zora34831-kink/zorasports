
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

// Masukkan API key BARU milik lu di sini.
// Jangan gunakan key lama yang sudah terlanjur terekspos.
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

    LaunchedEffect(selectedLeague, refresh) {
        loading = true
        error = null
        standings = emptyList()

        try {
            val result = withTimeout(25000L) {
                val apiNinjasId = selectedLeague.apiNinjasId

                if (apiNinjasId != null) {
                    val response = fetchNinjasStandings(apiNinjasId)

                    seasonLabel = response.season.ifBlank {
                        "MUSIM BERJALAN"
                    }

                    response.teams
                } else {
                    val footballId = selectedLeague.footballId
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
                    selectedLeague.name,
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
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
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
                            modifier = Modifier.weight(1f),
                            maxLines = 1
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
// POSISI PEMAIN DI LAPANGAN VERTIKAL
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
                "G", "GK" -> 1
                "D" -> 2
                "M" -> 3
                "F" -> 4
                else -> 3
            }
    }

    val maxRow = maxOf(rows.maxOrNull() ?: 4, 4)

    return players.indices.map { index ->
        val row = rows[index]

        val sameRow = players.indices
            .filter { rows[it] == row }
            .sortedBy { parsed[it]?.get(1) ?: it }

        val positionInRow = sameRow.indexOf(index)
        val countInRow = sameRow.size

        // Posisi horizontal dalam satu baris.
        val x = (positionInRow + 1).toFloat() /
            (countInRow + 1).toFloat()

        // Progres dari gawang sendiri menuju tengah lapangan.
        val progress = (row - 1).toFloat() /
            (maxRow - 1).coerceAtLeast(1)

        // Tim pertama di atas, tim kedua di bawah.
        val y = if (teamIndex == 0) {
            0.06f + progress * 0.40f
        } else {
            0.94f - progress * 0.40f
        }

        PitchPlayerPosition(
            player = players[index],
            x = x.coerceIn(0.08f, 0.92f),
            y = y.coerceIn(0.05f, 0.95f)
        )
    }
}

// ======================================================
// LINEUP FULL SCREEN
// ======================================================

@Composable
fun LineupDialog(
    fixtureId: Int,
    onDismiss: () -> Unit
) {
    var lineups by remember(fixtureId) {
        mutableStateOf<List<FixtureLineup>>(emptyList())
    }

    var loading by remember(fixtureId) {
        mutableStateOf(true)
    }

    var error by remember(fixtureId) {
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ExtraNavy
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = 14.dp,
                        vertical = 18.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "SUSUNAN PEMAIN",
                            color = ExtraWhite,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Formasi dan posisi pemain kedua tim",
                            color = ExtraMuted,
                            fontSize = 11.sp
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text(
                            "TUTUP",
                            color = ExtraCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                when {
                    loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp),
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
                                    "Mengambil susunan pemain...",
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
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                error ?: "Data tidak tersedia",
                                color = ExtraMuted,
                                textAlign = TextAlign.Center
                            )

                            TextButton(onClick = onDismiss) {
                                Text(
                                    "TUTUP",
                                    color = ExtraCyan
                                )
                            }
                        }
                    }

                    else -> {
                        val first = lineups.getOrNull(0)
                        val second = lineups.getOrNull(1)

                        MatchLineupHeader(
                            first = first,
                            second = second
                        )

                        SharedFootballPitch(
                            lineups = lineups.take(2)
                        )

                        if (lineups.size >= 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                CoachAndSubsCard(
                                    lineup = lineups[0],
                                    teamIndex = 0,
                                    modifier = Modifier.weight(1f)
                                )

                                CoachAndSubsCard(
                                    lineup = lineups[1],
                                    teamIndex = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Text(
                                "Data susunan pemain tim kedua belum tersedia.",
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

                        Text(
                            "Data susunan pemain disediakan oleh penyedia data pertandingan.",
                            color = ExtraMuted,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ExtraCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                ExtraBorder
                            )
                        ) {
                            Text("TUTUP SUSUNAN PEMAIN")
                        }
                    }
                }
            }
        }
    }
}

// ======================================================
// HEADER KEDUA TIM
// ======================================================

@Composable
private fun MatchLineupHeader(
    first: FixtureLineup?,
    second: FixtureLineup?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                ExtraPanel,
                RoundedCornerShape(14.dp)
            )
            .padding(13.dp),
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
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                first?.formation ?: "-",
                color = Color(0xFF35B9FF),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Text(
            "VS",
            color = ExtraMuted,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
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
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                second?.formation ?: "-",
                color = Color(0xFFFF5555),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

// ======================================================
// LAPANGAN VERTIKAL DUA TIM
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
            .height(560.dp)
            .background(
                Color(0xFF237B32),
                RoundedCornerShape(13.dp)
            )
    ) {
        val pitchWidth = maxWidth
        val pitchHeight = maxHeight

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val w = size.width
            val h = size.height
            val lineColor = Color.White.copy(alpha = 0.88f)
            val stroke = 2.dp.toPx()

            // Garis warna rumput.
            for (i in 0 until 10) {
                drawRect(
                    color = if (i % 2 == 0) {
                        Color(0xFF287F35)
                    } else {
                        Color(0xFF348C3B)
                    },
                    topLeft = Offset(0f, h * i / 10f),
                    size = Size(w, h / 10f)
                )
            }

            // Garis luar lapangan.
            drawRect(
                color = lineColor,
                topLeft = Offset(4f, 4f),
                size = Size(w - 8f, h - 8f),
                style = Stroke(stroke)
            )

            // Garis tengah horizontal.
            drawLine(
                color = lineColor,
                start = Offset(4f, h / 2f),
                end = Offset(w - 4f, h / 2f),
                strokeWidth = stroke
            )

            // Lingkaran tengah.
            drawCircle(
                color = lineColor,
                radius = w * 0.14f,
                center = Offset(w / 2f, h / 2f),
                style = Stroke(stroke)
            )

            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(w / 2f, h / 2f)
            )

            // Kotak penalti atas.
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.19f, 4f),
                size = Size(w * 0.62f, h * 0.14f),
                style = Stroke(stroke)
            )

            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.34f, 4f),
                size = Size(w * 0.32f, h * 0.07f),
                style = Stroke(stroke)
            )

            // Kotak penalti bawah.
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.19f, h * 0.86f),
                size = Size(w * 0.62f, h * 0.14f - 4f),
                style = Stroke(stroke)
            )

            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.34f, h * 0.93f),
                size = Size(w * 0.32f, h * 0.07f - 4f),
                style = Stroke(stroke)
            )

            // Titik penalti atas dan bawah.
            drawCircle(
                color = lineColor,
                radius = 2.5.dp.toPx(),
                center = Offset(w / 2f, h * 0.105f)
            )

            drawCircle(
                color = lineColor,
                radius = 2.5.dp.toPx(),
                center = Offset(w / 2f, h * 0.895f)
            )
        }

        // Pemain tim pertama di atas dan tim kedua di bawah.
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
                            x = pitchWidth * position.x - 36.dp,
                            y = pitchHeight * position.y - 25.dp
                        )
                        .width(72.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
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
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
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
            .padding(10.dp),
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
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        HorizontalDivider(color = ExtraBorder)

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
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
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
