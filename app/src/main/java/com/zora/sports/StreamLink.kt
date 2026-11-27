
package com.zora.sports

data class StreamOverlay(
    val id: String = "",
    val type: String = "TEXT",
    val content: String = "",
    val imageUrl: String = "",
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val visible: Boolean = true
)

data class StreamDesign(
    val backgroundColor: String = "#101820",
    val titleColor: String = "#FFFFFF",
    val overlays: List<StreamOverlay> = emptyList(),
    val designLocked: Boolean = false,
    val designUpdatedAt: Long = 0L
)

data class StreamLink(
    val id: String = "",
    val matchName: String = "",
    val competition: String = "",
    val provider: String = "",
    val url: String = "",
    val sourceType: String = "AUTO",
    val published: Boolean = false,
    val updatedAt: Long = 0L,
    val scheduledStartAt: Long = 0L,
    val liveStartedAt: Long = 0L,
    val scheduledEndAt: Long = 0L,
    val liveEnded: Boolean = false,
    val information: String = "",
    val description: String = "",
    val design: StreamDesign = StreamDesign()
)
