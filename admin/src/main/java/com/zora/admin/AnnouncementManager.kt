package com.zora.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase

private data class AdminAnnouncement(
    var information: String = "",
    var description: String = "",
    var link: String = "",
    var versionName: String = "",
    var minimumVersionCode: Long = 0L,
    var updatedAt: Long = 0L,
    var published: Boolean = false
)

private object AnnouncementAdminRepository {
    private val ref = FirebaseDatabase.getInstance(
        "https://zorasports-cf452-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).getReference("announcement")

    fun publish(info: String, description: String, link: String, versionName: String, versionCode: Long, done: (Boolean, String) -> Unit) {
        val data = AdminAnnouncement(
            information = info.trim(),
            description = description.trim(),
            link = link.trim(),
            versionName = versionName.trim(),
            minimumVersionCode = versionCode,
            updatedAt = System.currentTimeMillis(),
            published = true
        )
        ref.setValue(data)
            .addOnSuccessListener { done(true, "Informasi berhasil dikirim ke pengguna ZORASPORTS.") }
            .addOnFailureListener { done(false, it.localizedMessage ?: "Gagal mengirim informasi.") }
    }
}

@Composable
fun AnnouncementManager() {
    var expanded by remember { mutableStateOf(true) }
    var information by remember { mutableStateOf("ZORASPORTS MEMERLUKAN UPDATE VERSI BARU AGAR BISA MENGAKSES STREAMING") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("https://linkdownloadterbaru") }
    var versionName by remember { mutableStateOf("1.4.0") }
    var versionCode by remember { mutableStateOf("6") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("INFORMASI PENGGUNA", color = Color(0xFF174A91), style = MaterialTheme.typography.titleMedium)
                    Text("Kirim pengumuman ke aplikasi ZORASPORTS", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Tutup" else "Buka")
                }
            }
            if (expanded) {
                OutlinedTextField(
                    value = information,
                    onValueChange = { information = it },
                    label = { Text("INFORMASI") },
                    placeholder = { Text("Isi informasi untuk seluruh pengguna") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("KETERANGAN") },
                    placeholder = { Text("Isi keterangan tambahan") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = versionName,
                    onValueChange = { versionName = it },
                    label = { Text("VERSI TERBARU (contoh 1.4.0)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = versionCode,
                    onValueChange = { versionCode = it.filter(Char::isDigit) },
                    label = { Text("VERSION CODE MINIMUM") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("LINK UPDATE") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (information.isBlank()) {
                            message = "Isi informasi terlebih dahulu."
                            isError = true
                        } else if (versionName.isBlank() || versionCode.toLongOrNull() == null || versionCode.toLong() <= 0L) {
                            message = "Isi versi dan version code yang valid."
                            isError = true
                        } else if (link.isBlank() || (!link.trim().startsWith("https://") && !link.trim().startsWith("http://"))) {
                            message = "Link harus diawali https:// atau http://"
                            isError = true
                        } else {
                            busy = true
                            message = "Mengirim informasi..."
                            isError = false
                            AnnouncementAdminRepository.publish(information, description, link, versionName, versionCode.toLong()) { success, result ->
                                busy = false
                                message = result
                                isError = !success
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(if (busy) "MENGIRIM..." else "KIRIM INFORMASI KE PENGGUNA")
                }
                if (message.isNotBlank()) {
                    Text(message, color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF16834A))
                }
                Text(
                    "Pengguna dengan version code di bawah batas minimum tidak dapat membuka Streaming sampai memasang APK terbaru.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
