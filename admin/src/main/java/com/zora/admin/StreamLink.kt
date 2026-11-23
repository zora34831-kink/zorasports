
package com.zora.admin

data class StreamLink(
    val id: String = "",
    val matchName: String = "",
    val competition: String = "",
    val provider: String = "",
    val url: String = "",
    val published: Boolean = false,
    val updatedAt: Long = 0L
)
