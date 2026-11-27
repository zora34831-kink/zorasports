
package com.zora.admin

import android.net.Uri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

object StreamRepository {

    private val reference = FirebaseDatabase
        .getInstance(
            "https://zorasports-cf452-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        .getReference("streamLinks")

    // Mengikuti bucket default yang dikonfigurasi di google-services.json.
    private val storage = FirebaseStorage.getInstance()

    fun listen(
        onData: (List<StreamLink>) -> Unit,
        onError: (String) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val links = snapshot.children.mapNotNull { child ->
                    child.getValue(StreamLink::class.java)?.copy(
                        id = child.key ?: ""
                    )
                }

                onData(links.sortedByDescending { it.updatedAt })
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        reference.addValueEventListener(listener)
        return listener
    }

    fun removeListener(listener: ValueEventListener) {
        reference.removeEventListener(listener)
    }

    fun save(
        link: StreamLink,
        onResult: (Boolean, String) -> Unit
    ) {
        val key = link.id.ifBlank {
            reference.push().key.orEmpty()
        }

        if (key.isBlank()) {
            onResult(false, "Gagal membuat ID data")
            return
        }

        reference.child(key).get()
            .addOnSuccessListener { snapshot ->

                val previous = snapshot.getValue(StreamLink::class.java)
                val now = System.currentTimeMillis()

                val liveStartedAt = when {
                    !link.published -> 0L

                    previous?.published == true ->
                        previous.liveStartedAt.takeIf { it > 0L }
                            ?: link.liveStartedAt.takeIf { it > 0L }
                            ?: now

                    else ->
                        link.liveStartedAt.takeIf { it > 0L } ?: now
                }

                val finalDesign = link.design.copy(
                    designUpdatedAt = now
                )

                val data = link.copy(
                    id = key,
                    updatedAt = now,
                    liveStartedAt = liveStartedAt,
                    liveEnded = previous?.liveEnded ?: link.liveEnded,
                    design = finalDesign
                )

                reference.child(key).setValue(data)
                    .addOnSuccessListener {
                        onResult(
                            true,
                            "Streaming berhasil disimpan"
                        )
                    }
                    .addOnFailureListener {
                        onResult(
                            false,
                            it.message ?: "Gagal menyimpan streaming"
                        )
                    }
            }
            .addOnFailureListener {
                onResult(
                    false,
                    it.message ?: "Gagal membaca data streaming"
                )
            }
    }

    fun setLiveStatus(
        streamId: String,
        isLive: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        if (streamId.isBlank()) {
            onResult(false, "ID channel tidak valid")
            return
        }
        val updates = mapOf<String, Any>(
            "liveEnded" to !isLive,
            "liveStartedAt" to if (isLive) System.currentTimeMillis() else 0L,
            "updatedAt" to System.currentTimeMillis()
        )
        reference.child(streamId).updateChildren(updates)
            .addOnSuccessListener {
                onResult(true, if (isLive) "Channel sekarang LIVE" else "Channel selesai live")
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Gagal mengubah status live")
            }
    }

    fun saveDesign(
        streamId: String,
        design: StreamDesign,
        onResult: (Boolean, String) -> Unit
    ) {
        if (streamId.isBlank()) {
            onResult(false, "ID streaming tidak valid")
            return
        }

        val updatedDesign = design.copy(
            designUpdatedAt = System.currentTimeMillis()
        )

        reference.child(streamId)
            .child("design")
            .setValue(updatedDesign)
            .addOnSuccessListener {
                onResult(true, "Desain berhasil disimpan")
            }
            .addOnFailureListener {
                onResult(
                    false,
                    it.message ?: "Gagal menyimpan desain"
                )
            }
    }

    fun uploadOverlayImage(
        streamId: String,
        imageUri: Uri,
        onResult: (Boolean, String) -> Unit
    ) {
        if (streamId.isBlank()) {
            onResult(false, "Simpan data streaming terlebih dahulu")
            return
        }

        val fileName = "${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference
            .child("streamOverlays/$streamId/$fileName")

        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        imageRef.putFile(imageUri, metadata)
            .addOnSuccessListener { snapshot ->
                snapshot.storage.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onResult(true, downloadUri.toString())
                    }
                    .addOnFailureListener { error ->
                        onResult(
                            false,
                            "Foto terunggah, tetapi URL gagal dibuat: ${error.localizedMessage ?: error.javaClass.simpleName}"
                        )
                    }
            }
            .addOnFailureListener { error ->
                val detail = error.localizedMessage ?: error.javaClass.simpleName
                onResult(false, "Upload Storage gagal: $detail")
            }
    }

    fun delete(
        id: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (id.isBlank()) {
            onResult(false, "ID link tidak valid")
            return
        }

        reference.child(id)
            .removeValue()
            .addOnSuccessListener {
                onResult(true, "Link berhasil dihapus")
            }
            .addOnFailureListener {
                onResult(
                    false,
                    it.message ?: "Gagal menghapus link"
                )
            }
    }
}
