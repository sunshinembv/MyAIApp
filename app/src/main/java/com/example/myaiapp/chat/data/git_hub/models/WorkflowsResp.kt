package com.example.myaiapp.chat.data.git_hub.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkflowsResp(
    @Json(name = "total_count")
    val totalCount: Int,
    val workflows: List<WorkflowMeta>,
)
