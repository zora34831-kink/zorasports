package com.zora.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private data class PublicAppRelease(
    val versionCode: Long = 0L,
    val versionName: String = "",
    val apkUrl: String = "",
    val notes: String = "",
    val minimumVersionCode: Long = 0L
)

private object PublicAppReleaseSource {
    private val reference = FirebaseDatabase.getInstance(
        "https://zorasports-cf452-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).getReference("appRelease")

    fun listen(
        onData: (PublicAppRelease?) -> Unit,
        onError: () -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onData(snapshot.getValue(PublicAppRelease::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                onError()
            }
        }
        reference.addValueEventListener(listener)
        return listener
    }

    fun remove(listener: ValueEventListener) {
        reference.removeEventListener(listener)
    }
}

@Composable
fun AppUpdateNotice() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val installedVersion = remember {
        runCatching {
            PackageInfoCompat.getLongVersionCode(
                context.packageManager.getPackageInfo(context.packageName, 0)
            )
        }.getOrDefault(0L)
    }
    var release by remember { mutableStateOf<PublicAppRelease?>(null) }
    var checked by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = PublicAppReleaseSource.listen(
            onData = {
                release = it
                checked = true
                dismissed = false
            },
            onError = { checked = true }
        )
        onDispose { PublicAppReleaseSource.remove(listener) }
    }

    val available = release?.let {
        it.versionCode > installedVersion && it.apkUrl.startsWith("https://")
    } == true

    if (checked && available && !dismissed) {
        val latest = release!!
        val required = latest.minimumVersionCode > installedVersion
        AlertDialog(
            onDismissRequest = { dismissed = true },
            containerColor = Color(0xFF14283B),
            title = {
                Text(
                    if (required) "PEMBARUAN WAJIB" else "PEMBARUAN TERSEDIA",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        "ZORASPORTS versi ${latest.versionName} sudah tersedia.",
                        color = Color(0xFFEAF2F9)
                    )
                    if (latest.notes.isNotBlank()) {
                        Text(latest.notes, color = Color(0xFFB8C8D8))
                    }
                    if (required) {
                        Text(
                            "Pembaruan ini wajib dipasang untuk melanjutkan akses Streaming.",
                            color = Color(0xFFFFD18A)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { uriHandler.openUri(latest.apkUrl) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2D83C6)
                    )
                ) { Text("UPDATE SEKARANG") }
            },
            dismissButton = {
                if (!required) {
                    OutlinedButton(onClick = { dismissed = true }) {
                        Text("NANTI")
                    }
                }
            }
        )
    }
    AnnouncementNotice()
}


private data class PublicAnnouncement(
    val information: String = "",
    val description: String = "",
    val link: String = "",
    val versionName: String = "",
    val minimumVersionCode: Long = 0L,
    val updatedAt: Long = 0L,
    val published: Boolean = false
)

private object PublicAnnouncementSource {
    private val reference = FirebaseDatabase.getInstance(
        "https://zorasports-cf452-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).getReference("announcement")

    fun listen(onData: (PublicAnnouncement?) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onData(snapshot.getValue(PublicAnnouncement::class.java))
            }
            override fun onCancelled(error: DatabaseError) = Unit
        }
        reference.addValueEventListener(listener)
        return listener
    }

    fun remove(listener: ValueEventListener) {
        reference.removeEventListener(listener)
    }
}

@Composable
private fun AnnouncementNotice() {
    val uriHandler = LocalUriHandler.current
    var announcement by remember { mutableStateOf<PublicAnnouncement?>(null) }
    var shownAt by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        val listener = PublicAnnouncementSource.listen { announcement = it }
        onDispose { PublicAnnouncementSource.remove(listener) }
    }

    val current = announcement
    if (current != null && current.published && current.updatedAt > shownAt) {
        AlertDialog(
            onDismissRequest = { shownAt = current.updatedAt },
            containerColor = Color(0xFF14283B),
            title = {
                Text("INFORMASI!", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(current.information, color = Color(0xFFEAF2F9))
                    if (current.description.isNotBlank()) {
                        Text(current.description, color = Color(0xFFB8C8D8))
                    }
                }
            },
            confirmButton = {
                if (current.link.isNotBlank()) {
                    Button(
                        onClick = {
                            shownAt = current.updatedAt
                            uriHandler.openUri(current.link)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D83C6))
                    ) { Text("KLIK LINK DI BAWAH") }
                } else {
                    Button(onClick = { shownAt = current.updatedAt }) { Text("TUTUP") }
                }
            },
            dismissButton = {
                if (current.link.isNotBlank()) {
                    OutlinedButton(onClick = { shownAt = current.updatedAt }) { Text("TUTUP") }
                }
            }
        )
    }
}

@Composable
fun UpdateAwareStreamScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val installedVersion = remember {
        runCatching {
            PackageInfoCompat.getLongVersionCode(
                context.packageManager.getPackageInfo(context.packageName, 0)
            )
        }.getOrDefault(0L)
    }
    var announcement by remember { mutableStateOf<PublicAnnouncement?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = PublicAnnouncementSource.listen {
            announcement = it
            loading = false
            failed = false
        }
        onDispose { PublicAnnouncementSource.remove(listener) }
    }

    val update = announcement
    val mustUpdate = update != null && update.published &&
        update.minimumVersionCode > installedVersion

    when {
        loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF55B9E8))
        }
        failed -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tidak dapat memeriksa versi aplikasi. Periksa koneksi internet.", color = Color.White)
        }
        mustUpdate && update != null -> Box(
            modifier = modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A1A2A), Color(0xFF142D43))))
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF6BAFD0).copy(alpha = 0.35f), RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xE91B344A))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("INFORMASI!", color = Color(0xFF82D2F5), fontWeight = FontWeight.Bold)
                    Text(
                        update.information.ifBlank { "ZORASPORTS MEMERLUKAN UPDATE VERSI BARU" },
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "UPDATE VERSI ${update.versionName} UNTUK MELIHAT STREAMING",
                        color = Color(0xFFD2E1EC),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (update.description.isNotBlank()) {
                        Text(update.description, color = Color(0xFFB7C9D8), textAlign = TextAlign.Center)
                    }
                    Button(
                        onClick = { uriHandler.openUri(update.link) },
                        enabled = update.link.startsWith("https://") || update.link.startsWith("http://"),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF328CC8))
                    ) { Text("DOWNLOAD APK TERBARU", fontWeight = FontWeight.Bold) }
                    Text("Streaming terkunci sampai APK versi terbaru terpasang.", color = Color(0xFFFFD18A), textAlign = TextAlign.Center)
                }
            }
        }
        else -> PublicStreamScreen(modifier = modifier)
    }
}
