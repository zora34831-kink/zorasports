
package com.zora.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.roundToInt
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioEditorScreen(
    stream: StreamLink,
    onBack: () -> Unit
) {
    val overlays = remember(stream.id) {
        mutableStateListOf<StreamOverlay>().apply {
            addAll(stream.design.overlays)
        }
    }

    var locked by remember(stream.id) {
        mutableStateOf(stream.design.designLocked)
    }

    var backgroundColor by remember(stream.id) {
        mutableStateOf(stream.design.backgroundColor)
    }

    var selectedId by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    var uploading by remember {
        mutableStateOf(false)
    }

    val density = LocalDensity.current

    fun currentDesign(lock: Boolean = locked): StreamDesign {
        return StreamDesign(
            backgroundColor = backgroundColor,
            titleColor = stream.design.titleColor,
            overlays = overlays.toList(),
            designLocked = lock,
            designUpdatedAt = System.currentTimeMillis()
        )
    }

    fun saveDesign(lock: Boolean = locked) {
        if (stream.id.isBlank()) {
            message = "Simpan data streaming terlebih dahulu"
            return
        }

        StreamRepository.saveDesign(
            streamId = stream.id,
            design = currentDesign(lock)
        ) { success, result ->
            message = result
            if (success) {
                locked = lock
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && !locked) {
            uploading = true

            StreamRepository.uploadOverlayImage(
                streamId = stream.id,
                imageUri = uri
            ) { success, result ->
                uploading = false

                if (success) {
                    overlays.add(
                        StreamOverlay(
                            id = UUID.randomUUID().toString(),
                            type = "IMAGE",
                            imageUrl = result,
                            x = 0.5f,
                            y = 0.5f,
                            scale = 1f
                        )
                    )
                    message = "Foto berhasil ditambahkan"
                } else {
                    message = result
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "ZORA STUDIO",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stream.matchName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Kembali")
                    }
                },
                actions = {
                    if (locked) {
                        Text(
                            "TERKUNCI",
                            color = Color(0xFFE0A33B),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!locked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { saveDesign() },
                        modifier = Modifier.weight(1f),
                        enabled = !uploading
                    ) {
                        Text("Simpan Draft")
                    }

                    Button(
                        onClick = {
                            saveDesign(lock = true)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uploading
                    ) {
                        Text("Kunci Desain")
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                "LIVE PREVIEW",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(
                        Color(
                            android.graphics.Color.parseColor(
                                backgroundColor
                            )
                        ),
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(14.dp)
                    )
            ) {
                val canvasWidth = constraints.maxWidth
                val canvasHeight = constraints.maxHeight

                overlays.forEach { element ->

                    key(element.id) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (element.x * canvasWidth).roundToInt(),
                                        (element.y * canvasHeight).roundToInt()
                                    )
                                }
                                .graphicsLayer {
                                    scaleX = element.scale
                                    scaleY = element.scale
                                    rotationZ = element.rotation
                                }
                                .then(
                                    if (!locked) {
                                        Modifier.pointerInput(
                                            element.id,
                                            locked
                                        ) {
                                            detectDragGestures { change, drag ->
                                                change.consume()

                                                val dx =
                                                    drag.x / canvasWidth.toFloat()

                                                val dy =
                                                    drag.y / canvasHeight.toFloat()

                                                val index = overlays.indexOfFirst {
                                                    it.id == element.id
                                                }

                                                if (index >= 0) {
                                                    val old = overlays[index]
                                                    overlays[index] = old.copy(
                                                        x = (old.x + dx).coerceIn(
                                                            0f,
                                                            0.9f
                                                        ),
                                                        y = (old.y + dy).coerceIn(
                                                            0f,
                                                            0.9f
                                                        )
                                                    )
                                                }
                                                selectedId = element.id
                                            }
                                        }
                                    } else Modifier
                                )
                                .border(
                                    width = if (
                                        selectedId == element.id && !locked
                                    ) 1.dp else 0.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(5.dp)
                        ) {
                            when (element.type) {
                                "IMAGE" -> {
                                    AsyncImage(
                                        model = element.imageUrl,
                                        contentDescription = "Overlay image",
                                        modifier = Modifier
                                            .size(90.dp)
                                    )
                                }

                                "STICKER" -> {
                                    Text(
                                        element.content,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                else -> {
                                    Text(
                                        element.content,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (overlays.isEmpty()) {
                    Text(
                        "Preview desain streaming",
                        color = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            if (uploading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Mengunggah gambar...")
            }

            if (message.isNotBlank()) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider()

            Text(
                "STUDIO TOOLS",
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (!locked) {
                            val newElement = StreamOverlay(
                                id = UUID.randomUUID().toString(),
                                type = "TEXT",
                                content = "TEKS BARU"
                            )
                            overlays.add(newElement)
                            selectedId = newElement.id
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !locked
                ) {
                    Text("Tambah Teks")
                }

                OutlinedButton(
                    onClick = {
                        if (!locked) {
                            val newElement = StreamOverlay(
                                id = UUID.randomUUID().toString(),
                                type = "STICKER",
                                content = "LIVE"
                            )
                            overlays.add(newElement)
                            selectedId = newElement.id
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !locked
                ) {
                    Text("Tambah Stiker")
                }
            }

            OutlinedButton(
                onClick = {
                    if (!locked && !uploading) {
                        imagePicker.launch("image/*")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !locked && !uploading
            ) {
                Text("Upload Foto / Logo")
            }

            HorizontalDivider()

            Text(
                "PENGATURAN ELEMEN",
                fontWeight = FontWeight.Bold
            )

            val selected = overlays.find {
                it.id == selectedId
            }

            if (selected != null) {

                Text(
                    "Elemen dipilih: ${selected.type}",
                    fontSize = 13.sp
                )

                Text("Ukuran elemen")

                Slider(
                    value = selected.scale.coerceIn(0.5f, 3f),
                    onValueChange = { value ->
                        val index = overlays.indexOfFirst {
                            it.id == selected.id
                        }
                        if (index >= 0 && !locked) {
                            overlays[index] = overlays[index].copy(
                                scale = value
                            )
                        }
                    },
                    valueRange = 0.5f..3f,
                    enabled = !locked
                )

                if (selected.type == "TEXT" ||
                    selected.type == "STICKER"
                ) {
                    var textValue by remember(selected.id) {
                        mutableStateOf(selected.content)
                    }

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { value ->
                            textValue = value
                            val index = overlays.indexOfFirst {
                                it.id == selected.id
                            }
                            if (index >= 0 && !locked) {
                                overlays[index] = overlays[index].copy(
                                    content = value
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Isi teks") },
                        enabled = !locked
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (!locked) {
                            overlays.removeAll {
                                it.id == selected.id
                            }
                            selectedId = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !locked
                ) {
                    Text("Hapus Elemen")
                }

            } else {
                Text(
                    "Pilih elemen pada preview untuk mengatur ukurannya.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Text(
                "BACKGROUND",
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    "#101820",
                    "#172554",
                    "#202020",
                    "#164E63"
                ).forEach { colorHex ->
                    val color = Color(
                        android.graphics.Color.parseColor(colorHex)
                    )

                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .border(
                                2.dp,
                                if (backgroundColor == colorHex) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                CircleShape
                            ),
                        shape = CircleShape,
                        color = color,
                        onClick = {
                            if (!locked) {
                                backgroundColor = colorHex
                            }
                        }
                    ) {}
                }
            }

            if (locked) {
                Text(
                    "Desain sudah dikunci. Elemen tidak dapat dipindahkan atau diubah melalui editor ini.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }
    }
}
