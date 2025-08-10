package com.example.myaiapp.chat.data.model

import com.squareup.moshi.Json

enum class RoleData {
    @Json(name = "user")
    USER,

    @Json(name = "assistant")
    ASSISTANT,

    @Json(name = "system")
    SYSTEM
}
