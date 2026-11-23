package com.zora.sports

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object StreamRepository {

    private val reference = FirebaseDatabase
        .getInstance(
            "https://zorasports-cf452-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        .getReference("streamLinks")

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

        val data = link.copy(
            id = key,
            updatedAt = System.currentTimeMillis()
        )

        reference.child(key)
            .setValue(data)
            .addOnSuccessListener {
                onResult(true, "Link berhasil disimpan")
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Gagal menyimpan link")
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
                onResult(false, it.message ?: "Gagal menghapus link")
            }
    }
}
