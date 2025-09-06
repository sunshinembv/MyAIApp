package com.example.myaiapp.memory.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FactDTO(val text: String, val importance: Int)