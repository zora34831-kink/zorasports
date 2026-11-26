
package com.zora.sports

import android.content.pm.ActivityInfo
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private val Navy get() = ZoraThemeColors.background
private val Panel get() = ZoraThemeColors.panel
private val PanelLight get() = ZoraThemeColors.panelLight
private val Cyan get() = ZoraThemeColors.accent
private val Muted get() = ZoraThemeColors.muted
private val White get() = ZoraThemeColors.text

private const val WHATSAPP_CHANNEL =
    "https://whatsapp.com/channel/0029Vb9PO5tEAKWNW4GCQd1b"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZoraSportsApp()
        }
    }
}

data class AppTab(
    val title: String,
    val icon: ImageVector
)

@Composable
fun ZoraSportsApp() {
    val activity = LocalContext.current as ComponentActivity

    // Sinkronisasi jadwal Firebase untuk pengingat lokal.
    NotificationScheduleSync()

    val themePrefs = remember {
        activity.getSharedPreferences(
            "zora_theme_settings",
            android.content.Context.MODE_PRIVATE
        )
    }

    var lightMode by remember {
        mutableStateOf(
            themePrefs.getBoolean("light_mode", false)
        )
    }

    SideEffect {
        ZoraThemeColors.isLight = lightMode

        val barColor = if (lightMode) {
            android.graphics.Color.rgb(244, 247, 250)
        } else {
            android.graphics.Color.rgb(7, 19, 33)
        }

        activity.window.statusBarColor = barColor
        activity.window.navigationBarColor = barColor

        val bars = WindowInsetsControllerCompat(
            activity.window,
            activity.window.decorView
        )

        bars.isAppearanceLightStatusBars = lightMode
        bars.isAppearanceLightNavigationBars = lightMode
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var fullscreen by remember { mutableStateOf(false) }
    var showHamburger by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var extraPage by remember { mutableStateOf("") }

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
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler(
        enabled = showHamburger ||
            showNotifications ||
            extraPage.isNotEmpty() ||
            fullscreen
    ) {
        when {
            showHamburger -> showHamburger = false
            showNotifications -> showNotifications = false
            extraPage.isNotEmpty() -> extraPage = ""
            fullscreen -> toggleFullscreen()
        }
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
        AppTab("Beranda", Icons.Default.Home),
        AppTab("Live", Icons.Default.SportsSoccer),
        AppTab("Siaran", Icons.Default.PlayCircle),
        AppTab("Jadwal", Icons.Default.CalendarMonth),
        AppTab("Liga", Icons.Default.EmojiEvents),
        AppTab("Favorit", Icons.Default.Star)
    )

    MaterialTheme(
        colorScheme = if (lightMode) {
            lightColorScheme(
                primary = Cyan,
                background = Navy,
                surface = Panel,
                onBackground = White,
                onSurface = White
            )
        } else {
            darkColorScheme(
                primary = Cyan,
                background = Navy,
                surface = Panel,
                onBackground = White,
                onSurface = White
            )
        }
    ) {
        Scaffold(
            containerColor = Navy,
            bottomBar = {
                if (
                    !fullscreen &&
                    extraPage.isEmpty() &&
                    !showNotifications
                ) {
                    LiquidGlassNavigationBar(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            }
        ) { padding ->

            val pageModifier =
                if (fullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .padding(padding)
                        .fillMaxSize()
                }

            when {
                extraPage == "DUKUNGAN" -> {
                    SupportPage(
                        modifier = pageModifier,
                        onBack = { extraPage = "" }
                    )
                }

                extraPage == "INFO APK" -> {
                    AppInfoPage(
                        modifier = pageModifier,
                        onBack = { extraPage = "" }
                    )
                }

                showNotifications -> {
                    NotificationsScreen(
                        modifier = pageModifier,
                        onBack = {
                            showNotifications = false
                        }
                    )
                }

                else -> {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            modifier = pageModifier,
                            onNavigate = { selectedTab = it },
                            onFullscreen = { toggleFullscreen() },
                            fullscreen = fullscreen,
                            onMenuClick = {
                                showHamburger = true
                            },
                            onNotificationsClick = {
                                showNotifications = true
                            }
                        )

                        1 -> MatchPage(
                            title = "SKOR LANGSUNG",
                            description =
                                "Pantau pertandingan yang sedang berlangsung.",
                            modifier = pageModifier
                        )

                        2 -> PublicStreamScreen(
                            modifier = pageModifier
                        )

                        3 -> MatchPage(
                            title = "JADWAL PERTANDINGAN",
                            description =
                                "Lihat pertandingan hari ini dan jadwal mendatang.",
                            modifier = pageModifier
                        )

                        4 -> LeaguePage(modifier = pageModifier)
                        5 -> FavoritePage(modifier = pageModifier)
                    }
                }
            }
        }
    }

    if (showHamburger) {
        HamburgerMenu(
            onDismiss = { showHamburger = false },
            onSupport = {
                showHamburger = false
                extraPage = "DUKUNGAN"
            },
            onInfo = {
                showHamburger = false
                extraPage = "INFO APK"
            },
            lightMode = lightMode,
            onThemeChange = {
                lightMode = it
                themePrefs.edit()
                    .putBoolean("light_mode", it)
                    .apply()
            }
        )
    }
}


@Composable
fun LiquidGlassNavigationBar(
    tabs: List<AppTab>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 13.dp, vertical = 9.dp)
            .shadow(
                elevation = 22.dp,
                shape = RoundedCornerShape(27.dp),
                ambientColor = Cyan.copy(alpha = 0.20f),
                spotColor = Cyan.copy(alpha = 0.24f)
            )
            .clip(RoundedCornerShape(27.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xE62A4055),
                        Color(0xE6122235),
                        Color(0xE6293D51)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.34f),
                        Cyan.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(27.dp)
            )
            .padding(horizontal = 5.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.13f else 1f,
                    animationSpec = tween(
                        durationMillis = 360,
                        easing = FastOutSlowInEasing
                    ),
                    label = "nav_icon_scale"
                )
                val tint by animateColorAsState(
                    targetValue = if (selected) Color.White else Color(0xFF9EADBD),
                    animationSpec = tween(280),
                    label = "nav_icon_tint"
                )
                val pillColor by animateColorAsState(
                    targetValue = if (selected) {
                        Cyan.copy(alpha = 0.28f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(320),
                    label = "nav_pill"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(19.dp))
                        .background(pillColor)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = tint,
                        modifier = Modifier
                            .size(21.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                    Text(
                        text = tab.title,
                        color = if (selected) Color.White else Color(0xFF9EADBD),
                        fontSize = 8.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .width(if (selected) 13.dp else 3.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) Cyan else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun HamburgerMenu(
    onDismiss: () -> Unit,
    onSupport: () -> Unit,
    onInfo: () -> Unit,
    lightMode: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = {
            Text(
                "PENGATURAN",
                color = White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "TAMPILAN APLIKASI",
                    color = Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !lightMode,
                        onClick = { onThemeChange(false) },
                        label = { Text("Gelap") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DarkMode,
                                contentDescription = null
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Cyan,
                            selectedLabelColor = Navy,
                            selectedLeadingIconColor = Navy,
                            labelColor = White,
                            iconColor = Muted
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = lightMode,
                        onClick = { onThemeChange(true) },
                        label = { Text("Terang") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LightMode,
                                contentDescription = null
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Cyan,
                            selectedLabelColor = Navy,
                            selectedLeadingIconColor = Navy,
                            labelColor = White,
                            iconColor = Muted
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(
                    color = ZoraThemeColors.border
                )

                MenuOption(
                    title = "DUKUNGAN",
                    subtitle = "Dukung pengembangan ZORASPORTS",
                    icon = Icons.Default.Favorite,
                    onClick = onSupport
                )

                MenuOption(
                    title = "INFO APLIKASI",
                    subtitle = "Informasi aplikasi dan pengembang",
                    icon = Icons.Default.Info,
                    onClick = onInfo
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("TUTUP", color = Cyan)
            }
        }
    )
}

@Composable
fun MenuOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(PanelLight)
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Cyan,
            modifier = Modifier.size(25.dp)
        )

        Spacer(Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                subtitle,
                color = Muted,
                fontSize = 10.sp
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Muted
        )
    }
}

@Composable
fun SupportPage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var showQr by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Navy),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            PageTopBar(
                title = "DUKUNGAN",
                onBack = onBack
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    "Dukung ZORASPORTS",
                    color = White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Dukungan sukarela untuk membantu pengembangan aplikasi.",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Panel)
                    .border(
                        1.dp,
                        Cyan.copy(alpha = 0.16f),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "QRIS DONASI",
                    color = Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Image(
                    painter = painterResource(id = R.drawable.qris),
                    contentDescription = "QRIS dukungan ZORA",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White)
                        .clickable { showQr = true }
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )

                Text(
                    "Tekan QRIS untuk memperbesar",
                    color = Cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Scan QRIS, lalu masukkan nominal pembayaran sendiri di aplikasi pembayaran.",
                    color = Muted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Cyan,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        "Pastikan nama penerima benar sebelum membayar.",
                        color = Muted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(17.dp))
                    .background(Panel)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "CARA BERDONASI",
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                SupportStep("01", "Buka aplikasi pembayaran yang mendukung QRIS.")
                SupportStep("02", "Scan QRIS ZORASPORTS.")
                SupportStep("03", "Masukkan nominal yang ingin diberikan.")
                SupportStep("04", "Periksa penerima dan konfirmasi pembayaran.")
            }
        }

        item {
            Text(
                "DONASI BERSIFAT SUKARELA",
                color = Muted,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showQr) {
        Dialog(onDismissRequest = { showQr = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable { showQr = false }
                    .padding(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.qris),
                    contentDescription = "QRIS ukuran besar",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun SupportStep(
    number: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(PanelLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                color = Cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            description,
            color = Muted,
            fontSize = 11.sp
        )
    }
}

@Composable
fun AppInfoPage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val versionName = BuildConfig.VERSION_NAME

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Navy),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            PageTopBar(
                title = "INFO APLIKASI",
                onBack = onBack
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF153C55),
                                Color(0xFF102337)
                            )
                        )
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.ic_zorasports_logo
                    ),
                    contentDescription = "Logo ZORASPORTS",
                    modifier = Modifier
                        .size(65.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Fit
                )

                Text(
                    "ZORASPORTS",
                    color = White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Text(
                    "SEPAK BOLA DALAM SATU APLIKASI",
                    color = Cyan,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Panel)
                    .padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow("Pengembang", "ZORA")

                HorizontalDivider(
                    color = ZoraThemeColors.border
                )

                InfoRow("Versi APK", versionName)

                HorizontalDivider(
                    color = ZoraThemeColors.border
                )

                InfoRow("Package", "com.zora.sports")
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Panel)
                    .padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Radio,
                        contentDescription = null,
                        tint = Cyan
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SALURAN WHATSAPP",
                            color = White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Pengumuman dan pembaruan ZORASPORTS.",
                            color = Muted,
                            fontSize = 10.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        uriHandler.openUri(WHATSAPP_CHANNEL)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Cyan,
                        contentColor = Navy
                    ),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "BUKA SALURAN WHATSAPP",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Text(
                "ZORASPORTS © ZORA",
                color = Muted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Muted,
            fontSize = 12.sp
        )

        Text(
            value,
            color = White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PageTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBack,
            color = Panel,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Kembali",
                tint = Cyan,
                modifier = Modifier.padding(11.dp)
            )
        }

        Spacer(Modifier.width(13.dp))

        Text(
            title,
            color = White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigate: (Int) -> Unit,
    onFullscreen: () -> Unit,
    fullscreen: Boolean,
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit
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
                fullscreen = fullscreen,
                onMenuClick = onMenuClick,
                onNotificationsClick = onNotificationsClick
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "DUNIA SEPAK BOLA",
                    color = Cyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Text(
                    "Sepak bola dimulai di sini.",
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
            SectionHeader("Pertandingan", "Lihat semua") {
                onNavigate(1)
            }
        }

        item {
            LiveMatchSection()
        }

        item {
            SectionHeader("Jelajahi", "Lihat liga") {
                onNavigate(4)
            }
        }

        item {
            ExploreGrid(onNavigate)
        }

        item {
            Text(
                "ZORASPORTS • SEPAK BOLA DALAM SATU APLIKASI",
                color = Muted,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun Header(
    onFullscreen: () -> Unit,
    fullscreen: Boolean,
    onMenuClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(
                    id = R.drawable.ic_zorasports_logo
                ),
                contentDescription = "Logo ZORASPORTS",
                modifier = Modifier
                    .size(43.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )

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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tombol Landscape
            Surface(
                modifier = Modifier.clickable {
                    onFullscreen()
                },
                shape = RoundedCornerShape(14.dp),
                color = Panel
            ) {
                Icon(
                    if (fullscreen) {
                        Icons.Default.FullscreenExit
                    } else {
                        Icons.Default.Fullscreen
                    },
                    contentDescription = "Layar penuh",
                    tint = Cyan,
                    modifier = Modifier.padding(11.dp)
                )
            }

            // Tombol Notifikasi
            Surface(
                modifier = Modifier.clickable {
                    onNotificationsClick()
                },
                shape = RoundedCornerShape(14.dp),
                color = Panel
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifikasi",
                    tint = Cyan,
                    modifier = Modifier.padding(11.dp)
                )
            }

            // Tombol Menu
            Surface(
                modifier = Modifier.clickable {
                    onMenuClick()
                },
                shape = RoundedCornerShape(14.dp),
                color = Panel
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Cyan,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
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
            modifier = Modifier.clickable { onClick() },
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
fun ExploreGrid(onNavigate: (Int) -> Unit) {
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
                ZoraThemeColors.border.copy(alpha = 0.5f),
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
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
fun LeaguePage(modifier: Modifier = Modifier) {
    StandingsSection(modifier = modifier)
}

@Composable
fun FavoritePage(modifier: Modifier = Modifier) {
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
                ZoraThemeColors.border.copy(alpha = 0.5f),
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
