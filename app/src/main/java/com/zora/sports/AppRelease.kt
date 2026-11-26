package com.zora.sports

data class AppRelease(
    var versionCode: Long = 0L,
    var versionName: String = "",
    var apkUrl: String = "",
    var notes: String = "",
    var updatedAt: Long = 0L,
    var minimumVersionCode: Long = 0L
)
