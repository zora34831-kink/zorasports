package com.zora.sports

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale

private data class TvDestination(val label: String, val icon: ImageVector)

@Composable
fun ZoraSportsTvApp() {
    NotificationScheduleSync()
    AppUpdateNotice()
    val destinations = listOf(
        TvDestination("BERANDA", Icons.Default.Home),
        TvDestination("LIVE SCORE", Icons.Default.SportsSoccer),
        TvDestination("SIARAN", Icons.Default.PlayCircle),
        TvDestination("JADWAL", Icons.Default.CalendarMonth),
        TvDestination("LIGA", Icons.Default.EmojiEvents),
        TvDestination("FAVORIT", Icons.Default.Star),
        TvDestination("BERITA", Icons.Default.Newspaper),
        TvDestination("NOTIFIKASI", Icons.Default.Notifications)
    )
    var selected by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(false) }
    val firstNavFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstNavFocus.requestFocus() } }
    BackHandler(enabled = settings || selected != 0) {
        if (settings) settings = false else selected = 0
    }
    MaterialTheme(
        colorScheme = if (ZoraThemeColors.isLight) {
            lightColorScheme(
                primary = ZoraThemeColors.accent,
                background = ZoraThemeColors.background,
                surface = ZoraThemeColors.panel,
                surfaceVariant = ZoraThemeColors.panelLight,
                onBackground = ZoraThemeColors.text,
                onSurface = ZoraThemeColors.text,
                onSurfaceVariant = ZoraThemeColors.muted,
                outline = ZoraThemeColors.border,
                surfaceTint = ZoraThemeColors.accent
            )
        } else {
            darkColorScheme(
                primary = ZoraThemeColors.accent,
                background = ZoraThemeColors.background,
                surface = ZoraThemeColors.panel,
                surfaceVariant = ZoraThemeColors.panelLight,
                onBackground = ZoraThemeColors.text,
                onSurface = ZoraThemeColors.text,
                onSurfaceVariant = ZoraThemeColors.muted,
                outline = ZoraThemeColors.border,
                surfaceTint = ZoraThemeColors.accent
            )
        }
    ) {
        Row(
            Modifier.fillMaxSize().background(ZoraThemeColors.background)
        ) {
            Column(
                Modifier.width(224.dp).fillMaxHeight()
                    .background(ZoraThemeColors.navigation)
                    .padding(horizontal = 17.dp, vertical = 25.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("ZORA", color = ZoraThemeColors.accent, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                Text("SPORTS", color = ZoraThemeColors.text, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("SPORTS CENTER", color = ZoraThemeColors.muted, fontSize = 9.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(22.dp))
                destinations.forEachIndexed { index, item ->
                    val active = selected == index
                    val interaction = remember { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    Button(
                        onClick = { selected = index },
                        interactionSource = interaction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .then(if (index == 0) Modifier.focusRequester(firstNavFocus) else Modifier)
                            .border(
                                width = if (focused) 2.dp else 1.dp,
                                color = if (focused) ZoraThemeColors.accent else if (active) ZoraThemeColors.border else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .scale(if (focused) 1.025f else 1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (focused) ZoraThemeColors.panelLight.copy(alpha = 0.95f)
                                else if (active) ZoraThemeColors.panelLight else Color.Transparent,
                            contentColor = if (active || focused) ZoraThemeColors.accent else ZoraThemeColors.muted
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Icon(item.icon, null, Modifier.size(19.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(item.label, modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { settings = !settings }) {
                    Icon(Icons.Default.Settings, null, tint = ZoraThemeColors.muted)
                    Spacer(Modifier.width(8.dp))
                    Text("PENGATURAN", color = ZoraThemeColors.muted)
                }
            }
            Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 23.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("ZORASPORTS / ANDROID TV", color = ZoraThemeColors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(destinations[selected].label, color = ZoraThemeColors.text, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    }
                    Text("REMOTE READY", color = ZoraThemeColors.muted, fontSize = 9.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))
                        .background(ZoraThemeColors.panel)
                        .border(1.dp, ZoraThemeColors.border, RoundedCornerShape(22.dp))
                        .padding(15.dp)
                ) {
                    when {
                        settings -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("PENGATURAN", color = ZoraThemeColors.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Button(onClick = { ZoraThemeColors.isLight = !ZoraThemeColors.isLight }) { Text("GANTI TEMA") }
                            Text("Navigasi dirancang untuk remote D-pad Android TV.", color = ZoraThemeColors.muted)
                        }
                        selected == 0 -> HomeScreen(
                            modifier = Modifier.fillMaxSize(),
                            onNavigate = { selected = it },
                            onFullscreen = {},
                            fullscreen = false,
                            onMenuClick = { settings = true },
                            onNotificationsClick = { selected = 6 }
                        )
                        selected == 1 -> MatchPage("SKOR LANGSUNG", "Pantau pertandingan yang sedang berlangsung.", Modifier.fillMaxSize())
                        selected == 2 -> UpdateAwareStreamScreen(Modifier.fillMaxSize())
                        selected == 3 -> MatchPage("JADWAL PERTANDINGAN", "Lihat pertandingan hari ini dan jadwal mendatang.", Modifier.fillMaxSize())
                        selected == 4 -> LeaguePage(Modifier.fillMaxSize())
                        selected == 5 -> FavoritePage(Modifier.fillMaxSize())
                        selected == 6 -> FootballNewsPage(Modifier.fillMaxSize())
                        else -> NotificationsScreen(Modifier.fillMaxSize(), onBack = { selected = 0 })
                    }
                }
            }
        }
    }
}
