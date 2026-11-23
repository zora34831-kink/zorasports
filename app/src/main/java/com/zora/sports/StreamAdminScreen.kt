
package com.zora.sports

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.ValueEventListener

private val AdminBg = Color(0xFF071321)
private val AdminPanel = Color(0xFF102337)
private val AdminCyan = Color(0xFF42DDF5)
private val AdminMuted = Color(0xFF91A5B8)

private val streamTypes = listOf(
    "AUTO",
    "MP4",
    "HLS",
    "DASH",
    "YOUTUBE",
    "WEBSITE"
)

private fun streamTypeLabel(type: String): String {
    return when (type) {
        "AUTO" -> "Otomatis"
        "MP4" -> "Video MP4"
        "HLS" -> "HLS / M3U8"
        "DASH" -> "MPEG-DASH / MPD"
        "YOUTUBE" -> "YouTube"
        "WEBSITE" -> "Website / HTML"
        else -> type
    }
}

@Composable
fun StreamAdminScreen(
    activity: ComponentActivity,
    onBack: () -> Unit
) {
    var signedIn by remember { mutableStateOf(AdminAuth.isAdmin()) }
    var message by remember { mutableStateOf("") }
    var links by remember { mutableStateOf(emptyList<StreamLink>()) }

    var matchName by remember { mutableStateOf("") }
    var competition by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf("AUTO") }

    var editingId by remember { mutableStateOf("") }
    var published by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<StreamLink?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        AdminAuth.signIn(activity, result.data) { success, text ->
            signedIn = success
            message = text
        }
    }

    var listener by remember {
        mutableStateOf<ValueEventListener?>(null)
    }

    DisposableEffect(signedIn) {
        if (signedIn && AdminAuth.isAdmin()) {
            listener = StreamRepository.listen(
                onData = { links = it },
                onError = { message = it }
            )
        }

        onDispose {
            listener?.let(StreamRepository::removeListener)
            listener = null
        }
    }

    fun resetForm() {
        matchName = ""
        competition = ""
        provider = ""
        url = ""
        sourceType = "AUTO"
        editingId = ""
        published = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBg)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    "ZORAADMIN",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "STREAM MANAGEMENT",
                    color = AdminCyan,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp
                )
            }

            Icon(
                Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = AdminCyan,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        if (!signedIn || !AdminAuth.isAdmin()) {
            AdminCard {
                Text(
                    "Akses administrator",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Login menggunakan akun Google administrator yang sudah terdaftar.",
                    color = AdminMuted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = {
                        launcher.launch(AdminAuth.signInIntent(activity))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminCyan,
                        contentColor = AdminBg
                    )
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("LOGIN DENGAN GOOGLE")
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        AdminAuth.ADMIN_EMAIL,
                        color = AdminCyan,
                        fontSize = 12.sp
                    )
                    Text(
                        "Administrator aktif",
                        color = AdminMuted,
                        fontSize = 10.sp
                    )
                }

                TextButton(
                    onClick = {
                        AdminAuth.signOut()
                        signedIn = false
                        links = emptyList()
                        resetForm()
                        message = "Admin logout"
                    }
                ) {
                    Text("LOGOUT", color = Color(0xFFFF8B8B))
                }
            }

            Spacer(Modifier.height(12.dp))

            AdminCard {
                Text(
                    if (editingId.isBlank()) "Tambah streaming"
                    else "Edit streaming",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(14.dp))

                AdminInput(
                    value = matchName,
                    onValueChange = { matchName = it },
                    label = "Nama pertandingan"
                )

                AdminInput(
                    value = competition,
                    onValueChange = { competition = it },
                    label = "Kompetisi / liga"
                )

                AdminInput(
                    value = provider,
                    onValueChange = { provider = it },
                    label = "Nama penyedia streaming"
                )

                Text(
                    "JENIS SUMBER STREAMING",
                    color = AdminMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Box {
                    OutlinedButton(
                        onClick = { showTypeMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            streamTypeLabel(sourceType),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih jenis sumber"
                        )
                    }

                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        streamTypes.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(streamTypeLabel(type))
                                },
                                onClick = {
                                    sourceType = type
                                    showTypeMenu = false
                                },
                                trailingIcon = {
                                    if (sourceType == type) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = AdminCyan
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                AdminInput(
                    value = url,
                    onValueChange = { url = it },
                    label = "URL streaming resmi"
                )

                Text(
                    "URL hanya ditampilkan di panel admin.",
                    color = AdminMuted,
                    fontSize = 10.sp
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = published,
                        onCheckedChange = { published = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AdminCyan
                        )
                    )

                    Text(
                        "Publikasikan ke pengguna",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    enabled = !saving,
                    onClick = {
                        val cleanUrl = url.trim()
                        val validUrl = cleanUrl.startsWith("https://") ||
                            cleanUrl.startsWith("http://")

                        if (
                            matchName.isBlank() ||
                            provider.isBlank() ||
                            !validUrl
                        ) {
                            message =
                                "Isi nama pertandingan, penyedia, dan URL HTTP/HTTPS yang valid."
                        } else {
                            saving = true

                            StreamRepository.save(
                                StreamLink(
                                    id = editingId,
                                    matchName = matchName.trim(),
                                    competition = competition.trim(),
                                    provider = provider.trim(),
                                    url = cleanUrl,
                                    sourceType = sourceType,
                                    published = published
                                )
                            ) { success, resultMessage ->
                                saving = false
                                message = resultMessage

                                if (success) {
                                    resetForm()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminCyan,
                        contentColor = AdminBg
                    )
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AdminBg,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        if (editingId.isBlank()) "SIMPAN STREAMING"
                        else "UPDATE STREAMING"
                    )
                }

                if (editingId.isNotBlank()) {
                    TextButton(
                        onClick = { resetForm() }
                    ) {
                        Text("BATAL EDIT", color = AdminMuted)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "DAFTAR STREAMING (${links.size})",
                color = AdminCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(links, key = { it.id }) { item ->
                    AdminCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.matchName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "${item.competition} • ${item.provider}",
                                    color = AdminMuted,
                                    fontSize = 10.sp
                                )

                                Text(
                                    "Tipe: ${streamTypeLabel(item.sourceType)}",
                                    color = AdminCyan,
                                    fontSize = 10.sp
                                )

                                Text(
                                    if (item.published) "TERPUBLISH" else "DRAF",
                                    color = if (item.published) AdminCyan else AdminMuted,
                                    fontSize = 9.sp
                                )

                                Text(
                                    item.url,
                                    color = AdminMuted,
                                    fontSize = 9.sp,
                                    maxLines = 2
                                )
                            }

                            IconButton(
                                onClick = {
                                    editingId = item.id
                                    matchName = item.matchName
                                    competition = item.competition
                                    provider = item.provider
                                    url = item.url
                                    sourceType = item.sourceType
                                    published = item.published
                                    message = "Mode edit: ${item.matchName}"
                                }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = AdminCyan
                                )
                            }

                            IconButton(
                                onClick = {
                                    confirmDelete = item
                                }
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Hapus",
                                    tint = Color(0xFFFF8B8B)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(message, color = AdminCyan, fontSize = 11.sp)
        }
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = {
                Text("Hapus streaming?")
            },
            text = {
                Text("Hapus data ${item.matchName} dari daftar streaming?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        StreamRepository.delete(item.id) { success, text ->
                            message = text
                            if (success && editingId == item.id) {
                                resetForm()
                            }
                        }
                        confirmDelete = null
                    }
                ) {
                    Text(
                        "HAPUS",
                        color = Color(0xFFFF8B8B)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDelete = null }
                ) {
                    Text("BATAL")
                }
            }
        )
    }
}

@Composable
private fun AdminCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AdminPanel, RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun AdminInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        singleLine = true,
        shape = RoundedCornerShape(13.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = AdminCyan,
            unfocusedBorderColor = AdminMuted,
            focusedLabelColor = AdminCyan,
            unfocusedLabelColor = AdminMuted,
            cursorColor = AdminCyan
        )
    )
}
