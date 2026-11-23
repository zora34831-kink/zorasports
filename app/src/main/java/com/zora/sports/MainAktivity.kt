
package com.zora.sports

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private val Navy = Color(0xFF071321)
private val Panel = Color(0xFF102337)
private val PanelLight = Color(0xFF18334A)
private val Cyan = Color(0xFF42DDF5)
private val Muted = Color(0xFF91A5B8)
private val White = Color(0xFFF4FAFF)

class MainActivity : ComponentActivity() {

    var pipMode by mutableStateOf(false)
        private set

    var streamTabActive = false

    fun enterStreamPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            streamTabActive
        ) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()

            enterPictureInPictureMode(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (streamTabActive &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            enterStreamPip()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(
            isInPictureInPictureMode,
            newConfig
        )

        pipMode = isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor =
            android.graphics.Color.rgb(7, 19, 33)

        window.navigationBarColor =
            android.graphics.Color.rgb(7, 19, 33)

        setContent {
            ZoraSportsApp(
                isInPipMode = pipMode,
                onEnterPip = {
                    enterStreamPip()
                },
                onStreamTabChanged = {
                    streamTabActive = it
                }
            )
        }
    }
}

data class AppTab(
    val title: String,
    val icon: ImageVector
)

@Composable
fun ZoraSportsApp(
    isInPipMode: Boolean,
    onEnterPip: () -> Unit,
    onStreamTabChanged: (Boolean) -> Unit
) {
    val activity = LocalContext.current as ComponentActivity

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    var fullscreen by remember {
        mutableStateOf(false)
    }

    SideEffect {
        onStreamTabChanged(selectedTab == 2)
    }

    fun toggleFullscreen() {
        fullscreen = !fullscreen

        activity.requestedOrientation =
            if (fullscreen) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

        WindowCompat.setDecorFitsSystemWindows(
            activity.window,
            !fullscreen
        )

        val controller = WindowInsetsControllerCompat(
            activity.window,
            activity.window.decorView
        )

        if (fullscreen) {
            controller.hide(
                WindowInsetsCompat.Type.systemBars()
            )
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(
                WindowInsetsCompat.Type.systemBars()
            )
        }
    }

    BackHandler(enabled = fullscreen) {
        toggleFullscreen()
    }

    DisposableEffect(activity) {
        onDispose {
            activity.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            WindowCompat.setDecorFitsSystemWindows(
                activity.window,
                true
            )

            WindowInsetsControllerCompat(
                activity.window,
                activity.window.decorView
            ).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val tabs = listOf(
        AppTab("Home", Icons.Default.Home),
        AppTab("Live", Icons.Default.SportsSoccer),
        AppTab("Stream", Icons.Default.PlayCircle),
        AppTab("Jadwal", Icons.Default.CalendarMonth),
        AppTab("Liga", Icons.Default.EmojiEvents),
        AppTab("Favorit", Icons.Default.Star)
    )

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
                if (!fullscreen && !isInPipMode) {
                    NavigationBar(
                        containerColor = Color(0xFF0B1B2B)
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                },
                                icon = {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        tab.title,
                                        fontSize = 10.sp
                                    )
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
            }
        ) { padding ->

            val pageModifier =
                if (fullscreen || isInPipMode) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.padding(padding)
                }

            when (selectedTab) {
                0 -> HomeScreen(
                    modifier = pageModifier,
                    onNavigate = {
                        selectedTab = it
                    },
                    onFullscreen = {
                        toggleFullscreen()
                    },
                    fullscreen = fullscreen
                )

                1 -> MatchPage(
                    title = "LIVE SCORES",
                    description =
                        "Pantau pertandingan yang sedang berlangsung.",
                    modifier = pageModifier
                )

                2 -> PublicStreamScreen(
                    modifier = pageModifier,
                    pipMode = isInPipMode,
                    onEnterPip = onEnterPip
                )

                3 -> MatchPage(
                    title = "JADWAL PERTANDINGAN",
                    description =
                        "Lihat pertandingan hari ini dan jadwal mendatang.",
                    modifier = pageModifier
                )

                4 -> LeaguePage(
                    modifier = pageModifier
                )

                5 -> FavoritePage(
                    modifier = pageModifier
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigate: (Int) -> Unit,
    onFullscreen: () -> Unit,
    fullscreen: Boolean
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Navy),
        contentPadding = PaddingValues(
            horizontal = 18.dp,
            vertical = 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Header(
                onFullscreen = onFullscreen,
                fullscreen = fullscreen
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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

        item {
            FeaturedMatch(onNavigate)
        }

        item {
            SectionHeader(
                "Pertandingan",
                "Lihat semua"
            ) {
                onNavigate(1)
            }
        }

        item {
            LiveMatchSection()
        }

        item {
            SectionHeader(
                "Jelajahi",
                "Lihat liga"
            ) {
                onNavigate(4)
            }
        }

        item {
            ExploreGrid(onNavigate)
        }

        item {
            Text(
                "ZORASPORTS • FOOTBALL, ALL IN ONE",
                color = Muted,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun Header(
    onFullscreen: () -> Unit,
    fullscreen: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Cyan,
                                Color(0xFF1679A5)
                            )
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.clickable {
                    onFullscreen()
                },
                shape = RoundedCornerShape(14.dp),
                color = Panel
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 11.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (fullscreen) {
                            Icons.Default.FullscreenExit
                        } else {
                            Icons.Default.Fullscreen
                        },
                        contentDescription = "Fullscreen",
                        tint = Cyan
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (fullscreen) "Normal" else "Fullscreen",
                        color = White,
                        fontSize = 10.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Panel
            ) {
                Icon(
                    Icons.Default.NotificationsNone,
                    contentDescription = "Notifikasi",
                    tint = White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun FeaturedMatch(
    onNavigate: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(23.dp))
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
                Cyan.copy(alpha = 0.2f),
                RoundedCornerShape(23.dp)
            )
            .clickable {
                onNavigate(1)
            }
            .padding(19.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
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
                tint = Cyan
            )
        }

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamPlaceholder("HOME", "H")

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "VS",
                    color = White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    "MATCH CENTER",
                    color = Muted,
                    fontSize = 8.sp
                )
            }

            TeamPlaceholder("AWAY", "A")
        }

        Spacer(Modifier.height(20.dp))

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.12f)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Buka pusat pertandingan",
            color = Cyan,
            fontSize = 11.sp
        )
    }
}

@Composable
fun TeamPlaceholder(
    name: String,
    letter: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Color.White.copy(alpha = 0.08f)
                )
                .border(
                    1.dp,
                    Cyan.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letter,
                color = Cyan,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            name,
            color = White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.clickable {
                onClick()
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                action,
                color = Cyan,
                fontSize = 10.sp
            )

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
fun ExploreGrid(
    onNavigate: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExploreCard(
                "KLASEMEN",
                "Posisi setiap tim",
                Icons.Default.EmojiEvents,
                Modifier.weight(1f)
            ) {
                onNavigate(4)
            }

            ExploreCard(
                "STATISTIK",
                "Analisis pertandingan",
                Icons.Default.BarChart,
                Modifier.weight(1f)
            ) {
                onNavigate(1)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExploreCard(
                "HASIL",
                "Pertandingan selesai",
                Icons.Default.Scoreboard,
                Modifier.weight(1f)
            ) {
                onNavigate(1)
            }

            ExploreCard(
                "FAVORIT",
                "Tim pilihanmu",
                Icons.Default.Star,
                Modifier.weight(1f)
            ) {
                onNavigate(5)
            }
        }
    }
}

@Composable
fun ExploreCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(18.dp)
            )
            .clickable {
                onClick()
            }
            .padding(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = Cyan,
            modifier = Modifier.size(23.dp)
        )

        Spacer(Modifier.height(13.dp))

        Text(
            title,
            color = White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            subtitle,
            color = Muted,
            fontSize = 9.sp
        )
    }
}

@Composable
fun MatchPage(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Navy),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                title,
                color = White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text(
                description,
                color = Muted,
                fontSize = 12.sp
            )
        }

        item {
            LiveMatchSection()
        }
    }
}

@Composable
fun LeaguePage(
    modifier: Modifier = Modifier
) {
    StandingsSection(
        modifier = modifier
    )
}

@Composable
fun FavoritePage(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Navy)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = Cyan,
            modifier = Modifier.size(55.dp)
        )

        Spacer(Modifier.height(18.dp))

        Text(
            "TIM FAVORIT",
            color = White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(9.dp))

        Text(
            "Belum ada tim favorit. Fitur penyimpanan tim favorit akan ditambahkan berikutnya.",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun InfoCard(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(Panel)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(17.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            title,
            color = Cyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(7.dp))

        Text(
            description,
            color = Muted,
            fontSize = 11.sp
        )
    }
}
