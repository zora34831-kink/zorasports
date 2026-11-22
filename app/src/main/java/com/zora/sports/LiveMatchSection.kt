
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
import kotlinx.coroutines.launch

@Composable
fun LiveMatchSection() {
    var matches by remember {
        mutableStateOf<List<FixtureItem>>(emptyList())
    }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var liveOnly by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null

            try {
                matches = if (liveOnly) {
                    FootballRepository.getLiveMatches()
                } else {
                    FootballRepository.getTodayMatches()
                }
            } catch (e: Exception) {
                error = e.message ?: "Gagal mengambil data"
            }

            loading = false
        }
    }

    LaunchedEffect(liveOnly) {
        refresh()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PERTANDINGAN",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(onClick = { refresh() }) {
                Text("Refresh", color = Color(0xFF42DDF5))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !liveOnly,
                onClick = { liveOnly = false },
                label = { Text("Hari ini") }
            )

            FilterChip(
                selected = liveOnly,
                onClick = { liveOnly = true },
                label = { Text("Live") }
            )
        }

        when {
            loading -> CircularProgressIndicator(
                color = Color(0xFF42DDF5),
                modifier = Modifier.size(28.dp)
            )

            error != null -> Text(
                "Gagal memuat: $error",
                color = Color(0xFFFF8A80),
                fontSize = 12.sp
            )

            matches.isEmpty() -> Text(
                "Belum ada pertandingan.",
                color = Color.LightGray,
                fontSize = 12.sp
            )

            else -> matches.forEach { match ->
                ApiMatchCard(match)
            }
        }
    }
}

@Composable
fun ApiMatchCard(match: FixtureItem) {
    val status = match.fixture.status.short

    val isLive = status in listOf(
        "1H", "HT", "2H", "ET", "BT", "P"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF102337),
                RoundedCornerShape(17.dp)
            )
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                match.league.name,
                color = Color(0xFF91A5B8),
                fontSize = 10.sp
            )

            Text(
                if (isLive) "LIVE" else status,
                color = if (isLive) Color(0xFF42DDF5)
                else Color(0xFF91A5B8),
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
            Column {
                Text(
                    match.teams.home.name,
                    color = Color.White,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    match.teams.away.name,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${match.goals.home ?: "-"} : ${match.goals.away ?: "-"}",
                    color = Color(0xFF42DDF5),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    match.fixture.status.elapsed?.let { "$it'" }
                        ?: status,
                    color = Color(0xFF91A5B8),
                    fontSize = 10.sp
                )
            }
        }
    }
}
