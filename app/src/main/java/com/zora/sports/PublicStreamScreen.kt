
package com.zora.sports

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
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
import com.google.firebase.database.ValueEventListener

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

    DisposableEffect(Unit) {
        val listener: ValueEventListener =
            StreamRepository.listen(
                onData = { data ->
                    links = data.filter { it.published }
                    loading = false
                    error = ""
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

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF071321))
            .padding(18.dp)
    ) {
        Text(
            "LIVE STREAMING",
            color = Color(0xFF42DDF5),
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
            "Link siaran resmi yang dipublikasikan administrator.",
            color = Color(0xFF91A5B8),
            fontSize = 12.sp
        )

        Spacer(Modifier.height(20.dp))

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF42DDF5)
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
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color(0xFF42DDF5),
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
                            color = Color(0xFF91A5B8),
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

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF102337),
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    val uri = Uri.parse(link.url)
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        uri
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF42DDF5),
                                    modifier = Modifier.size(30.dp)
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
                                        color = Color(0xFF91A5B8),
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    "BUKA",
                                    color = Color(0xFF42DDF5),
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
                                color = Color(0xFF91A5B8),
                                fontSize = 11.sp
                            )

                            Text(
                                link.url,
                                color = Color(0xFF42DDF5),
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
