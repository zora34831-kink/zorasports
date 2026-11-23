
package com.zora.sports

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.database.ValueEventListener

private val StreamNavy = Color(0xFF071321)
private val StreamPanel = Color(0xFF102337)
private val StreamCyan = Color(0xFF42DDF5)
private val StreamMuted = Color(0xFF91A5B8)

@Composable
fun PublicStreamScreen(
    modifier: Modifier = Modifier
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
            .padding(18.dp)
    ) {
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
            "Siaran resmi langsung di aplikasi ZORASPORTS.",
            color = StreamMuted,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(18.dp))

        selectedLink?.let { link ->
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

            StreamVideoPlayer(
                url = link.url
            )

            Spacer(Modifier.height(12.dp))

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
                        horizontalAlignment = Alignment.CenterHorizontally
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        links,
                        key = { it.id }
                    ) { link ->

                        val isSelected =
                            selectedLink?.id == link.id

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected)
                                        Color(0xFF173B50)
                                    else
                                        StreamPanel,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    selectedLink = link
                                }
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
                                    modifier = Modifier.size(32.dp)
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

                                Text(
                                    if (isSelected) "DIPILIH"
                                    else "PUTAR",
                                    color = StreamCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
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

                            Text(
                                link.url,
                                color = StreamCyan,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamVideoPlayer(
    url: String
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var fullscreen by remember(url) {
        mutableStateOf(false)
    }

    var playbackError by remember(url) {
        mutableStateOf("")
    }

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlayerError(
                    error: PlaybackException
                ) {
                    playbackError =
                        error.message ?: "Video tidak dapat diputar."
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

            val controller =
                WindowInsetsControllerCompat(
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

                activity.requestedOrientation =
                    oldOrientation
            }
        } else {
            onDispose {}
        }
    }

    BackHandler(
        enabled = fullscreen
    ) {
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

            IconButton(
                onClick = {
                    fullscreen = true
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .background(
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

    if (playbackError.isNotBlank()) {
        Spacer(Modifier.height(8.dp))

        Text(
            "Gagal memutar video: $playbackError",
            color = Color(0xFFFF8A80),
            fontSize = 12.sp
        )
    }
}
