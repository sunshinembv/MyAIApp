package com.example.myaiapp.chat.presentation.state

import com.example.myaiapp.chat.domain.model.ResponseType
import com.example.myaiapp.chat.presentation.mapper.ChatUiModelMapper
import com.example.myaiapp.chat.presentation.state.ChatCommand.CallLlm
import com.example.myaiapp.chat.presentation.state.ChatCommand.CallLlmToDocker
import com.example.myaiapp.chat.presentation.state.ChatCommand.CallLlmToMCP
import com.example.myaiapp.chat.presentation.state.ChatCommand.CallLlmToMCPGitHubPr
import com.example.myaiapp.chat.presentation.state.ChatCommand.CallLlmToOrchestrator
import com.example.myaiapp.chat.presentation.state.ChatCommand.CallLlmToReleaseApk
import com.example.myaiapp.chat.presentation.state.ChatCommand.GetHistoryFromCache
import com.example.myaiapp.chat.presentation.ui_model.item.OwnMessageItem
import com.example.myaiapp.chat.presentation.ui_model.item.UiItem
import com.example.myaiapp.chat.voice.VoiceState
import com.example.myaiapp.core.Reducer
import com.example.myaiapp.core.Result
import com.example.myaiapp.utils.ImmutableList
import javax.inject.Inject

class ChatReducer @Inject constructor(
    state: ChatState,
    private val mapper: ChatUiModelMapper,
) : Reducer<ChatEvents, ChatState, ChatCommand>(state) {

    override fun reduce(event: ChatEvents, state: ChatState): Result<ChatCommand> {
        return when (event) {
            is ChatEvents.Ui.CallLlm -> {

                val userMessage = OwnMessageItem(
                    text = event.content,
                )

                val newHistory = state.history.list + userMessage

                setState(
                    state.copy(
                        history = ImmutableList(newHistory),
                        isPending = true,
                        error = null,
                        isEmptyState = false,
                        typedText = null
                    )
                )

                val command = when (event.responseType) {
                    ResponseType.JSON -> {
                        CallLlmToOrchestrator(
                            content = event.content,
                            model = event.model
                        )
                    }

                    ResponseType.MCP -> {
                        CallLlmToMCP(
                            content = event.content,
                            model = event.model
                        )
                    }

                    ResponseType.MCP_GIT_PR -> {
                        CallLlmToMCPGitHubPr(
                            content = event.content,
                            model = event.model
                        )
                    }

                    ResponseType.DOCKER_KOTLIN,
                    ResponseType.DOCKER_KOTLIN_TEST -> {
                        CallLlmToDocker(
                            content = event.content,
                            model = event.model
                        )
                    }

                    ResponseType.RELEASE_OPS_SYSTEM_PROMPT -> {
                        CallLlmToReleaseApk(
                            content = event.content,
                            model = event.model,
                        )
                    }

                    ResponseType.STRING -> {
                        CallLlm(
                            content = event.content,
                            model = event.model,
                        )
                    }

                    ResponseType.VOICE -> {
                        CallLlm(
                            content = event.content,
                            model = event.model,
                        )
                    }
                }
                Result(command)
            }

            is ChatEvents.Ui.Typing -> {
                setState(
                    state.copy(typedText = event.text)
                )
                Result(null)
            }

            ChatEvents.Ui.GetHistoryFromCache -> {
                Result(GetHistoryFromCache)
            }

            is ChatEvents.Internal.MCPResponse -> {
                /*val idx = state.history.list.indexOfLast { it.role == Role.ASSISTANT && it.pending }
                val newHistory = if (idx >= 0) {
                    state.history.list.toMutableList().apply {
                        this[idx] = this[idx].copy(response = null, pending = false, content = event.response, verify = null)
                    }
                } else state.history.list

                val newState = state.copy(
                    history = ImmutableList(newHistory),
                    loading = false,
                )
                setState(newState)*/
                Result(null)
            }

            is ChatEvents.Internal.MCPResponseGitHubPr -> {
                val prBriefItems = event.prBrief.map { prBrief ->
                    mapper.toPrBriefItem(prBrief)
                }
                val newHistory = state.history.list + prBriefItems

                updateState(state, newHistory)
                Result(null)
            }

            is ChatEvents.Internal.DockerResponse -> {
                val runResultItem = mapper.toRunResultItem(event.runResult)
                val newHistory = state.history.list + runResultItem

                updateState(state, newHistory)
                Result(null)
            }

            is ChatEvents.Internal.ErrorLoading -> {
                setState(
                    state.copy(error = event.error.message, isPending = false, isEmptyState = false)
                )
                Result(null)
            }

            is ChatEvents.Internal.AskLoaded -> {
                val ask = mapper.fromAskToMessageItem(event.ask)
                val newHistory = state.history.list + ask

                updateState(state, newHistory)
                Result(null)
            }

            is ChatEvents.Internal.SummeryAndReviewLoaded -> {
                val summary = mapper.toSummaryItem(event.summary)
                val verify = mapper.toVerifyItem(event.verify)
                val newHistory = state.history.list + summary + verify

                updateState(state, newHistory)
                Result(null)
            }

            is ChatEvents.Internal.MessageLoaded -> {
                val message = mapper.fromStringToMessageItem(event.message)
                val newHistory = state.history.list + message

                updateState(state, newHistory)
                Result(null)
            }

            is ChatEvents.Internal.ChatHistoryLoaded -> {
                val messages = mapper.toChatMessages(event.history)
                val newHistory = state.history.list + messages
                updateState(state, newHistory)
                Result(null)
            }

            //Voice
            is ChatEvents.VoiceEvent.Failed -> {
                setState(
                    state.copy(voiceState = VoiceState.Error(event.msg))
                )
                Result(null)
            }

            is ChatEvents.VoiceEvent.FinalRecognized -> {

                val message = mapper.fromSpeechToOwnMessage(event.query)
                val newHistory = state.history.list + message

                setState(
                    state.copy(
                        isPending = true,
                        history = ImmutableList(newHistory),
                        voiceState = VoiceState.Thinking(event.query)
                    )
                )
                Result(null)
            }

            ChatEvents.VoiceEvent.Idle -> {
                setState(
                    state.copy(voiceState = VoiceState.Idle)
                )
                Result(null)
            }

            ChatEvents.VoiceEvent.ListeningStarted -> {
                setState(
                    state.copy(voiceState = VoiceState.Listening)
                )
                Result(null)
            }

            is ChatEvents.VoiceEvent.PartialUpdated -> {
                setState(
                    state.copy(voiceState = VoiceState.Recognizing(event.text))
                )
                Result(null)
            }

            is ChatEvents.VoiceEvent.Speaking -> {

                val message = mapper.fromStringToMessageItem(event.chunk)
                val newHistory = state.history.list + message

                setState(
                    state.copy(
                        isPending = false,
                        history = ImmutableList(newHistory),
                        voiceState = VoiceState.Speaking(event.chunk),
                    )
                )
                Result(null)
            }
        }
    }

    private fun updateState(state: ChatState, newHistory: List<UiItem>) {
        val newState = state.copy(
            history = ImmutableList(newHistory),
            isPending = false,
        )
        setState(newState)
    }
}
