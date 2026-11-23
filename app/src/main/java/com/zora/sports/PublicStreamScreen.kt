
package com.zora.sports

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.database.ValueEventListener

private val StreamNavy = Color(0xFF071321)
private val StreamPanel = Color(0xFF102337)
private val StreamCyan = Color(0xFF42DDF5)
private val StreamMuted = Color(0xFF91A5B8)

private enum class StreamMode {
    VIDEO, YOUTUBE, WEBSITE
}

private fun resolveStreamMode(link: StreamLink): StreamMode {
    val type = link.sourceType.uppercase()
    val url = link.url.lowercase()

    return when (type) {
        "MP4", "VIDEO", "HLS", "M3U8", "DASH", "MPD" ->
            StreamMode.VIDEO

        "YOUTUBE" -> StreamMode.YOUTUBE

        "WEBSITE", "HTML", "WEB" ->
            StreamMode.WEBSITE

        else -> when {
            "youtube.com" in url || "youtu.be" in url ->
                StreamMode.YOUTUBE

            ".m3u8" in url || ".mpd" in url ||
                ".mp4" in url || ".m4v" in url ->
                StreamMode.VIDEO

            else -> StreamMode.WEBSITE
        }
    }
}

private fun createMediaItem(link: StreamLink): MediaItem {
    val type = link.sourceType.uppercase()

    val mimeType = when (type) {
        "HLS", "M3U8" -> MimeTypes.APPLICATION_M3U8
        "DASH", "MPD" -> MimeTypes.APPLICATION_MPD
        "MP4", "VIDEO" -> MimeTypes.VIDEO_MP4
        else -> null
    }

    val builder = MediaItem.Builder()
        .setUri(link.url)

    if (mimeType != null) {
        builder.setMimeType(mimeType)
    }

    return builder.build()
}

@Composable
fun PublicStreamScreen(
    modifier: Modifier = Modifier,
    pipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    var links by remember {
        mutableStateOf<List<StreamLink>>(emptyList())
    }

    var loading by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    var selectedLink by remember {
        mutableStateOf<StreamLink?>(null)
    }

    DisposableEffect(Unit) {
        val listener: ValueEventListener =
            StreamRepository.listen(
                onData = { data ->
                    links = data.filter { it.published }
                    loading = false
                    error = ""

                    selectedLink?.let { current ->
                        selectedLink = links.find {
                            it.id == current.id
                        }
                    }
                },
                onError = { message ->
                    error = message
                    loading = false
                }
            )

        onDispose {
            StreamRepository.removeListener(listener)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StreamNavy)
            .then(
                if (pipMode) Modifier
                else Modifier.padding(18.dp)
            )
    ) {
        if (!pipMode) {
            Text(
                "LIVE STREAMING",
                color = StreamCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Tonton Pertandingan",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Siaran pertandingan pilihan di ZORASPORTS.",
                color = StreamMuted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(18.dp))
        }

        selectedLink?.let { link ->
            if (!pipMode) {
                Text(
                    "SEDANG DIPUTAR",
                    color = StreamCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    link.matchName,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    link.competition,
                    color = StreamMuted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(12.dp))
            }

            StreamSourcePlayer(
                link = link,
                pipMode = pipMode,
                onEnterPip = onEnterPip
            )

            if (!pipMode) {
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            selectedLink = null
                        }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = StreamCyan
                        )

                        Spacer(Modifier.width(5.dp))

                        Text(
                            "Tutup pemutar",
                            color = StreamCyan
                        )
                    }
                }
            }
        }

        if (!pipMode) {
            Text(
                "DAFTAR SIARAN",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = StreamCyan
                        )
                    }
                }

                error.isNotBlank() -> {
                    Text(
                        "Gagal memuat streaming: $error",
                        color = Color(0xFFFF8A80),
                        fontSize = 12.sp
                    )
                }

                links.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 35.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = StreamCyan,
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Belum ada siaran tersedia",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Cek kembali nanti.",
                                color = StreamMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            links,
                            key = { it.id }
                        ) { link ->

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selectedLink?.id == link.id)
                                            Color(0xFF173B50)
                                        else
                                            StreamPanel,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = StreamCyan,
                                        modifier = Modifier.size(34.dp)
                                    )

                                    Spacer(Modifier.width(12.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            link.matchName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )

                                        Text(
                                            link.competition,
                                            color = StreamMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(
                                        vertical = 12.dp
                                    ),
                                    color = Color.White.copy(alpha = 0.1f)
                                )

                                Text(
                                    "Penyedia: ${link.provider}",
                                    color = StreamMuted,
                                    fontSize = 11.sp
                                )

                                Spacer(Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        selectedLink = link
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StreamCyan,
                                        contentColor = StreamNavy
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayCircle,
                                        contentDescription = null
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        "TONTON SEKARANG",
                                        fontWeight = FontWeight.Bold
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
private fun StreamSourcePlayer(
    link: StreamLink,
    pipMode: Boolean,
    onEnterPip: () -> Unit
) {
    when (resolveStreamMode(link)) {
        StreamMode.VIDEO -> StreamVideoPlayer(
            link = link,
            pipMode = pipMode,
            onEnterPip = onEnterPip
        )

        StreamMode.YOUTUBE,
        StreamMode.WEBSITE -> StreamWebPlayer(link.url)
    }
}

@Composable
private fun StreamVideoPlayer(
    link: StreamLink,
    pipMode: Boolean,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var fullscreen by remember(link.id) {
        mutableStateOf(false)
    }

    var playbackError by remember(link.id) {
        mutableStateOf("")
    }

    val player = remember(link.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(createMediaItem(link))
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlayerError(
                    error: PlaybackException
                ) {
                    playbackError = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                            "Format streaming tidak valid."

                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            "Koneksi atau server streaming bermasalah."

                        PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ->
                            "Siaran live perlu dimuat ulang."

                        else ->
                            error.message ?: "Sumber tidak dapat diputar."
                    }
                }
            })
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    DisposableEffect(activity, fullscreen) {
        if (activity != null && fullscreen) {
            val oldOrientation =
                activity.requestedOrientation

            val controller = WindowInsetsControllerCompat(
                activity.window,
                activity.window.decorView
            )

            activity.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            controller.hide(
                WindowInsetsCompat.Type.systemBars()
            )

            controller.systemBarsBehavior =
                WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            onDispose {
                controller.show(
                    WindowInsetsCompat.Type.systemBars()
                )
                activity.requestedOrientation = oldOrientation
            }
        } else {
            onDispose {}
        }
    }

    BackHandler(enabled = fullscreen) {
        fullscreen = false
    }

    if (fullscreen) {
        Dialog(
            onDismissRequest = {
                fullscreen = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            this.player = player
                            useController = true
                            setShowBuffering(
                                PlayerView.SHOW_BUFFERING_ALWAYS
                            )
                        }
                    },
                    update = {
                        it.player = player
                    }
                )

                IconButton(
                    onClick = {
                        fullscreen = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(
                            Color.Black.copy(alpha = 0.65f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.FullscreenExit,
                        contentDescription = "Keluar layar penuh",
                        tint = Color.White
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.Black,
                    RoundedCornerShape(14.dp)
                )
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = true
                        setShowBuffering(
                            PlayerView.SHOW_BUFFERING_ALWAYS
                        )
                    }
                },
                update = {
                    it.player = player
                }
            )

            if (!pipMode) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = onEnterPip,
                        colors = ButtonDefaults.textButtonColors(
                            containerColor =
                                Color.Black.copy(alpha = 0.75f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            "PiP",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            fullscreen = true
                        },
                        modifier = Modifier.background(
                            Color.Black.copy(alpha = 0.65f),
                            RoundedCornerShape(50)
                        )
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Layar penuh",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    if (playbackError.isNotBlank()) {
        Spacer(Modifier.height(8.dp))

        Text(
            "Gagal memutar video: $playbackError",
            color = Color(0xFFFF8A80),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StreamWebPlayer(url: String) {
    var fullscreen by remember(url) {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    val webView = remember(url) {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.BLACK)

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false

            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()

            loadUrl(url)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    BackHandler(enabled = fullscreen) {
        fullscreen = false
    }

    if (fullscreen) {
        Dialog(
            onDismissRequest = {
                fullscreen = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { webView }
                )

                IconButton(
                    onClick = {
                        fullscreen = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.FullscreenExit,
                        contentDescription = "Keluar layar penuh",
                        tint = Color.White
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(
                    Color.Black,
                    RoundedCornerShape(14.dp)
                )
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { webView }
            )

            IconButton(
                onClick = {
                    fullscreen = true
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = "Layar penuh",
                    tint = Color.White
                )
            }
        }
    }
}
