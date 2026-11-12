package com.example.exoplayerdemo

import java.io.Serializable

data class VideoItem(
    val title: String,
    val thumbnailUrl: String,
    val videoUrl: String
) : Serializable
