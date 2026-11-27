
package com.zora.sports

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackGroup
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

private val StreamBackground
    get() = ZoraThemeColors.background

private val StreamPanel
    get() = ZoraThemeColors.panel

private val StreamPanelLight
    get() = ZoraThemeColors.panelLight

private val StreamBlue
    get() = ZoraThemeColors.accent

private val StreamWhite
    get() = ZoraThemeColors.text

private val StreamMuted
    get() = ZoraThemeColors.muted

private const val LIVE_LIMIT_MS = 120L * 60L * 1000L
private const val LIVE_WARNING_MS = 115L * 60L * 1000L

private data class StreamQualityOption(
    val trackGroup: TrackGroup,
    val trackIndex: Int,
    val label: String
)

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun formatWib(timestamp: Long): String {
    if (timestamp <= 0L) return "--:--"

    val formatter = SimpleDateFormat(
        "HH.mm",
        Locale("id", "ID")
    )

    formatter.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    return formatter.format(Date(timestamp))
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)

    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return String.format(
        Locale.US,
        "%02d:%02d:%02d",
        hours,
        minutes,
        seconds
    )
}

// =====================================================
// OVERLAY LAYER
// =====================================================

@Composable
private fun StreamOverlayLayer(
    design: StreamDesign
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val canvasWidth = constraints.maxWidth
        val canvasHeight = constraints.maxHeight

        design.overlays
            .filter { it.visible }
            .forEach { element ->
                key(element.id) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (element.x * canvasWidth).roundToInt(),
                                    (element.y * canvasHeight).roundToInt()
                                )
                            }
                            .graphicsLayer {
                                scaleX = element.scale
                                scaleY = element.scale
                                rotationZ = element.rotation
                            }
                    ) {
                        when (element.type.uppercase()) {
                            "IMAGE" -> {
                                if (element.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = element.imageUrl,
                                        contentDescription = "Logo streaming",
                                        modifier = Modifier.size(90.dp)
                                    )
                                }
                            }

                            "STICKER" -> {
                                Text(
                                    text = element.content,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            "TEXT" -> {
                                Text(
                                    text = element.content,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
    }
}

// =====================================================
// PUBLIC STREAM SCREEN
// =====================================================

@Composable
fun PublicStreamScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val currentVersionCode = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
        }.getOrDefault(0L)
    }

    var release by remember { mutableStateOf<AppRelease?>(null) }
    var releaseChecked by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val reference = FirebaseDatabase.getInstance(
            "https://zorasports-cf452-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("appRelease")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                release = snapshot.getValue(AppRelease::class.java)
                releaseChecked = true
            }
            override fun onCancelled(error: DatabaseError) {
                releaseChecked = true
            }
        }
        reference.addValueEventListener(listener)
        onDispose { reference.removeEventListener(listener) }
    }

    LaunchedEffect(Unit) {
        delay(8000L)
        releaseChecked = true
    }

    val requiredRelease = release?.takeIf {
        it.minimumVersionCode > currentVersionCode && it.apkUrl.isNotBlank()
    }
    val updateRequired = releaseChecked && requiredRelease != null

    var streams by remember {
        mutableStateOf<List<StreamLink>>(emptyList())
    }

    var selectedStream by remember {
        mutableStateOf<StreamLink?>(null)
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    var showExpiredMessage by remember {
        mutableStateOf(false)
    }

    DisposableEffect(Unit) {
        val listener = StreamRepository.listen(
            onData = { result ->
                streams = result.filter { it.published }
                loading = false
                error = null
            },
            onError = { message ->
                loading = false
                error = message
            }
        )

        onDispose {
            StreamRepository.removeListener(listener)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StreamBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ZORA SPORTS  /  BROADCAST", color = StreamBlue, fontSize = 9.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                Text("Siaran langsung", color = StreamWhite,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("Pertandingan pilihan, dalam satu layar.", color = StreamMuted,
                    style = MaterialTheme.typography.bodySmall)
            }
            Surface(
                color = StreamPanelLight,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZoraThemeColors.border)
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE35D68)))
                    Text("ON AIR", color = StreamWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(17.dp))

        when {
            !releaseChecked -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StreamBlue)
                }
            }

            updateRequired && requiredRelease != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(StreamPanel)
                            .border(1.dp, ZoraThemeColors.border, RoundedCornerShape(22.dp))
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("STREAMING TERKUNCI", color = StreamBlue,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold)
                        Text("UPDATE APK VERSI ${requiredRelease.versionName}", color = StreamWhite,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold)
                        Text("UNTUK MENONTON LIVE", color = StreamWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            requiredRelease.notes.ifBlank {
                                "Perbarui aplikasi ZORASPORTS ke versi terbaru untuk membuka semua siaran."
                            },
                            color = StreamMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { runCatching { uriHandler.openUri(requiredRelease.apkUrl) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StreamBlue,
                                contentColor = if (ZoraThemeColors.isLight) Color.White else StreamBackground
                            )
                        ) { Text("UPDATE SEKARANG", fontWeight = FontWeight.Bold) }
                        Text(
                            "Versi terpasang: ${currentVersionCode} • Versi minimum: ${requiredRelease.minimumVersionCode}",
                            color = StreamMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            loading && streams.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = StreamBlue)
                }
            }

            error != null && streams.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "Terjadi kesalahan",
                        color = StreamWhite
                    )
                }
            }

            streams.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada pertandingan tersedia",
                        color = StreamMuted
                    )
                }
            }

            else -> {
                val featured = streams.first()
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF1B3D55), Color(0xFF101D2D), Color(0xFF080F19))))
                            .clickable { selectedStream = featured }
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(horizontal = 16.dp, vertical = 13.dp)
                        ) {
                            Text(
                                featured.matchName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                featured.competition,
                                color = Color(0xFFBFC8D2),
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Putar live",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(62.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xAA152C40))
                                .padding(11.dp)
                        )
                        Text(
                            "LIVE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFF252A30))
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                        )
                        Text(
                            "PUTAR SIARAN",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(14.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("JADWAL SIARAN", color = StreamWhite, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            Text("${streams.size} pertandingan tersedia", color = StreamMuted, fontSize = 10.sp)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(
                            items = streams,
                            key = { it.id }
                        ) { stream ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(StreamPanel)
                                    .border(1.dp, ZoraThemeColors.border.copy(alpha = 0.65f), RoundedCornerShape(17.dp))
                                    .clickable { selectedStream = stream }
                                    .padding(horizontal = 13.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.linearGradient(listOf(StreamPanelLight, Color(0xFF1C4058)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        stream.competition.take(3).uppercase(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 11.dp)
                                ) {
                                    Text(
                                        stream.matchName,
                                        color = StreamWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        stream.competition,
                                        color = StreamMuted,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                    if (stream.information.isNotBlank()) {
                                        Text(
                                            stream.information,
                                            color = StreamWhite,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                    if (stream.description.isNotBlank()) {
                                        Text(
                                            stream.description,
                                            color = StreamMuted,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                                val now = System.currentTimeMillis()
                                val ended = stream.scheduledEndAt > 0L &&
                                    now >= stream.scheduledEndAt
                                val live = now >= stream.scheduledStartAt && !ended
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        if (ended) "SELESAI LIVE" else if (live) "LIVE" else formatWib(stream.scheduledStartAt),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (ended) Color(0xFF777D85)
                                                else if (live) Color(0xFFD62836)
                                                else Color(0xFF70757D)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                    Text(
                                        if (ended) "Selesai ${formatWib(stream.scheduledEndAt)}"
                                        else "Mulai ${formatWib(stream.scheduledStartAt)}",
                                        color = Color(0xFF454C54),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedStream?.let { stream ->
        StreamPlayerDialog(
            stream = stream,
            availableStreams = streams,
            onSelectStream = { selectedStream = it },
            onDismiss = {
                selectedStream = null
            },
            onExpired = {
                selectedStream = null
                showExpiredMessage = true
            }
        )
    }

    if (showExpiredMessage) {
        AlertDialog(
            onDismissRequest = {
                showExpiredMessage = false
            },
            containerColor = StreamPanel,
            title = {
                Text(
                    "SIARAN BERAKHIR",
                    color = StreamBlue,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Batas waktu 120 menit telah tercapai.",
                    color = StreamWhite
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExpiredMessage = false
                    }
                ) {
                    Text("MENGERTI", color = StreamBlue)
                }
            }
        )
    }
}

// =====================================================
// STREAM CARD
// =====================================================

@Composable
private fun StreamCard(
    stream: StreamLink,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StreamPanel)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFB32635))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "LANGSUNG",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = stream.competition,
                color = StreamMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = stream.matchName,
            color = StreamWhite,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = "Penyedia: ${stream.provider}",
            color = StreamMuted,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(StreamBlue)
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = StreamBackground
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = "TONTON SEKARANG",
                color = StreamBackground,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StreamChannelPicker(
    streams: List<StreamLink>,
    selectedId: String,
    onSelect: (StreamLink) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text("CHANNEL", color = StreamBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = StreamPanel) {
            streams.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.matchName.ifBlank { item.competition.ifBlank { "Channel" } }, color = if (item.id == selectedId) StreamBlue else StreamWhite, maxLines = 1) },
                    onClick = { expanded = false; onSelect(item) }
                )
            }
        }
    }
}

// =====================================================
// STREAM PLAYER DIALOG
// =====================================================

@Composable
private fun StreamPlayerDialog(
    stream: StreamLink,
    availableStreams: List<StreamLink>,
    onSelectStream: (StreamLink) -> Unit,
    onDismiss: () -> Unit,
    onExpired: () -> Unit
) {
    val context = LocalContext.current

    val activity = remember(context) {
        context.findActivity()
    }

    var fullScreen by remember {
        mutableStateOf(false)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var currentTime by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

    var showWarning by remember {
        mutableStateOf(false)
    }

    var warningShown by remember {
        mutableStateOf(false)
    }

    val startTime = stream.liveStartedAt
    val type = stream.sourceType.uppercase()

    val isWebSource = type == "YOUTUBE" ||
        type == "WEBSITE" ||
        type == "HTML"

    val elapsed = if (startTime > 0L) {
        (currentTime - startTime).coerceAtLeast(0L)
    } else {
        0L
    }

    LaunchedEffect(stream.id, startTime) {
        if (startTime > 0L) {
            while (true) {
                currentTime = System.currentTimeMillis()

                val liveElapsed = currentTime - startTime

                if (
                    liveElapsed >= LIVE_WARNING_MS &&
                    liveElapsed < LIVE_LIMIT_MS &&
                    !warningShown
                ) {
                    warningShown = true
                    showWarning = true
                }

                if (liveElapsed >= LIVE_LIMIT_MS) {
                    onExpired()
                    break
                }

                delay(1000L)
            }
        }
    }

    DisposableEffect(activity, fullScreen) {
        val previousOrientation =
            activity?.requestedOrientation
                ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        if (fullScreen) {
            activity?.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            activity?.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        onDispose {
            activity?.requestedOrientation = previousOrientation
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        if (fullScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (isWebSource) {
                    StreamWebPlayer(
                        url = stream.url,
                        fullScreen = true,
                        design = stream.design
                    )
                } else {
                    StreamExoPlayer(
                        url = stream.url,
                        fullScreen = true,
                        design = stream.design,
                        onPlaybackChanged = {
                            isPlaying = it
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Color(0xCC071321))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stream.matchName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        LiveTimerInfo(
                            startTime = startTime,
                            elapsed = elapsed
                        )
                    }

                    StreamChannelPicker(
                        streams = availableStreams,
                        selectedId = stream.id,
                        onSelect = onSelectStream
                    )

                    IconButton(
                        onClick = {
                            fullScreen = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Keluar dari layar penuh",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Tutup pemutar",
                            tint = Color.White
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StreamBackground)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stream.matchName,
                            color = StreamWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        LiveTimerInfo(
                            startTime = startTime,
                            elapsed = elapsed
                        )
                    }

                    StreamChannelPicker(
                        streams = availableStreams,
                        selectedId = stream.id,
                        onSelect = onSelectStream
                    )

                    IconButton(
                        onClick = {
                            fullScreen = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Layar penuh",
                            tint = StreamBlue
                        )
                    }

                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Tutup pemutar",
                            tint = StreamWhite
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (isWebSource) {
                    StreamWebPlayer(
                        url = stream.url,
                        fullScreen = false,
                        design = stream.design
                    )
                } else {
                    StreamExoPlayer(
                        url = stream.url,
                        fullScreen = false,
                        design = stream.design,
                        onPlaybackChanged = {
                            isPlaying = it
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isPlaying || isWebSource) {
                        "Siaran sedang diputar"
                    } else {
                        "Pemutar siap"
                    },
                    color = StreamMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = {
                showWarning = false
            },
            containerColor = StreamPanel,
            title = {
                Text(
                    "PERINGATAN SIARAN",
                    color = StreamBlue,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Siaran sudah berjalan 115 menit. Pemutar akan otomatis ditutup pada menit ke-120.",
                    color = StreamWhite
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWarning = false
                    }
                ) {
                    Text("MENGERTI", color = StreamBlue)
                }
            }
        )
    }
}

// =====================================================
// TIMER
// =====================================================

@Composable
private fun LiveTimerInfo(
    startTime: Long,
    elapsed: Long
) {
    val startLabel = if (startTime > 0L) {
        "${formatWib(startTime)} WIB"
    } else {
        "BELUM TERSEDIA"
    }

    val durationLabel = if (startTime > 0L) {
        formatDuration(elapsed)
    } else {
        "--:--:--"
    }

    Text(
        text = "MULAI $startLabel  •  DURASI $durationLabel",
        color = StreamBlue,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1
    )
}

// =====================================================
// EXOPLAYER + OVERLAY
// =====================================================

@Composable
private fun StreamExoPlayer(
    url: String,
    fullScreen: Boolean,
    design: StreamDesign,
    onPlaybackChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val player = remember(url) {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
    }

    var trackVersion by remember {
        mutableIntStateOf(0)
    }

    var qualityMenuExpanded by remember {
        mutableStateOf(false)
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onPlaybackChanged(isPlaying)
            }

            override fun onTracksChanged(
                tracks: androidx.media3.common.Tracks
            ) {
                trackVersion++
            }

            override fun onPlayerError(
                error: androidx.media3.common.PlaybackException
            ) {
                onPlaybackChanged(false)
            }
        }

        player.addListener(listener)

        onDispose {
            onPlaybackChanged(false)
            player.removeListener(listener)
            player.release()
        }
    }

    val qualityOptions = remember(player, trackVersion) {
        player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_VIDEO }
            .flatMap { group ->
                (0 until group.length).mapNotNull { index ->
                    if (!group.isTrackSupported(index)) {
                        null
                    } else {
                        val format = group.getTrackFormat(index)

                        if (format.height <= 0) {
                            null
                        } else {
                            StreamQualityOption(
                                trackGroup = group.mediaTrackGroup,
                                trackIndex = index,
                                label = "${format.height}p"
                            )
                        }
                    }
                }
            }
            .distinctBy { it.label }
            .sortedByDescending {
                it.label.removeSuffix("p").toIntOrNull() ?: 0
            }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = if (fullScreen) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
            }
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = true
                        resizeMode =
                            AspectRatioFrameLayout.RESIZE_MODE_FIT

                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = player
                }
            )

            StreamOverlayLayer(
                design = design
            )
        }

        if (!fullScreen) {
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = {
                            qualityMenuExpanded = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StreamBlue
                        )
                    ) {
                        Text("OTOMATIS")
                    }

                    DropdownMenu(
                        expanded = qualityMenuExpanded,
                        onDismissRequest = {
                            qualityMenuExpanded = false
                        },
                        containerColor = StreamPanel
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("OTOMATIS", color = StreamWhite)
                            },
                            onClick = {
                                player.trackSelectionParameters =
                                    player.trackSelectionParameters
                                        .buildUpon()
                                        .clearOverridesOfType(
                                            C.TRACK_TYPE_VIDEO
                                        )
                                        .build()

                                qualityMenuExpanded = false
                            }
                        )

                        qualityOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(option.label, color = StreamWhite)
                                },
                                onClick = {
                                    player.trackSelectionParameters =
                                        player.trackSelectionParameters
                                            .buildUpon()
                                            .clearOverridesOfType(
                                                C.TRACK_TYPE_VIDEO
                                            )
                                            .addOverride(
                                                TrackSelectionOverride(
                                                    option.trackGroup,
                                                    listOf(option.trackIndex)
                                                )
                                            )
                                            .build()

                                    qualityMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = if (qualityOptions.isEmpty()) {
                        "Kualitas otomatis"
                    } else {
                        "Pilih kualitas video"
                    },
                    color = StreamMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// =====================================================
// WEBVIEW + OVERLAY
// =====================================================

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun StreamWebPlayer(
    url: String,
    fullScreen: Boolean,
    design: StreamDesign
) {
    Box(
        modifier = if (fullScreen) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
        }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val originalHost = android.net.Uri.parse(url)
                    .host
                    ?.lowercase()
                    .orEmpty()

                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)

                    webChromeClient = WebChromeClient()

                    webViewClient = object : WebViewClient() {

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            if (!request.isForMainFrame) {
                                return false
                            }

                            val target = request.url
                            val targetHost = target.host
                                ?.lowercase()
                                .orEmpty()

                            val sameHost =
                                targetHost == originalHost ||
                                (
                                    originalHost.isNotEmpty() &&
                                    targetHost.endsWith(".$originalHost")
                                )

                            return target.scheme !in listOf("http", "https") ||
                                !sameHost
                        }
                    }

                    setBackgroundColor(
                        android.graphics.Color.BLACK
                    )

                    loadUrl(url)
                }
            },
            update = { view ->
                if (view.url != url) {
                    view.loadUrl(url)
                }
            }
        )

        StreamOverlayLayer(
            design = design
        )
    }
}
