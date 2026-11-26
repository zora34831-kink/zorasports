package com.zora.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.database.ValueEventListener

@Composable
fun ApkReleaseManager() {
    var versionName by remember { mutableStateOf("1.4.0") }
    var versionCode by remember { mutableStateOf("6") }
    var notes by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var current by remember { mutableStateOf<ApkRelease?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedUri = uri
        if (uri != null) {
            message = "File APK dipilih. Periksa versi lalu publikasikan."
            isError = false
        }
    }

    DisposableEffect(Unit) {
        val listener: ValueEventListener = ApkReleaseRepository.listen(
            onData = { current = it },
            onError = {
                message = "Gagal membaca versi: $it"
                isError = true
            }
        )
        onDispose { ApkReleaseRepository.removeListener(listener) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("APK TERBARU", style = MaterialTheme.typography.titleMedium, color = Color(0xFF174A91))
            Text(
                current?.let { "Rilis aktif: ${it.versionName} • code ${it.versionCode}" }
                    ?: "Belum ada APK yang dipublikasikan",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            OutlinedTextField(
                value = versionName,
                onValueChange = { versionName = it.trim() },
                label = { Text("Versi aplikasi (contoh 1.4.0)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = versionCode,
                onValueChange = { value -> versionCode = value.filter(Char::isDigit) },
                label = { Text("Version code (harus lebih tinggi)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan pembaruan (opsional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = { picker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            ) {
                Text(if (selectedUri == null) "PILIH FILE APK" else "GANTI FILE APK")
            }
            selectedUri?.let {
                Text("File APK sudah dipilih", color = Color(0xFF16834A), style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    val code = versionCode.toLongOrNull()
                    val uri = selectedUri
                    if (uri == null) {
                        message = "Pilih file APK terlebih dahulu."
                        isError = true
                    } else if (code == null || code <= (current?.versionCode ?: 0L)) {
                        message = "Version code harus lebih tinggi dari rilis aktif."
                        isError = true
                    } else if (versionName.isBlank()) {
                        message = "Isi nomor versi aplikasi."
                        isError = true
                    } else {
                        busy = true
                        message = "Mengunggah APK..."
                        isError = false
                        ApkReleaseRepository.upload(uri, versionName, code, notes) { success, result ->
                            busy = false
                            message = result
                            isError = !success
                            if (success) selectedUri = null
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (busy) "MENGUNGGAH..." else "PUBLIKASIKAN APK TERBARU")
            }
            if (message.isNotBlank()) {
                Text(
                    message,
                    color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF16834A),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                "Saat dipublikasikan, versi ini menjadi batas minimum untuk membuka halaman streaming. Pastikan APK sudah berhasil dibangun dan diuji.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
