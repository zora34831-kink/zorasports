
package com.zora.sports

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF071321)
private val Panel = Color(0xFF102337)
private val PanelLight = Color(0xFF18334A)
private val Cyan = Color(0xFF42DDF5)
private val Muted = Color(0xFF91A5B8)
private val White = Color(0xFFF4FAFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(7, 19, 33)
        window.navigationBarColor = android.graphics.Color.rgb(7, 19, 33)

        setContent {
            ZoraSportsApp()
        }
    }
}

data class Match(
    val league: String,
    val home: String,
    val away: String,
    val time: String,
    val status: String = "UPCOMING",
    val homeScore: String = "-",
    val awayScore: String = "-"
)

@Composable
fun ZoraSportsApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cyan,
            background = Navy,
            surface = Panel,
            onBackground = White,
            onSurface = White
        )
    ) {
        Scaffold(
            containerColor = Navy,
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0B1B2B),
                    contentColor = White
                ) {
                    val tabs = listOf(
                        Triple("Home", Icons.Default.Home, 0),
                        Triple("Scores", Icons.Default.SportsSoccer, 1),
                        Triple("Jadwal", Icons.Default.CalendarMonth, 2),
                        Triple("Favorit", Icons.Default.Star, 3)
                    )

                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab.third,
                            onClick = { selectedTab = tab.third },
                            icon = {
                                Icon(tab.second, contentDescription = tab.first)
                            },
                            label = {
                                Text(tab.first, fontSize = 10.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Cyan,
                                selectedTextColor = Cyan,
                                indicatorColor = PanelLight,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(
                    Modifier.padding(padding),
                    onNavigate = { selectedTab = it }
                )
                1 -> SimpleScreen(
                    "Live Scores",
                    "Skor pertandingan akan ditampilkan setelah API terhubung.",
                    Icons.Default.SportsSoccer,
                    Modifier.padding(padding)
                )
                2 -> SimpleScreen(
                    "Jadwal Pertandingan",
                    "Jadwal sepak bola akan ditampilkan setelah API terhubung.",
                    Icons.Default.CalendarMonth,
                    Modifier.padding(padding)
                )
                3 -> SimpleScreen(
                    "Tim Favorit",
                    "Tim favorit yang kamu pilih akan muncul di sini.",
                    Icons.Default.Star,
                    Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigate: (Int) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Navy),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 18.dp,
            bottom = 25.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Header()
        }

        item {
            WelcomeSection()
        }

        item {
            QuickMenu(onNavigate)
        }

        item {
            FeaturedMatch()
        }

        item {
            SectionHeader(
                "Pertandingan",
                "Lihat jadwal",
                { onNavigate(2) }
            )
        }

        item {
            DateSelector()
        }

        items(
            listOf(
                Match(
                    "PREVIEW LIGA",
                    "Home FC",
                    "Away FC",
                    "20:00"
                ),
                Match(
                    "PREVIEW CUP",
                    "United FC",
                    "City FC",
                    "22:00"
                ),
                Match(
                    "PREVIEW LEAGUE",
                    "Royal FC",
                    "Athletic FC",
                    "23:30"
                )
            )
        ) { match ->
            MatchCard(match)
        }

        item {
            SectionHeader(
                "Jelajahi",
                "Semua statistik",
                { onNavigate(1) }
            )
        }

        item {
            ExploreCards()
        }

        item {
            Text(
                "ZORASPORTS  •  FOOTBALL, ALL IN ONE",
                color = Muted,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Cyan, Color(0xFF1679A5))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Z",
                    color = Navy,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.width(11.dp))

            Column {
                Text(
                    "ZORA",
                    color = White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    "SPORTS",
                    color = Cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            HeaderButton(Icons.Default.Search)
            HeaderButton(Icons.Default.NotificationsNone)
        }
    }
}

@Composable
fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun WelcomeSection() {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            "THE WORLD OF FOOTBALL",
            color = Cyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Text(
            "Football starts here.",
            color = White,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Semua informasi sepak bola dalam satu aplikasi.",
            color = Muted,
            fontSize = 12.sp
        )
    }
}

@Composable
fun QuickMenu(onNavigate: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        QuickItem("Live", Icons.Default.Sensors, true) {
            onNavigate(1)
        }
        QuickItem("Jadwal", Icons.Default.CalendarMonth) {
            onNavigate(2)
        }
        QuickItem("Liga", Icons.Default.EmojiEvents) {
            onNavigate(1)
        }
        QuickItem("Favorit", Icons.Default.Star) {
            onNavigate(3)
        }
    }
}

@Composable
fun QuickItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(17.dp))
            .background(if (active) PanelLight else Panel)
            .border(
                1.dp,
                if (active) Cyan.copy(alpha = 0.35f)
                else Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(17.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = if (active) Cyan else Muted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            color = White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FeaturedMatch() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF153C55),
                        Color(0xFF102438),
                        Color(0xFF0C1C2B)
                    )
                )
            )
            .border(
                1.dp,
                Cyan.copy(alpha = 0.18f),
                RoundedCornerShape(24.dp)
            )
            .padding(19.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "MATCH CENTER",
                color = Cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Icon(
                Icons.Default.ArrowOutward,
                contentDescription = null,
                tint = Cyan,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.height(19.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamPlaceholder("HOME", "A")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "VS",
                    color = White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "MATCH PREVIEW",
                    color = Muted,
                    fontSize = 8.sp,
                    letterSpacing = 1.sp
                )
            }
            TeamPlaceholder("AWAY", "B")
        }

        Spacer(Modifier.height(19.dp))

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

        Spacer(Modifier.height(13.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Featured match", color = Muted, fontSize = 11.sp)
            Text("Coming soon", color = Cyan, fontSize = 11.sp)
        }
    }
}

@Composable
fun TeamPlaceholder(name: String, letter: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(55.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.09f))
                .border(1.dp, Cyan.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letter,
                color = Cyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(name, color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(
    title: String,
    action: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            color = White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(action, color = Cyan, fontSize = 10.sp)
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Cyan,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
fun DateSelector() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("TODAY", "TOMORROW", "ALL").forEachIndexed { index, date ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (index == 0) PanelLight else Panel)
                    .border(
                        1.dp,
                        if (index == 0) Cyan.copy(alpha = 0.4f)
                        else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    date,
                    color = if (index == 0) Cyan else Muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun MatchCard(match: Match) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(18.dp)
            )
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                match.league,
                color = Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                match.status,
                color = Cyan,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(match.home, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(11.dp))
                Text(match.away, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    match.time,
                    color = Cyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Waktu contoh",
                    color = Muted,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
fun ExploreCards() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ExploreCard(
            "STATISTIK",
            "Analisis pertandingan",
            Icons.Default.BarChart,
            Modifier.weight(1f)
        )
        ExploreCard(
            "KLASEMEN",
            "Posisi setiap tim",
            Icons.Default.EmojiEvents,
            Modifier.weight(1f)
        )
    }
}

@Composable
fun ExploreCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Cyan, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(15.dp))
        Text(title, color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Muted, fontSize = 9.sp)
    }
}

@Composable
fun SimpleScreen(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Navy)
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Cyan,
            modifier = Modifier.size(55.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            title,
            color = White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(9.dp))
        Text(
            description,
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 19.sp
        )
    }
}
