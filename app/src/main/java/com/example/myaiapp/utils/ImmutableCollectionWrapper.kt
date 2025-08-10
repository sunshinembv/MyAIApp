package com.example.myaiapp.utils

import androidx.compose.runtime.Immutable

@Immutable
data class ImmutableList<T>(
    val list: List<T>
)