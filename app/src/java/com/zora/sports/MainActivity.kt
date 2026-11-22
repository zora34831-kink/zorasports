
package com.zora.sports

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Navy = Color(0xFF071321)
private val Panel = Color(0xFF112438)
private val Cyan = Color(0xFF45D9F5)
private val Muted = Color(0xFF8FA6B9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZoraSportsApp()
        }
    }
}

@Composable
fun ZoraSportsApp() {
    var selectedTab by remember { mutableStateOf("Home") }
    var matches by remember { mutableStateOf<List<FixtureItem>>(emptyList()) }
    var favorites by remember { mutableStateOf(setOf<Int>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun loadMatches(live: Boolean) {
        scope.launch {
            loading = true
            error = null

            try {
                if (BuildConfig.FOOTBALL_API_KEY.isBlank()) {
                    error = "API Key belum dipasang. Tambahkan FOOTBALL_API_KEY di konfigurasi build."
                    matches = emptyList()
                } else {
                    matches = if (live) {
                        FootballRepository.live()
                    } else {
                        FootballRepository.today()
                    }
                }
            } catch (e: Exception) {
                error = "Gagal mengambil data. Periksa koneksi dan API Key."
                matches = emptyList()
            }

            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadMatches(false)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Navy,
            surface = Panel,
            primary = Cyan,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Scaffold(
            containerColor = Navy,
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0B1B2B),
                    contentColor = Color.White
                ) {
                    val tabs = listOf(
                        "Home" to Icons.Default.Home,
                        "Live" to Icons.Default.SportsSoccer,
                        "Jadwal" to Icons.Default.CalendarMonth,
                        "Favorit" to Icons.Default.Favorite
                    )

                    tabs.forEach { (name, icon) ->
                        NavigationBarItem(
                            selected = selectedTab == name,
                            onClick = {
                                selectedTab = name
                                if (name == "Live") loadMatches(true)
                                if (name == "Home" || name == "Jadwal") {
                                    loadMatches(false)
                                }
                            },
                            icon = {
                                Icon(icon, contentDescription = name)
                            },
                            label = {
                                Text(name, fontSize = 10.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Cyan,
                                selectedTextColor = Cyan,
                                indicatorColor = Color(0xFF18394D),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Navy)
                    .padding(padding)
                    .padding(horizontal = 18.dp)
            ) {
                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "ZORA",
                            color = Color.White,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "SPORTS",
                            color = Cyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    }

                    Surface(
                        color = Panel,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 13.dp,
                                vertical = 9.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Circle,
                                contentDescription = null,
                                tint = Cyan,
                                modifier = Modifier.size(8.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text("FOOTBALL", fontSize = 10.sp)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    when (selectedTab) {
                        "Live" -> "Pertandingan Live"
                        "Jadwal" -> "Jadwal Hari Ini"
                        "Favorit" -> "Tim Favorit"
                        else -> "Football Center"
                    },
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    "Semua informasi sepak bola dalam satu aplikasi",
                    color = Muted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(18.dp))

                if (selectedTab == "Home" || selectedTab == "Jadwal") {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        FilterChip(
                            selected = true,
                            onClick = { loadMatches(false) },
                            label = { Text("Hari ini") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = { loadMatches(true) },
                            label = { Text("Live sekarang") }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                if (selectedTab == "Favorit") {
                    Text(
                        "Pertandingan yang kamu tandai akan muncul di sini.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }

                when {
                    loading -> {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Cyan)
                        }
                    }

                    error != null -> {
                        EmptyState(
                            title = "Data belum tersedia",
                            description = error ?: "",
                            onRetry = {
                                loadMatches(selectedTab == "Live")
                            }
                        )
                    }

                    else -> {
                        val visibleMatches = if (selectedTab == "Favorit") {
                            matches.filter { favorites.contains(it.fixture.id) }
                        } else {
                            matches
                        }

                        if (visibleMatches.isEmpty()) {
                            EmptyState(
                                title = "Belum ada pertandingan",
                                description = "Coba muat ulang atau periksa jadwal pertandingan.",
                                onRetry = {
                                    loadMatches(selectedTab == "Live")
                                }
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 20.dp)
                            ) {
                                items(
                                    visibleMatches,
                                    key = { it.fixture.id }
                                ) { match ->
                                    MatchCard(
                                        match = match,
                                        isFavorite = favorites.contains(
                                            match.fixture.id
                                        ),
                                        onFavorite = {
                                            favorites = if (
                                                favorites.contains(match.fixture.id)
                                            ) {
                                                favorites - match.fixture.id
                                            } else {
                                                favorites + match.fixture.id
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: FixtureItem,
    isFavorite: Boolean,
    onFavorite: () -> Unit
) {
    val status = match.fixture.status.short
    val isLive = status in listOf(
        "1H", "2H", "HT", "ET", "P", "LIVE"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Panel,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    match.league.name,
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )

                if (isLive) {
                    Text(
                        "LIVE",
                        color = Cyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        status,
                        color = Muted,
                        fontSize = 10.sp
                    )
                }

                IconButton(
                    onClick = onFavorite,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite
                        else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorit",
                        tint = if (isFavorite) Cyan else Muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        match.teams.home.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${match.goals.home ?: 0}  -  ${match.goals.away ?: 0}",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black
                    )
                    match.fixture.status.elapsed?.let {
                        Text(
                            "$it'",
                            color = Cyan,
                            fontSize = 10.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        match.teams.away.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 55.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.SportsSoccer,
            contentDescription = null,
            tint = Cyan,
            modifier = Modifier.size(42.dp)
        )

        Spacer(Modifier.height(15.dp))

        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Spacer(Modifier.height(7.dp))

        Text(
            description,
            color = Muted,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(15.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Cyan,
                contentColor = Navy
            )
        ) {
            Text("Coba lagi")
        }
    }
}
