package com.example.myaiapp.core

data class Result<C : Command>(
    val command: C?,
)