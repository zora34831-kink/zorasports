package com.zora.admin

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
    val liveStartedAt: Long = 0L
)
