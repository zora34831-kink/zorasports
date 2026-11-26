package com.zora.admin

import android.net.Uri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata

data class ApkRelease(
    var versionCode: Long = 0L,
    var versionName: String = "",
    var apkUrl: String = "",
    var notes: String = "",
    var updatedAt: Long = 0L,
    var minimumVersionCode: Long = 0L
)

object ApkReleaseRepository {
    private val database = FirebaseDatabase.getInstance(
        "https://zorasports-cf452-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).getReference("appRelease")

    private val storage = FirebaseStorage.getInstance()

    fun listen(onData: (ApkRelease?) -> Unit, onError: (String) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onData(snapshot.getValue(ApkRelease::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }
        database.addValueEventListener(listener)
        return listener
    }

    fun removeListener(listener: ValueEventListener) {
        database.removeEventListener(listener)
    }

    fun upload(uri: Uri, versionName: String, versionCode: Long, notes: String, onResult: (Boolean, String) -> Unit) {
        if (versionName.isBlank() || versionCode <= 0L) {
            onResult(false, "Versi dan versionCode wajib diisi")
            return
        }
        val file = storage.reference.child("apkReleases/zorasports-$versionName.apk")
        val metadata = StorageMetadata.Builder()
            .setContentType("application/vnd.android.package-archive")
            .build()

        file.putFile(uri, metadata)
            .addOnSuccessListener {
                file.downloadUrl.addOnSuccessListener { downloadUri ->
                    val release = ApkRelease(
                        versionCode = versionCode,
                        versionName = versionName.trim(),
                        apkUrl = downloadUri.toString(),
                        notes = notes.trim(),
                        updatedAt = System.currentTimeMillis(),
                        minimumVersionCode = versionCode
                    )
                    database.setValue(release)
                        .addOnSuccessListener { onResult(true, "APK ${versionName} berhasil dipublikasikan") }
                        .addOnFailureListener { error ->
                            onResult(false, error.localizedMessage ?: "Gagal menyimpan data versi")
                        }
                }.addOnFailureListener { error ->
                    onResult(false, error.localizedMessage ?: "Gagal mengambil URL APK")
                }
            }.addOnFailureListener { error ->
                onResult(false, error.localizedMessage ?: "Upload APK gagal")
            }
    }
}
