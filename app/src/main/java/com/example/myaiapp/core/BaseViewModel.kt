package com.example.myaiapp.core

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel<E : Event, S : State> : ViewModel() {
    abstract val state: StateFlow<S>
}