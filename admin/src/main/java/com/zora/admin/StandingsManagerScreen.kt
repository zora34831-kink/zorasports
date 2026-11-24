
package com.zora.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.database.ValueEventListener

@Composable
fun StandingsManagerScreen(
    onBack: () -> Unit
) {
    var leagueInput by remember { mutableStateOf("liga_1") }
    var activeLeagueId by remember { mutableStateOf("liga_1") }

    var teams by remember { mutableStateOf(emptyList<StandingsTeam>()) }
    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    var editingTeam by remember { mutableStateOf<StandingsTeam?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    DisposableEffect(activeLeagueId) {
        var listener: ValueEventListener? = null

        if (activeLeagueId.isNotBlank()) {
            listener = StandingsRepository.listen(
                leagueId = activeLeagueId,
                onData = {
                    teams = it
                    error = ""
                },
                onError = {
                    error = it
                }
            )
        }

        onDispose {
            listener?.let {
                StandingsRepository.removeListener(activeLeagueId, it)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Kembali")
            }

            Spacer(Modifier.weight(1f))

            Text(
                "ZORAADMIN",
                color = Color(0xFF174A91),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "MANAJEMEN KLASEMEN",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF174A91)
        )

        Text(
            "Kelola data tim dan statistik liga.",
            color = Color.Gray
        )

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "ID LIGA",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = leagueInput,
                    onValueChange = { leagueInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Contoh: liga_1") }
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val cleanId = leagueInput.trim()
                            .lowercase()
                            .replace(" ", "_")

                        val validId = cleanId.isNotBlank() &&
                            cleanId.none { it in ".#$[]/" }

                        if (validId) {
                            activeLeagueId = cleanId
                            leagueInput = cleanId
                            message = "Liga dibuka: $cleanId"
                            error = ""
                        } else {
                            error = "ID liga tidak valid."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Buka liga")
                }

                Text(
                    "Liga aktif: $activeLeagueId",
                    color = Color(0xFF16834A),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total tim", color = Color.Gray)
                Text(
                    teams.size.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    editingTeam = null
                    showEditor = true
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+ Tambah tim")
            }
        }

        if (message.isNotBlank()) {
            Text(
                message,
                color = Color(0xFF16834A),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "DAFTAR TIM",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(Modifier.height(8.dp))

        if (teams.isEmpty()) {
            Text(
                "Belum ada data tim untuk liga ini.",
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(teams, key = { it.id }) { team ->
                    StandingTeamCard(
                        team = team,
                        onEdit = {
                            editingTeam = team
                            showEditor = true
                        },
                        onDelete = {
                            StandingsRepository.delete(
                                activeLeagueId,
                                team.id
                            ) { success, result ->
                                message = result
                                if (!success) error = result
                            }
                        }
                    )
                }
            }
        }
    }

    if (showEditor) {
        StandingTeamEditor(
            initial = editingTeam,
            onDismiss = { showEditor = false },
            onSave = { team ->
                StandingsRepository.save(
                    leagueId = activeLeagueId,
                    team = team
                ) { success, result ->
                    message = result
                    error = if (success) "" else result
                }
                showEditor = false
            }
        )
    }
}

@Composable
private fun StandingTeamCard(
    team: StandingsTeam,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        team.teamName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${team.played} pertandingan • ${team.points} poin",
                        color = Color(0xFF174A91),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    "SG ${team.goalDifference}",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "M ${team.won}   S ${team.drawn}   K ${team.lost}",
                color = Color.Gray
            )

            Text(
                "Gol: ${team.goalsFor} - ${team.goalsAgainst}",
                color = Color.Gray
            )

            Row {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }

                TextButton(onClick = onDelete) {
                    Text(
                        "Hapus",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun StandingTeamEditor(
    initial: StandingsTeam?,
    onDismiss: () -> Unit,
    onSave: (StandingsTeam) -> Unit
) {
    var teamName by remember(initial?.id) {
        mutableStateOf(initial?.teamName ?: "")
    }

    var won by remember(initial?.id) {
        mutableStateOf((initial?.won ?: 0).toString())
    }

    var drawn by remember(initial?.id) {
        mutableStateOf((initial?.drawn ?: 0).toString())
    }

    var lost by remember(initial?.id) {
        mutableStateOf((initial?.lost ?: 0).toString())
    }

    var goalsFor by remember(initial?.id) {
        mutableStateOf((initial?.goalsFor ?: 0).toString())
    }

    var goalsAgainst by remember(initial?.id) {
        mutableStateOf((initial?.goalsAgainst ?: 0).toString())
    }

    var error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    if (initial == null) "Tambah tim" else "Edit tim",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Nama tim") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    "HASIL PERTANDINGAN",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )

                NumberField("Menang", won) { won = it }
                NumberField("Seri", drawn) { drawn = it }
                NumberField("Kalah", lost) { lost = it }

                Text(
                    "STATISTIK GOL",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )

                NumberField("Gol memasukkan", goalsFor) {
                    goalsFor = it
                }

                NumberField("Gol kemasukan", goalsAgainst) {
                    goalsAgainst = it
                }

                val w = won.toIntOrNull() ?: 0
                val d = drawn.toIntOrNull() ?: 0
                val l = lost.toIntOrNull() ?: 0
                val gf = goalsFor.toIntOrNull() ?: 0
                val ga = goalsAgainst.toIntOrNull() ?: 0

                val played = w + d + l
                val points = w * 3 + d
                val difference = gf - ga

                Spacer(Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEAF1FA)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("HASIL OTOMATIS", fontWeight = FontWeight.Bold)
                        Text("Main: $played pertandingan")
                        Text("Selisih gol: $difference")
                        Text("Poin: $points")
                    }
                }

                if (error.isNotBlank()) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val values = listOf(won, drawn, lost, goalsFor, goalsAgainst)
                            val validNumbers = values.all {
                                val number = it.toIntOrNull()
                                number != null && number >= 0
                            }

                            if (teamName.isBlank()) {
                                error = "Nama tim wajib diisi."
                            } else if (!validNumbers) {
                                error = "Statistik harus berupa angka nol atau lebih."
                            } else {
                                onSave(
                                    StandingsTeam(
                                        id = initial?.id ?: "",
                                        teamName = teamName.trim(),
                                        played = played,
                                        won = w,
                                        drawn = d,
                                        lost = l,
                                        goalsFor = gf,
                                        goalsAgainst = ga
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

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.all { it.isDigit() } && input.length <= 6) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )
}
