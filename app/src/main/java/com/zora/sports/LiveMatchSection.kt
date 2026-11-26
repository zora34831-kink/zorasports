package com.zora.sports

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import java.io.IOException

@Composable
fun LiveMatchSection(showHeader: Boolean = true) {
    var matches by remember { mutableStateOf<List<FixtureItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var liveOnly by remember { mutableStateOf(false) }
    var refreshVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(liveOnly, refreshVersion) {
        loading = true
        error = null
        try {
            matches = withTimeout(20000L) {
                if (liveOnly) FootballRepository.getLiveMatches()
                else FootballRepository.getTodayMatches()
            }
        } catch (e: TimeoutCancellationException) {
            error = "Koneksi terlalu lama. Periksa internet lalu coba lagi."
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            error = when (e.code()) {
                401, 403 -> "Akses data pertandingan ditolak. Periksa konfigurasi API."
                429 -> "Batas permintaan data tercapai. Coba beberapa saat lagi."
                in 500..599 -> "Server data pertandingan sedang bermasalah."
                else -> "Permintaan gagal (${e.code()}). Coba muat ulang."
            }
        } catch (e: IOException) {
            error = "Tidak dapat terhubung ke internet."
        } catch (e: Exception) {
            error = e.message?.takeIf { it.isNotBlank() }
                ?: "Gagal memuat pertandingan. Coba lagi."
        } finally {
            loading = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("MATCH CENTER", color = ZoraThemeColors.accent, fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("Pertandingan", color = ZoraThemeColors.text, fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = { refreshVersion++ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Muat ulang",
                        tint = ZoraThemeColors.muted)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(
                    ZoraThemeColors.panelLight.copy(alpha = 0.88f),
                    ZoraThemeColors.panel.copy(alpha = 0.96f)
                )))
                .border(1.dp, ZoraThemeColors.border.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            MatchFilter("Hari ini", !liveOnly, Modifier.weight(1f)) { liveOnly = false }
            MatchFilter("Sedang berlangsung", liveOnly, Modifier.weight(1f)) { liveOnly = true }
            IconButton(onClick = { refreshVersion++ }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang pertandingan",
                    tint = ZoraThemeColors.muted, modifier = Modifier.size(18.dp))
            }
        }

        when {
            loading -> {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(ZoraThemeColors.panel.copy(alpha = 0.82f))
                        .border(1.dp, ZoraThemeColors.border, RoundedCornerShape(20.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = ZoraThemeColors.accent,
                        strokeWidth = 2.dp, modifier = Modifier.size(25.dp))
                    Text("Mengambil data pertandingan…", color = ZoraThemeColors.muted, fontSize = 11.sp)
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(ZoraThemeColors.panel.copy(alpha = 0.88f))
                        .border(1.dp, ZoraThemeColors.border, RoundedCornerShape(20.dp))
                        .padding(17.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("DATA BELUM TERSEDIA", color = ZoraThemeColors.text,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
                    Text(error.orEmpty(), color = ZoraThemeColors.muted,
                        fontSize = 11.sp, lineHeight = 16.sp)
                    TextButton(onClick = { refreshVersion++ }) {
                        Text("COBA LAGI", color = ZoraThemeColors.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
            matches.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(ZoraThemeColors.panel.copy(alpha = 0.82f))
                        .border(1.dp, ZoraThemeColors.border, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null,
                        tint = ZoraThemeColors.muted, modifier = Modifier.size(25.dp))
                    Text(if (liveOnly) "Belum ada pertandingan live" else "Belum ada jadwal hari ini",
                        color = ZoraThemeColors.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Coba periksa kembali nanti.", color = ZoraThemeColors.muted, fontSize = 10.sp)
                }
            }
            else -> matches.forEach { match -> ApiMatchCard(match) }
        }
    }
}

@Composable
private fun MatchFilter(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) ZoraThemeColors.panel else Color.Transparent,
        border = if (selected) androidx.compose.foundation.BorderStroke(
            1.dp, ZoraThemeColors.border.copy(alpha = 0.85f)
        ) else null
    ) {
        Box(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) ZoraThemeColors.text else ZoraThemeColors.muted,
                fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1)
        }
    }
}

@Composable
fun ApiMatchCard(match: FixtureItem) {
    val fixture = match.fixture ?: return
    val league = match.league ?: return
    val teams = match.teams ?: return
    val homeTeam = teams.home
    val awayTeam = teams.away
    val goals = match.goals
    val status = fixture.status?.short ?: "NS"
    val elapsed = fixture.status?.elapsed
    var showLineups by remember(fixture.id) { mutableStateOf(false) }

    val isLive = status in listOf("1H", "HT", "2H", "ET", "BT", "P", "LIVE")
    val statusLabel = when (status) {
        "NS" -> "Belum dimulai"
        "FT", "AET", "PEN" -> "Selesai"
        "HT" -> "Jeda"
        "1H" -> "Babak pertama"
        "2H" -> "Babak kedua"
        "ET" -> "Perpanjangan waktu"
        "BT" -> "Jeda perpanjangan"
        "P" -> "Adu penalti"
        "PST" -> "Ditunda"
        "CANC" -> "Dibatalkan"
        "SUSP", "ABD" -> "Dihentikan"
        "INT" -> "Terhenti sementara"
        "TBD" -> "Waktu belum ditentukan"
        else -> status
    }

    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(
                ZoraThemeColors.panelLight.copy(alpha = 0.84f),
                ZoraThemeColors.panel.copy(alpha = 0.96f),
                ZoraThemeColors.panel.copy(alpha = 0.98f)
            )))
            .border(1.dp, Brush.linearGradient(listOf(
                Color.White.copy(alpha = if (ZoraThemeColors.isLight) 0.9f else 0.12f),
                ZoraThemeColors.border.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.025f)
            )), RoundedCornerShape(22.dp))
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                AsyncImage(model = league.logo, contentDescription = null,
                    modifier = Modifier.size(17.dp), contentScale = ContentScale.Fit)
                Text(league.name ?: "Kompetisi", color = ZoraThemeColors.muted,
                    fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Surface(
                color = if (isLive) Color(0xFF8B4148).copy(alpha = 0.18f)
                else ZoraThemeColors.panelLight.copy(alpha = 0.75f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (isLive) Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFC8797D)))
                    Text(if (isLive) "LIVE" else statusLabel.uppercase(),
                        color = if (isLive) Color(0xFFC8797D) else ZoraThemeColors.muted,
                        fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
                }
            }
        }

        Spacer(Modifier.height(17.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                TeamBadge(homeTeam?.logo, homeTeam?.name ?: "Kandang")
                Text(homeTeam?.name ?: "Kandang", color = ZoraThemeColors.text,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 13.sp)
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${goals?.home ?: "–"}  :  ${goals?.away ?: "–"}",
                    color = ZoraThemeColors.text, fontSize = 22.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                Text(when {
                    isLive && elapsed != null -> "$elapsed′"
                    isLive -> "BERLANGSUNG"
                    else -> statusLabel.uppercase()
                }, color = if (isLive) Color(0xFFC8797D) else ZoraThemeColors.muted,
                    fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)) {
                TeamBadge(awayTeam?.logo, awayTeam?.name ?: "Tandang")
                Text(awayTeam?.name ?: "Tandang", color = ZoraThemeColors.text,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 13.sp)
            }
        }

        Spacer(Modifier.height(13.dp))
        HorizontalDivider(color = ZoraThemeColors.border.copy(alpha = 0.62f))
        TextButton(onClick = { showLineups = true }, enabled = fixture.id != null,
            modifier = Modifier.align(Alignment.End)) {
            Text("SUSUNAN PEMAIN", color = ZoraThemeColors.accent,
                fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
    }

    if (showLineups && fixture.id != null) {
        LineupDialog(fixtureId = fixture.id, onDismiss = { showLineups = false })
    }
}

@Composable
private fun TeamBadge(logo: String?, teamName: String) {
    Box(
        modifier = Modifier.size(43.dp).clip(RoundedCornerShape(15.dp))
            .background(ZoraThemeColors.panelLight.copy(alpha = 0.75f))
            .border(1.dp, ZoraThemeColors.border.copy(alpha = 0.7f), RoundedCornerShape(15.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!logo.isNullOrBlank()) {
            AsyncImage(model = logo, contentDescription = "$teamName logo",
                modifier = Modifier.size(31.dp), contentScale = ContentScale.Fit)
        } else {
            Icon(Icons.Default.SportsSoccer, contentDescription = null,
                tint = ZoraThemeColors.muted, modifier = Modifier.size(22.dp))
        }
    }
}
