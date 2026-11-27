
package com.zora.sports

import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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

        val isAndroidTv = packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_LEANBACK
        )
        setContent {
            if (isAndroidTv) ZoraSportsTvApp() else ZoraSportsApp()
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
    // Tampilkan pemberitahuan pembaruan saat versi baru tersedia.
    AppUpdateNotice()

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
        AppTab("Market", Icons.Default.Store)
    )

    MaterialTheme(
        colorScheme = if (lightMode) {
            lightColorScheme(
                primary = Cyan,
                background = Navy,
                surface = Panel,
                surfaceVariant = PanelLight,
                onBackground = White,
                onSurface = White,
                onSurfaceVariant = Muted,
                outline = ZoraThemeColors.border,
                surfaceTint = Cyan,
                inverseSurface = if (lightMode) Color(0xFF182B3A) else Color(0xFFE8F1F7),
                inverseOnSurface = if (lightMode) Color(0xFFF4FAFF) else Color(0xFF142536)
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

            val pageKey = when {
                extraPage.isNotEmpty() -> "extra:$extraPage"
                showNotifications -> "notifications"
                else -> "tab:$selectedTab"
            }
            AnimatedContent(
                targetState = pageKey,
                transitionSpec = {
                    (fadeIn(tween(190)) + slideInHorizontally(tween(230)) { it / 16 }) togetherWith
                        (fadeOut(tween(130)) + slideOutHorizontally(tween(190)) { -it / 20 })
                },
                label = "zora_page_transition"
            ) { targetPage ->
            when {
                targetPage == "extra:DUKUNGAN" -> {
                    SupportPage(
                        modifier = pageModifier,
                        onBack = { extraPage = "" }
                    )
                }

                targetPage == "extra:INFO APK" -> {
                    AppInfoPage(
                        modifier = pageModifier,
                        onBack = { extraPage = "" }
                    )
                }

                targetPage == "extra:BERITA" -> {
                    FootballNewsPage(modifier = pageModifier)
                }


                targetPage == "notifications" -> {
                    NotificationsScreen(
                        modifier = pageModifier,
                        onBack = {
                            showNotifications = false
                        }
                    )
                }

                else -> {
                    val targetTab = targetPage.removePrefix("tab:").toIntOrNull() ?: selectedTab
                    when (targetTab) {
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

                        2 -> UpdateAwareStreamScreen(
                            modifier = pageModifier
                        )

                        3 -> MatchPage(
                            title = "JADWAL PERTANDINGAN",
                            description =
                                "Lihat pertandingan hari ini dan jadwal mendatang.",
                            modifier = pageModifier
                        )

                        4 -> LeaguePage(modifier = pageModifier)
                        5 -> TransferCenterPage(modifier = pageModifier)
                    }
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
            onNews = {
                showHamburger = false
                extraPage = "BERITA"
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
                        (if (ZoraThemeColors.isLight) Color(0xFF7890A2) else Color.White).copy(alpha = 0.34f),
                        Cyan.copy(alpha = 0.20f),
                        (if (ZoraThemeColors.isLight) Color(0xFF7890A2) else Color.White).copy(alpha = 0.08f)
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
                    animationSpec = spring(
                        dampingRatio = 0.62f,
                        stiffness = 520f
                    ),
                    label = "nav_icon_scale"
                )
                val tint by animateColorAsState(
                    targetValue = if (selected) (if (ZoraThemeColors.isLight) Color(0xFF142536) else Color.White) else Muted,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 650f),
                    label = "nav_icon_tint"
                )
                val pillColor by animateColorAsState(
                    targetValue = if (selected) {
                        Cyan.copy(alpha = 0.28f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 560f),
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
                        color = if (selected) White else Muted,
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

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 1.dp)
                .fillMaxWidth(0.42f)
                .height(1.dp)
                .clip(CircleShape)
                .background((if (ZoraThemeColors.isLight) Color(0xFF7890A2) else Color.White).copy(alpha = 0.42f))
        )
    }
}

@Composable
fun HamburgerMenu(
    onDismiss: () -> Unit,
    onSupport: () -> Unit,
    onInfo: () -> Unit,
    onNews: () -> Unit,
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
                    title = "FOOTBALL NEWS",
                    subtitle = "Berita sepak bola terbaru",
                    icon = Icons.Default.Newspaper,
                    onClick = onNews
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
        modifier = modifier.fillMaxSize().background(Navy),
        contentPadding = PaddingValues(horizontal = 17.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.spacedBy(19.dp)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(29.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF164D69),
                                Color(0xFF102D47),
                                Color(0xFF101E34)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(Cyan.copy(alpha = 0.65f), Color.White.copy(alpha = 0.08f))
                        ),
                        RoundedCornerShape(29.dp)
                    )
                    .clickable { onNavigate(1) }
                    .padding(21.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(142.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 37.dp, y = 27.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                        .clip(CircleShape)
                        .background(Cyan.copy(alpha = 0.07f))
                )
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 12.dp, y = (-18).dp)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(0.86f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF5B63))
                        )
                        Text(
                            "THE HOME OF FOOTBALL",
                            color = Color(0xFFBDEEFF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.7.sp
                        )
                    }
                    Text(
                        "SEMUA AKSI.\nSATU LAYAR.",
                        color = Color.White,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        "Ikuti pertandingan, siaran, dan kabar sepak bola pilihanmu.",
                        color = Color(0xFFC1D2DF),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Cyan)
                            .padding(horizontal = 13.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            "JELAJAHI PERTANDINGAN",
                            color = Color(0xFF071725),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.4.sp
                        )
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF071725),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "MATCH CENTER",
                        color = Cyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Text(
                        "Pertandingan pilihan",
                        color = White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(
                    color = PanelLight,
                    shape = RoundedCornerShape(10.dp),
                    onClick = { onNavigate(1) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SEMUA", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ChevronRight, null, tint = Cyan, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        item { LiveMatchSection(showHeader = false) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "EXPLORE MORE",
                    color = Cyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp
                )
                Text(
                    "Jelajahi sepak bola",
                    color = White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        item { ExploreGrid(onNavigate) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(22.dp).height(2.dp).clip(CircleShape).background(Cyan))
                Spacer(Modifier.width(8.dp))
                Text(
                    "ZORASPORTS  /  FOOTBALL NEVER STOPS",
                    color = Muted,
                    fontSize = 8.sp,
                    letterSpacing = 1.1.sp
                )
            }
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
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1C536D), Color(0xFF10283F))
                        )
                    )
                    .border(1.dp, Cyan.copy(alpha = 0.28f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_zorasports_logo),
                    contentDescription = "Logo ZORASPORTS",
                    modifier = Modifier.size(34.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "ZORA",
                    color = White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.3.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SPORTS",
                        color = Cyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.8.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.width(19.dp).height(1.dp).background(Cyan.copy(alpha = 0.55f)))
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Triple(if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Layar penuh", onFullscreen),
                Triple(Icons.Default.NotificationsNone, "Notifikasi", onNotificationsClick),
                Triple(Icons.Default.Menu, "Menu", onMenuClick)
            ).forEach { (icon, description, action) ->
                Surface(
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(13.dp))
                        .clickable { action() },
                    shape = RoundedCornerShape(13.dp),
                    color = Panel
                ) {
                    Icon(
                        icon,
                        contentDescription = description,
                        tint = Color(0xFFD6EAF5),
                        modifier = Modifier.padding(10.dp).size(18.dp)
                    )
                }
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(21.dp))
            .background(
                Brush.linearGradient(
                    listOf(PanelLight, Panel, Color(0xFF101F31))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Cyan.copy(alpha = 0.22f), ZoraThemeColors.border.copy(alpha = 0.35f))
                ),
                RoundedCornerShape(21.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Icon(
            Icons.Default.NorthEast,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.18f),
            modifier = Modifier.align(Alignment.TopEnd).size(16.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                modifier = Modifier
                    .size(39.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Cyan.copy(alpha = 0.12f))
                    .border(1.dp, Cyan.copy(alpha = 0.15f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = Cyan, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                color = White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(5.dp))
            Text(
                subtitle,
                color = Muted,
                fontSize = 9.sp,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun MatchPage(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(Navy),
        contentPadding = PaddingValues(horizontal = 17.dp, vertical = 17.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(25.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF174760), Color(0xFF102A42), Color(0xFF101C30))
                        )
                    )
                    .border(1.dp, Cyan.copy(alpha = 0.22f), RoundedCornerShape(25.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "ZORA SPORTS  /  MATCH CENTER",
                        color = Color(0xFFB9E9F8),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        description,
                        color = Color(0xFFC1D2DF),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(22.dp).clip(CircleShape).background(Cyan))
                Spacer(Modifier.width(9.dp))
                Text("UPDATE PERTANDINGAN", color = White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp)
            }
        }
        item { LiveMatchSection(showHeader = false) }
    }
}

@Composable
fun LeaguePage(modifier: Modifier = Modifier) {
    StandingsSection(modifier = modifier)
}

@Composable
fun FavoritePage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(Navy).padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(27.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF18364D), Panel, Color(0xFF111D30))
                    )
                )
                .border(1.dp, Cyan.copy(alpha = 0.20f), RoundedCornerShape(27.dp))
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Cyan.copy(alpha = 0.12f))
                    .border(1.dp, Cyan.copy(alpha = 0.22f), RoundedCornerShape(25.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Cyan, modifier = Modifier.size(37.dp))
            }
            Text(
                "YOUR CLUBS",
                color = Cyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                "TIM FAVORIT",
                color = White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                "Belum ada tim favorit yang tersimpan. Area ini akan menjadi tempat untuk mengikuti klub pilihanmu.",
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = Cyan, modifier = Modifier.size(15.dp))
                Text("PILIH TIM DARI HALAMAN LIGA", color = Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
            }
        }
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
