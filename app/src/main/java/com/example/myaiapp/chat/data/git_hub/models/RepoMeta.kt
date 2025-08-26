package com.example.myaiapp.chat.data.git_hub.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RepoMeta(
    @Json(name = "default_branch")
    val defaultBranch: String,
)
