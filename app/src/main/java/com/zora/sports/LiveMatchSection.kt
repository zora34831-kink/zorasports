
package com.zora.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException

@Composable
fun LiveMatchSection() {

    var matches by remember {
        mutableStateOf<List<FixtureItem>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var liveOnly by remember {
        mutableStateOf(false)
    }

    var refreshVersion by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(liveOnly, refreshVersion) {
        loading = true
        error = null

        try {
            val result = withTimeout(20000L) {
                if (liveOnly) {
                    FootballRepository.getLiveMatches()
                } else {
                    FootballRepository.getTodayMatches()
                }
            }

            matches = result

        } catch (e: TimeoutCancellationException) {
            error = "Koneksi terlalu lama. Coba muat ulang."

        } catch (e: CancellationException) {
            throw e

        } catch (e: HttpException) {
            error = "Server API bermasalah (${e.code()})."

        } catch (e: IOException) {
            error = "Tidak dapat terhubung ke internet."

        } catch (e: Exception) {
            error = "Gagal memuat pertandingan. Coba lagi."

        } finally {
            loading = false
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PERTANDINGAN",
                color = ZoraThemeColors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = {
                    refreshVersion++
                }
            ) {
                Text(
                    "Muat ulang",
                    color = ZoraThemeColors.accent
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !liveOnly,
                onClick = {
                    liveOnly = false
                },
                label = {
                    Text("Hari ini")
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ZoraThemeColors.accent.copy(alpha = 0.16f),
                    selectedLabelColor = ZoraThemeColors.accent,
                    labelColor = ZoraThemeColors.text
                )
            )

            FilterChip(
                selected = liveOnly,
                onClick = {
                    liveOnly = true
                },
                label = {
                    Text("Sedang berlangsung")
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ZoraThemeColors.accent.copy(alpha = 0.16f),
                    selectedLabelColor = ZoraThemeColors.accent,
                    labelColor = ZoraThemeColors.text
                )
            )
        }

        when {

            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ZoraThemeColors.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            ZoraThemeColors.panel,
                            RoundedCornerShape(15.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        error ?: "Terjadi kesalahan",
                        color = Color(0xFFFF8A80),
                        fontSize = 12.sp
                    )

                    TextButton(
                        onClick = {
                            refreshVersion++
                        }
                    ) {
                        Text(
                            "Coba lagi",
                            color = ZoraThemeColors.accent
                        )
                    }
                }
            }

            matches.isEmpty() -> {
                Text(
                    "Belum ada pertandingan untuk ditampilkan.",
                    color = ZoraThemeColors.muted,
                    fontSize = 12.sp
                )
            }

            else -> {
                matches.forEach { match ->
                    ApiMatchCard(match)
                }
            }
        }
    }
}

@Composable
fun ApiMatchCard(
    match: FixtureItem
) {

    val fixture = match.fixture ?: return
    val league = match.league ?: return
    val teams = match.teams ?: return

    val home = teams.home?.name ?: "Kandang"
    val away = teams.away?.name ?: "Tandang"
    val goals = match.goals

    val status = fixture.status?.short ?: "NS"
    val elapsed = fixture.status?.elapsed

    var showLineups by remember(fixture.id) {
        mutableStateOf(false)
    }

    val isLive = status in listOf(
        "1H", "HT", "2H", "ET", "BT", "P", "LIVE"
    )

    val statusLabel = when (status) {
        "NS" -> "Belum dimulai"
        "FT" -> "Selesai"
        "HT" -> "Jeda"
        "1H" -> "Babak pertama"
        "2H" -> "Babak kedua"
        "ET" -> "Perpanjangan waktu"
        "BT" -> "Jeda perpanjangan"
        "P" -> "Adu penalti"
        "PST" -> "Ditunda"
        "CANC" -> "Dibatalkan"
        "SUSP" -> "Dihentikan"
        "ABD" -> "Dihentikan"
        else -> status
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                ZoraThemeColors.panel,
                RoundedCornerShape(17.dp)
            )
            .padding(15.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                league.name ?: "Kompetisi",
                color = ZoraThemeColors.muted,
                fontSize = 10.sp
            )

            Text(
                if (isLive) "LANGSUNG" else statusLabel,
                color = if (isLive) Color(0xFFE45C65)
                else ZoraThemeColors.muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(13.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    home,
                    color = ZoraThemeColors.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    away,
                    color = ZoraThemeColors.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "${goals?.home ?: "-"} : ${goals?.away ?: "-"}",
                    color = ZoraThemeColors.accent,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    elapsed?.let { "$it menit" } ?: statusLabel,
                    color = ZoraThemeColors.muted,
                    fontSize = 10.sp
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            color = ZoraThemeColors.border.copy(alpha = 0.7f)
        )

        TextButton(
            onClick = {
                showLineups = true
            },
            enabled = fixture.id != null
        ) {
            Text(
                "LIHAT SUSUNAN PEMAIN",
                color = ZoraThemeColors.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showLineups && fixture.id != null) {
        LineupDialog(
            fixtureId = fixture.id,
            onDismiss = {
                showLineups = false
            }
        )
    }
}
