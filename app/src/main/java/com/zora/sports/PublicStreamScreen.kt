
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val StreamBackground = Color(0xFF071321)
private val StreamPanel = Color(0xFF102337)
private val StreamBlue = Color(0xFF42DDF5)
private val StreamWhite = Color(0xFFF4FAFF)
private val StreamMuted = Color(0xFF91A5B8)

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
        "HH:mm",
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

@Composable
fun PublicStreamScreen(
    modifier: Modifier = Modifier
) {
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

        Text(
            text = "LIVE STREAMING",
            color = StreamWhite,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Saksikan pertandingan pilihan",
            color = StreamMuted,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(18.dp))

        when {
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = streams,
                        key = { it.id }
                    ) { stream ->
                        StreamCard(
                            stream = stream,
                            onClick = {
                                selectedStream = stream
                            }
                        )
                    }
                }
            }
        }
    }

    selectedStream?.let { stream ->
        StreamPlayerDialog(
            stream = stream,
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
                    "LIVE BERAKHIR",
                    color = StreamBlue,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "BATAS WAKTU 120 MENIT TERCAPAI",
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
                    text = "LIVE",
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
private fun StreamPlayerDialog(
    stream: StreamLink,
    onDismiss: () -> Unit,
    onExpired: () -> Unit
) {
    val context = LocalContext.current

    val activity = remember(context) {
        context.findActivity()
    }

    var fullScreen by remember {
        mutableStateOf(true)
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

    val remaining = if (startTime > 0L) {
        (LIVE_LIMIT_MS - elapsed).coerceAtLeast(0L)
    } else {
        LIVE_LIMIT_MS
    }

    // Timer mengikuti waktu mulai yang tersimpan di Firebase.
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

    // Otomatis masuk landscape saat player dibuka.
    DisposableEffect(activity) {
        val previousOrientation =
            activity?.requestedOrientation
                ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        activity?.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

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
                        fullScreen = true
                    )
                } else {
                    StreamExoPlayer(
                        url = stream.url,
                        fullScreen = true,
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
                            color = StreamWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        LiveTimerInfo(
                            startTime = startTime,
                            elapsed = elapsed
                        )
                    }

                    TextButton(
                        onClick = {
                            fullScreen = false
                        }
                    ) {
                        Text(
                            text = "EXIT FULL SCREEN",
                            color = StreamBlue
                        )
                    }

                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Tutup player",
                            tint = StreamWhite
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

                    TextButton(
                        onClick = {
                            fullScreen = true
                        }
                    ) {
                        Text(
                            text = "FULL SCREEN",
                            color = StreamBlue
                        )
                    }

                    IconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Tutup player",
                            tint = StreamWhite
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (isWebSource) {
                    StreamWebPlayer(
                        url = stream.url,
                        fullScreen = false
                    )
                } else {
                    StreamExoPlayer(
                        url = stream.url,
                        fullScreen = false,
                        onPlaybackChanged = {
                            isPlaying = it
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isPlaying || isWebSource) {
                        "Streaming"
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
                    "PERINGATAN LIVE",
                    color = StreamBlue,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Live sudah berjalan 115 menit. Player akan otomatis ditutup pada menit ke-120.",
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

@Composable
private fun StreamExoPlayer(
    url: String,
    fullScreen: Boolean,
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
        AndroidView(
            modifier = if (fullScreen) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
            },
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
                        Text("AUTO")
                    }

                    DropdownMenu(
                        expanded = qualityMenuExpanded,
                        onDismissRequest = {
                            qualityMenuExpanded = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = { Text("AUTO") },
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
                                text = { Text(option.label) },
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
                        "Pilih kualitas"
                    },
                    color = StreamMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun StreamWebPlayer(
    url: String,
    fullScreen: Boolean
) {
    AndroidView(
        modifier = if (fullScreen) {
            Modifier.fillMaxSize()
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
        },
        factory = { context ->
            val originalHost = android.net.Uri.parse(url).host
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

                setBackgroundColor(android.graphics.Color.BLACK)

                loadUrl(url)
            }
        },
        update = { view ->
            if (view.url != url) {
                view.loadUrl(url)
            }
        }
    )
}
