package com.example.myaiapp.chat.presentation.state

import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.domain.model.LlmState
import com.example.myaiapp.chat.presentation.state.ChatCommand.CallLlm
import com.example.myaiapp.chat.presentation.state.ChatCommand.CallLlmToMCP
import com.example.myaiapp.chat.presentation.ui_model.MessageUiModel
import com.example.myaiapp.chat.presentation.ui_model.PrBriefUiModel
import com.example.myaiapp.core.Reducer
import com.example.myaiapp.core.Result
import com.example.myaiapp.utils.ImmutableList
import javax.inject.Inject

class ChatReducer @Inject constructor(
    state: ChatState
) : Reducer<ChatEvents, ChatState, ChatCommand>(state) {

    override fun reduce(event: ChatEvents, state: ChatState): Result<ChatCommand> {
        return when (event) {
            is ChatEvents.Internal.ErrorLoading -> {
                setState(
                    state.copy(error = event.error.message, loading = false, isEmptyState = false)
                )
                Result(null)
            }

            is ChatEvents.Ui.CallLlm -> {
                val assistantPlaceholder = MessageUiModel(
                    role = Role.ASSISTANT,
                    content = LlmState.THINKS.state,
                    isOwnMessage = false,
                    pending = true,
                )

                val userMessage = MessageUiModel(
                    role = Role.USER,
                    content = event.content,
                    isOwnMessage = true,
                )

                val newHistory = mutableListOf<MessageUiModel>()
                state.history.list.onEach {
                    newHistory.add(it)
                }
                newHistory.add(userMessage)
                newHistory.add(assistantPlaceholder)

                setState(
                    state.copy(
                        history = ImmutableList(newHistory),
                        loading = true,
                        error = null,
                        isEmptyState = false,
                        typedText = null
                    )
                )
                val command = CallLlm(
                    event.history,
                    event.content,
                    event.model,
                    event.rawHistory
                )
                Result(command)
            }

            is ChatEvents.Ui.Typing -> {
                setState(
                    state.copy(typedText = event.text)
                )
                Result(null)
            }

            is ChatEvents.Internal.MessageLoaded -> {
                val idx = state.history.list.indexOfLast { it.role == Role.ASSISTANT && it.pending }
                val newHistory = if (idx >= 0) {
                    state.history.list.toMutableList().apply {
                        this[idx] = this[idx].copy(response = null, pending = false, content = event.message, verify = null)
                    }
                } else state.history.list

                val newState = state.copy(
                    history = ImmutableList(newHistory),
                    loading = false,
                )
                setState(newState)
                Result(null)
            }

            is ChatEvents.Internal.SummeryAndReviewLoaded -> {
                val idx = state.history.list.indexOfLast { it.role == Role.ASSISTANT && it.pending }
                val newHistory = if (idx >= 0) {
                    state.history.list.toMutableList().apply {
                        this[idx] = this[idx].copy(response = event.summary, pending = false, verify = event.verify, content = null)
                    }
                } else state.history.list

                val newState = state.copy(
                    history = ImmutableList(newHistory),
                    loading = false,
                )
                setState(newState)
                Result(null)
            }

            is ChatEvents.Ui.CallLlmToMCP -> {
                val assistantPlaceholder = MessageUiModel(
                    role = Role.ASSISTANT,
                    content = LlmState.THINKS.state,
                    isOwnMessage = false,
                    pending = true,
                )

                val userMessage = MessageUiModel(
                    role = Role.USER,
                    content = event.content,
                    isOwnMessage = true,
                )

                val newHistory = mutableListOf<MessageUiModel>()
                state.history.list.onEach {
                    newHistory.add(it)
                }
                newHistory.add(userMessage)
                newHistory.add(assistantPlaceholder)

                setState(
                    state.copy(
                        history = ImmutableList(newHistory),
                        loading = true,
                        error = null,
                        isEmptyState = false,
                        typedText = null
                    )
                )
                val command = CallLlmToMCP(
                    event.history,
                    event.content,
                    event.model,
                    event.rawHistory
                )
                Result(command)
            }

            is ChatEvents.Internal.MCPResponse -> {
                val idx = state.history.list.indexOfLast { it.role == Role.ASSISTANT && it.pending }
                val newHistory = if (idx >= 0) {
                    state.history.list.toMutableList().apply {
                        this[idx] = this[idx].copy(response = null, pending = false, content = event.response, verify = null)
                    }
                } else state.history.list

                val newState = state.copy(
                    history = ImmutableList(newHistory),
                    loading = false,
                )
                setState(newState)
                Result(null)
            }

            is ChatEvents.Internal.MCPResponseGitHubPr -> {
                val idx = state.history.list.indexOfLast { it.role == Role.ASSISTANT && it.pending }
                val newHistory = if (idx >= 0) {
                    state.history.list.toMutableList().apply {
                        if (idx == state.history.list.lastIndex) {
                            this[idx] = this[idx].copy(response = null, pending = false, content = null, verify = null, prsBrief = PrBriefUiModel(event.prBrief))
                        }
                    }
                } else state.history.list

                val newState = state.copy(
                    history = ImmutableList(newHistory),
                    loading = false,
                )
                setState(newState)
                Result(null)
            }

            is ChatEvents.Ui.CallLlmToMCPGitHubPr -> {
                val assistantPlaceholder = MessageUiModel(
                    role = Role.ASSISTANT,
                    content = LlmState.THINKS.state,
                    isOwnMessage = false,
                    pending = true,
                )

                val userMessage = MessageUiModel(
                    role = Role.USER,
                    content = event.content,
                    isOwnMessage = true,
                )

                val newHistory = mutableListOf<MessageUiModel>()
                state.history.list.onEach {
                    newHistory.add(it)
                }
                newHistory.add(userMessage)
                newHistory.add(assistantPlaceholder)

                setState(
                    state.copy(
                        history = ImmutableList(newHistory),
                        loading = true,
                        error = null,
                        isEmptyState = false,
                        typedText = null
                    )
                )
                val command = ChatCommand.CallLlmToMCPGitHubPr(
                    event.history,
                    event.content,
                    event.model,
                    event.rawHistory
                )
                Result(command)
            }
        }
    }
}