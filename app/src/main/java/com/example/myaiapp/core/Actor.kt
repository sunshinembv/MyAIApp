package com.example.myaiapp.core

interface Actor<C : Command, IE : Event> {
    suspend fun execute(command: C, onEvent: (IE) -> Unit)
}