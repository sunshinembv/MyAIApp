package com.example.myaiapp.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class Reducer<E : Event, S : State, C : Command>(initialState: S) {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    fun sendEvent(event: E): Result<C> {
        return reduce(event, _state.value)
    }

    fun setState(newState: S) {
        _state.tryEmit(newState)
    }

    abstract fun reduce(event: E, state: S): Result<C>
}

interface Event

interface State

interface Command