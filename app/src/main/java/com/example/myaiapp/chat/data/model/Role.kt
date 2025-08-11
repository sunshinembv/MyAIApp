package com.example.myaiapp.chat.data.model

import com.squareup.moshi.Json

enum class Role {
    @Json(name = "user")
    USER,

    @Json(name = "assistant")
    ASSISTANT,

    @Json(name = "system")
    SYSTEM
}
