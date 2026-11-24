
package com.zora.sports

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val NotificationNavy get() = ZoraThemeColors.background
private val NotificationPanel get() = ZoraThemeColors.panel
private val NotificationCyan get() = ZoraThemeColors.accent
private val NotificationMuted get() = ZoraThemeColors.muted
private val NotificationWhite get() = ZoraThemeColors.text

@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var streams by remember {
        mutableStateOf<List<StreamLink>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var notificationsEnabled by remember {
        mutableStateOf(
            StreamNotificationScheduler.isEnabled(context)
        )
    }

    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted

        if (granted) {
            notificationsEnabled = true
            StreamNotificationScheduler.setEnabled(
                context,
                true
            )
            StreamNotificationScheduler.sync(
                context,
                streams
            )
        } else {
            notificationsEnabled = false
            StreamNotificationScheduler.setEnabled(
                context,
                false
            )
        }
    }

    DisposableEffect(Unit) {
        val listener: ValueEventListener = StreamRepository.listen(
            onData = { result ->
                streams = result
                    .filter { it.published && it.scheduledStartAt > 0L }
                    .sortedBy { it.scheduledStartAt }

                loading = false

                StreamNotificationScheduler.sync(
                    context,
                    result
                )
            },
            onError = {
                loading = false
            }
        )

        onDispose {
            StreamRepository.removeListener(listener)
        }
    }

    val now = System.currentTimeMillis()

    val upcoming = streams.filter {
        it.scheduledStartAt > now
    }

    val started = streams.filter {
        it.scheduledStartAt <= now
    }.sortedByDescending {
        it.scheduledStartAt
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NotificationNavy),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    color = NotificationPanel,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = NotificationCyan,
                        modifier = Modifier.padding(11.dp)
                    )
                }

                Spacer(Modifier.width(13.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "NOTIFIKASI",
                        color = NotificationWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Pengingat jadwal pertandingan",
                        color = NotificationMuted,
                        fontSize = 11.sp
                    )
                }

                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = NotificationCyan,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NotificationPanel)
                    .border(
                        1.dp,
                        NotificationCyan.copy(alpha = 0.2f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "PENGINGAT PERTANDINGAN",
                            color = NotificationWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            "Terima notifikasi saat pertandingan memasuki jadwal mulai.",
                            color = NotificationMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                notificationsEnabled = false
                                StreamNotificationScheduler.setEnabled(
                                    context,
                                    false
                                )
                            } else if (Build.VERSION.SDK_INT >= 33 &&
                                !permissionGranted
                            ) {
                                permissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            } else {
                                notificationsEnabled = true
                                StreamNotificationScheduler.setEnabled(
                                    context,
                                    true
                                )
                                StreamNotificationScheduler.sync(
                                    context,
                                    streams
                                )
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NotificationCyan,
                            uncheckedThumbColor = NotificationMuted
                        )
                    )
                }

                HorizontalDivider(
                    color = ZoraThemeColors.border
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (notificationsEnabled) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = if (notificationsEnabled) {
                            NotificationCyan
                        } else {
                            NotificationMuted
                        },
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        when {
                            notificationsEnabled ->
                                "Pengingat telah diaktifkan"

                            Build.VERSION.SDK_INT >= 33 && !permissionGranted ->
                                "Izin notifikasi belum diberikan"

                            else ->
                                "Pengingat sedang nonaktif"
                        },
                        color = NotificationMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "JADWAL MENDATANG",
                    color = NotificationWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    "${upcoming.size} pertandingan",
                    color = NotificationCyan,
                    fontSize = 10.sp
                )
            }
        }

        if (loading) {
            item {
                NotificationLoadingCard()
            }
        } else if (upcoming.isEmpty()) {
            item {
                NotificationEmptyCard(
                    title = "Belum ada jadwal mendatang",
                    description = "Jadwal yang dipublikasikan admin akan muncul di sini."
                )
            }
        } else {
            items(
                items = upcoming,
                key = { it.id }
            ) { stream ->
                NotificationMatchCard(
                    stream = stream,
                    started = false
                )
            }
        }

        item {
            Text(
                "PERTANDINGAN SUDAH DIMULAI",
                color = NotificationWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (started.isEmpty()) {
            item {
                NotificationEmptyCard(
                    title = "Belum ada pertandingan",
                    description = "Pertandingan yang melewati waktu mulai akan muncul di sini."
                )
            }
        } else {
            items(
                items = started,
                key = { "started_${it.id}" }
            ) { stream ->
                NotificationMatchCard(
                    stream = stream,
                    started = true
                )
            }
        }
    }
}

@Composable
private fun NotificationMatchCard(
    stream: StreamLink,
    started: Boolean
) {
    val date = remember(stream.scheduledStartAt) {
        val formatter = SimpleDateFormat(
            "EEEE, dd MMMM yyyy",
            Locale("id", "ID")
        ).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }

        formatter.format(Date(stream.scheduledStartAt))
    }

    val time = remember(stream.scheduledStartAt) {
        val formatter = SimpleDateFormat(
            "HH:mm",
            Locale("id", "ID")
        ).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }

        formatter.format(Date(stream.scheduledStartAt))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(NotificationPanel)
            .border(
                1.dp,
                ZoraThemeColors.border,
                RoundedCornerShape(17.dp)
            )
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (started) {
                    Color(0xFF174B3B)
                } else {
                    NotificationCyan.copy(alpha = 0.13f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    if (started) "DIMULAI" else "TERJADWAL",
                    color = if (started) {
                        Color(0xFF72D9AC)
                    } else {
                        NotificationCyan
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 9.dp,
                        vertical = 6.dp
                    )
                )
            }

            Spacer(Modifier.weight(1f))

            Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = NotificationMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            stream.matchName.ifBlank { "Pertandingan" },
            color = NotificationWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        if (stream.competition.isNotBlank()) {
            Text(
                stream.competition,
                color = NotificationMuted,
                fontSize = 11.sp
            )
        }

        HorizontalDivider(
            color = ZoraThemeColors.border
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = NotificationCyan,
                modifier = Modifier.size(17.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                date,
                color = NotificationMuted,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )

            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = NotificationCyan,
                modifier = Modifier.size(17.dp)
            )

            Spacer(Modifier.width(5.dp))

            Text(
                "$time WIB",
                color = NotificationWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NotificationEmptyCard(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(NotificationPanel)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.EventAvailable,
            contentDescription = null,
            tint = NotificationMuted,
            modifier = Modifier.size(30.dp)
        )

        Text(
            title,
            color = NotificationWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Text(
            description,
            color = NotificationMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun NotificationLoadingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(NotificationPanel)
            .padding(25.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = NotificationCyan,
            modifier = Modifier.size(27.dp),
            strokeWidth = 2.dp
        )
    }
}
