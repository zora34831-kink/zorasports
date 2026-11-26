
package com.zora.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val sourceTypes = listOf(
    "AUTO", "MP4", "HLS", "DASH", "YOUTUBE", "WEBSITE"
)

private fun formatScheduleTime(
    timestamp: Long,
    pattern: String
): String {
    val formatter = SimpleDateFormat(pattern, Locale.US)
    formatter.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
    return formatter.format(Date(timestamp))
}

private fun parseScheduleTime(
    date: String,
    time: String
): Long? {
    return try {
        val formatter = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.US
        )
        formatter.isLenient = false
        formatter.timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        formatter.parse("$date $time")?.time
    } catch (_: Exception) {
        null
    }
}

class MainActivity : ComponentActivity() {

    private var loggedIn by mutableStateOf(false)
    private var loginMessage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loggedIn = AdminAuth.isAdmin()

        val loginLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            AdminAuth.signIn(this, result.data) { success, message ->
                runOnUiThread {
                    loggedIn = success
                    loginMessage = message
                }
            }
        }

        setContent {
            MaterialTheme {
                AdminApp(
                    loggedIn = loggedIn,
                    loginMessage = loginMessage,
                    onLogin = {
                        loginLauncher.launch(
                            AdminAuth.signInIntent(this)
                        )
                    },
                    onLogout = {
                        AdminAuth.signOut()
                        loggedIn = false
                        loginMessage = "Berhasil keluar"
                    }
                )
            }
        }
    }
}

@Composable
fun AdminApp(
    loggedIn: Boolean,
    loginMessage: String,
    onLogin: () -> Unit,
    onLogout: () -> Unit
) {
    var links by remember {
        mutableStateOf(emptyList<StreamLink>())
    }

    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    var editing by remember {
        mutableStateOf<StreamLink?>(null)
    }

    var showEditor by remember {
        mutableStateOf(false)
    }

    var showStandings by remember {
        mutableStateOf(false)
    }

    var studioStream by remember {
        mutableStateOf<StreamLink?>(null)
    }

    DisposableEffect(loggedIn) {
        var listener: ValueEventListener? = null

        if (loggedIn) {
            listener = StreamRepository.listen(
                onData = { links = it },
                onError = { error = it }
            )
        }

        onDispose {
            listener?.let {
                StreamRepository.removeListener(it)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF2F5FA)
    ) {
        when {
            !loggedIn -> {
                LoginScreen(
                    message = loginMessage,
                    onLogin = onLogin
                )
            }

            studioStream != null -> {
                StudioEditorScreen(
                    stream = studioStream!!,
                    onBack = {
                        studioStream = null
                    }
                )
            }

            showStandings -> {
                StandingsManagerScreen(
                    onBack = {
                        showStandings = false
                    }
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "ZORAADMIN",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF174A91)
                            )

                            Text(
                                "STREAM MANAGEMENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }

                        TextButton(onClick = onLogout) {
                            Text("Keluar")
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total streaming")
                                Text(
                                    links.size.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Text("Dipublikasikan")
                                Text(
                                    links.count { it.published }.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            editing = null
                            showEditor = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Tambah streaming")
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            showStandings = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("MANAJEMEN KLASEMEN")
                    }

                    Spacer(Modifier.height(18.dp))

                    AnnouncementManager()


                    if (message.isNotBlank()) {
                        Text(
                            message,
                            color = Color(0xFF16834A),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (error.isNotBlank()) {
                        Text(
                            "Firebase: $error",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "DAFTAR STREAMING",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    if (links.isEmpty()) {
                        Text(
                            "Belum ada link streaming.",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            links.forEach { link ->
                                StreamCard(
                                    link = link,
                                    onEdit = {
                                        editing = link
                                        showEditor = true
                                    },
                                    onStudio = {
                                        studioStream = link
                                    },
                                    onDelete = {
                                        StreamRepository.delete(link.id) { _, msg ->
                                            message = msg
                                        }
                                    },
                                    onPublish = { value ->
                                        StreamRepository.save(link.copy(published = value)) { _, msg ->
                                            message = msg
                                        }
                                    }
                                )
                            }
                        }
                }
            }
        }
    }
    }

    if (showEditor && loggedIn && !showStandings && studioStream == null) {
        StreamEditor(
            initial = editing,
            onDismiss = {
                showEditor = false
            },
            onSave = { link ->
                StreamRepository.save(link) { success, msg ->
                    message = msg
                }
                showEditor = false
            }
        )
    }
}

@Composable
fun LoginScreen(
    message: String,
    onLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "ZORAADMIN",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF174A91)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Panel pengelolaan streaming ZORASPORTS.",
            color = Color.Gray
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Masuk dengan Google")
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Akses khusus administrator terverifikasi.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun StreamCard(
    link: StreamLink,
    onEdit: () -> Unit,
    onStudio: () -> Unit,
    onDelete: () -> Unit,
    onPublish: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Text(
                link.matchName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "${link.competition} • ${link.provider}",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "Tipe sumber: ${link.sourceType}",
                color = Color(0xFF2866B2),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (link.scheduledStartAt > 0L) {
                Text(
                    "Mulai: ${
                        formatScheduleTime(
                            link.scheduledStartAt,
                            "dd/MM/yyyy HH:mm"
                        )
                    } WIB",
                    color = Color(0xFF16834A),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Text(
                link.url,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (link.published) {
                        "Dipublikasikan"
                    } else {
                        "Disembunyikan"
                    },
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = link.published,
                    onCheckedChange = onPublish
                )
            }

            Row {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }

                TextButton(onClick = onStudio) {
                    Text(
                        "Studio Editor",
                        color = Color(0xFF174A91)
                    )
                }

                TextButton(onClick = onDelete) {
                    Text(
                        "Hapus",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (link.design.designLocked) {
                Text(
                    "DESAIN TERKUNCI",
                    color = Color(0xFF16834A),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            } else if (link.design.overlays.isNotEmpty()) {
                Text(
                    "DESAIN DRAFT",
                    color = Color(0xFFB7791F),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun StreamEditor(
    initial: StreamLink?,
    onDismiss: () -> Unit,
    onSave: (StreamLink) -> Unit
) {
    var matchName by remember(initial?.id) {
        mutableStateOf(initial?.matchName ?: "")
    }

    var competition by remember(initial?.id) {
        mutableStateOf(initial?.competition ?: "")
    }

    var provider by remember(initial?.id) {
        mutableStateOf(initial?.provider ?: "")
    }

    var information by remember(initial?.id) {
        mutableStateOf(initial?.information ?: "")
    }

    var description by remember(initial?.id) {
        mutableStateOf(initial?.description ?: "")
    }

    var url by remember(initial?.id) {
        mutableStateOf(initial?.url ?: "")
    }

    var sourceType by remember(initial?.id) {
        mutableStateOf(initial?.sourceType ?: "AUTO")
    }

    var published by remember(initial?.id) {
        mutableStateOf(initial?.published ?: false)
    }

    val initialSchedule =
        initial?.scheduledStartAt?.takeIf { it > 0L }
            ?: System.currentTimeMillis()

    var scheduleDate by remember(initial?.id) {
        mutableStateOf(
            formatScheduleTime(initialSchedule, "dd/MM/yyyy")
        )
    }

    var scheduleTime by remember(initial?.id) {
        mutableStateOf(
            formatScheduleTime(initialSchedule, "HH:mm")
        )
    }

    val initialEnd = initial?.scheduledEndAt?.takeIf { it > 0L }
        ?: (initialSchedule + 2L * 60L * 60L * 1000L)

    var endTime by remember(initial?.id) {
        mutableStateOf(formatScheduleTime(initialEnd, "HH:mm"))
    }

    var error by remember {
        mutableStateOf("")
    }

    var typeExpanded by remember {
        mutableStateOf(false)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    if (initial == null) {
                        "Tambah streaming"
                    } else {
                        "Edit streaming"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = matchName,
                    onValueChange = { matchName = it },
                    label = { Text("Nama pertandingan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = competition,
                    onValueChange = { competition = it },
                    label = { Text("Kompetisi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Penyedia resmi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = information,
                    onValueChange = { information = it },
                    label = { Text("INFORMASI") },
                    placeholder = { Text("Isi infomu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("KETERANGAN") },
                    placeholder = { Text("Isi keteranganmu") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL streaming resmi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    )
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "JADWAL MULAI LIVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )

                OutlinedTextField(
                    value = scheduleDate,
                    onValueChange = { scheduleDate = it },
                    label = { Text("Tanggal (DD/MM/YYYY)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                OutlinedTextField(
                    value = scheduleTime,
                    onValueChange = { scheduleTime = it },
                    label = { Text("Jam mulai (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii
                    )
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("Jam selesai live (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii
                    )
                )

                Text(
                    "Zona waktu: WIB (Asia/Jakarta). Contoh jam: 15:00",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "JENIS SUMBER",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )

                Box {
                    OutlinedButton(
                        onClick = {
                            typeExpanded = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            sourceType,
                            modifier = Modifier.weight(1f)
                        )
                        Text("▼")
                    }

                    DropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = {
                            typeExpanded = false
                        }
                    ) {
                        sourceTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    sourceType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Publikasikan",
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = published,
                        onCheckedChange = {
                            published = it
                        }
                    )
                }

                if (error.isNotBlank()) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val cleanUrl = url.trim()

                            val validUrl =
                                cleanUrl.startsWith("https://") ||
                                cleanUrl.startsWith("http://")

                            val scheduleTimestamp = parseScheduleTime(
                                scheduleDate.trim(),
                                scheduleTime.trim()
                            )

                            val endTimestamp = parseScheduleTime(
                                scheduleDate.trim(),
                                endTime.trim()
                            )

                            if (
                                matchName.isBlank() ||
                                competition.isBlank() ||
                                provider.isBlank() ||
                                information.isBlank() ||
                                description.isBlank() ||
                                !validUrl ||
                                scheduleTimestamp == null ||
                                endTimestamp == null ||
                                endTimestamp <= (scheduleTimestamp ?: 0L)
                            ) {
                                error =
                                    "Lengkapi informasi, keterangan, URL, tanggal, serta jam mulai dan selesai yang benar."
                            } else {
                                onSave(
                                    StreamLink(
                                        id = initial?.id ?: "",
                                        matchName = matchName.trim(),
                                        competition = competition.trim(),
                                        provider = provider.trim(),
                                        url = cleanUrl,
                                        sourceType = sourceType,
                                        published = published,
                                        updatedAt = System.currentTimeMillis(),
                                        scheduledStartAt = scheduleTimestamp,
                                        scheduledEndAt = endTimestamp,
                                        information = information.trim(),
                                        description = description.trim(),
                                        liveStartedAt =
                                            initial?.liveStartedAt ?: 0L,
                                        design =
                                            initial?.design ?: StreamDesign()
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
