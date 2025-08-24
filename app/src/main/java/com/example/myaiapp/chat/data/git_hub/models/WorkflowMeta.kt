package com.example.myaiapp.chat.data.git_hub.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkflowMeta(
    val id: Long,
    val name: String,
    val path: String,
    val state: String,
)
