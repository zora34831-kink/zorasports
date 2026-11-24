
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
import kotlinx.coroutines.withTimeout

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

private data class LeagueOption(
    val name: String,
    val id: Int
)

@Composable
fun StandingsSection(
    modifier: Modifier = Modifier
) {
    val leagues = listOf(
        LeagueOption("Premier League", 39),
        LeagueOption("La Liga", 140),
        LeagueOption("Serie A", 135),
        LeagueOption("Bundesliga", 78),
        LeagueOption("Ligue 1", 61),
        LeagueOption("Liga 1 Indonesia", 274)
    )

    var selectedLeague by remember {
        mutableStateOf(leagues[0])
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    var standings by remember {
        mutableStateOf<List<StandingTeam>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var refresh by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(selectedLeague, refresh) {
        loading = true
        error = null

        try {
            standings = withTimeout(20000L) {
                FootballRepository.getStandings(
                    leagueId = selectedLeague.id,
                    season = 2026
                )
            }

            if (standings.isEmpty()) {
                error =
                    "API berhasil merespons, tetapi data klasemen kosong untuk liga dan musim ini."
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            error = e.message ?: "Gagal mengambil klasemen."

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
                    selectedLeague.name,
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

        Text(
            "MUSIM 2026",
            color = ExtraCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

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
                        onClick = {
                            refresh++
                        }
                    ) {
                        Text(
                            "Coba lagi",
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
                            item.team?.name ?: "Klub",
                            color = ExtraWhite,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            item.all?.played?.toString() ?: "-",
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
