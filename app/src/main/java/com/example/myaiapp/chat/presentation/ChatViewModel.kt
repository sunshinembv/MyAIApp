package com.example.myaiapp.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myaiapp.chat.presentation.state.ChatActor
import com.example.myaiapp.chat.presentation.state.ChatEvents
import com.example.myaiapp.chat.presentation.state.ChatReducer
import com.example.myaiapp.chat.presentation.state.ChatState
import com.example.myaiapp.core.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val reducer: ChatReducer,
    private val actor: ChatActor,
) : BaseViewModel<ChatEvents, ChatState>() {

    override val state: StateFlow<ChatState>
        get() = reducer.state

    fun obtainEvent(event: ChatEvents.Ui) {
        when (event) {

            is ChatEvents.Ui.CallLlm -> {
                handleEvent(event)
            }

            is ChatEvents.Ui.Typing -> {
                handleOnlyEvent(event)
            }
        }
    }

    private fun handleEvent(event: ChatEvents) {
        viewModelScope.launch {
            val result = reducer.sendEvent(event)
            result.command?.let { actor.execute(it, ::handleEvent) }
        }
    }

    private fun handleOnlyEvent(event: ChatEvents.Ui) {
        reducer.sendEvent(event)
    }

    class Factory(
        private val reducer: ChatReducer,
        private val actor: ChatActor,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == ChatViewModel::class.java)
            return ChatViewModel(
                reducer,
                actor
            ) as T
        }
    }

    class AssistedFactory(
        private val reducer: ChatReducer,
        private val actor: ChatActor
    ) {
        fun create(): Factory {
            return Factory(reducer, actor)
        }
    }
}
