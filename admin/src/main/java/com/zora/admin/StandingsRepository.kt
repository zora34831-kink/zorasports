
package com.zora.admin

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class StandingsTeam(
    val id: String = "",
    val teamName: String = "",
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0
) {
    val goalDifference: Int
        get() = goalsFor - goalsAgainst

    val points: Int
        get() = won * 3 + drawn
}

object StandingsRepository {

    private val reference = FirebaseDatabase
        .getInstance(
            "https://zorasports-cf452-default-rtdb.asia-southeast1.firebasedatabase.app/"
        )
        .getReference("standings")

    fun listen(
        leagueId: String,
        onData: (List<StandingsTeam>) -> Unit,
        onError: (String) -> Unit
    ): ValueEventListener {
        val leagueReference = reference.child(leagueId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val teams = snapshot.children.mapNotNull { child ->
                    child.getValue(StandingsTeam::class.java)
                        ?.copy(id = child.key ?: "")
                }

                onData(
                    teams.sortedWith(
                        compareByDescending<StandingsTeam> { it.points }
                            .thenByDescending { it.goalDifference }
                            .thenByDescending { it.goalsFor }
                            .thenBy { it.teamName.lowercase() }
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        leagueReference.addValueEventListener(listener)
        return listener
    }

    fun removeListener(
        leagueId: String,
        listener: ValueEventListener
    ) {
        reference.child(leagueId).removeEventListener(listener)
    }

    fun save(
        leagueId: String,
        team: StandingsTeam,
        onResult: (Boolean, String) -> Unit
    ) {
        if (leagueId.isBlank() || team.teamName.isBlank()) {
            onResult(false, "Nama liga dan tim wajib diisi")
            return
        }

        val key = team.id.ifBlank {
            reference.child(leagueId).push().key.orEmpty()
        }

        if (key.isBlank()) {
            onResult(false, "Gagal membuat ID tim")
            return
        }

        val cleanTeam = team.copy(
            id = key,
            teamName = team.teamName.trim(),
            played = team.played.coerceAtLeast(0),
            won = team.won.coerceAtLeast(0),
            drawn = team.drawn.coerceAtLeast(0),
            lost = team.lost.coerceAtLeast(0),
            goalsFor = team.goalsFor.coerceAtLeast(0),
            goalsAgainst = team.goalsAgainst.coerceAtLeast(0)
        )

        reference.child(leagueId).child(key)
            .setValue(cleanTeam)
            .addOnSuccessListener {
                onResult(true, "Data tim berhasil disimpan")
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Gagal menyimpan tim")
            }
    }

    fun delete(
        leagueId: String,
        teamId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (leagueId.isBlank() || teamId.isBlank()) {
            onResult(false, "ID liga atau tim tidak valid")
            return
        }

        reference.child(leagueId).child(teamId)
            .removeValue()
            .addOnSuccessListener {
                onResult(true, "Tim berhasil dihapus")
            }
            .addOnFailureListener {
                onResult(false, it.message ?: "Gagal menghapus tim")
            }
    }
}
