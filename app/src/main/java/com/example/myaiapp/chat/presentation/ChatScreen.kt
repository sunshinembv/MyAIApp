package com.example.myaiapp.chat.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myaiapp.R
import com.example.myaiapp.chat.domain.model.LlmState
import com.example.myaiapp.chat.presentation.preview_data.ChatStatePreviewParameterProvider
import com.example.myaiapp.chat.presentation.state.ChatEvents
import com.example.myaiapp.chat.presentation.state.ChatState
import com.example.myaiapp.chat.presentation.ui_model.MessageUiModel
import com.example.myaiapp.ui.components.LlmMessage
import com.example.myaiapp.ui.components.OwnMessage
import com.example.myaiapp.ui.components.SendMessageTextField
import com.example.myaiapp.ui.components.basic.AppTopAppBar
import com.example.myaiapp.ui.components.basic.AppTopAppBarIconItem
import com.example.myaiapp.ui.theme.MyAIAppTheme
import com.example.myaiapp.utils.ImmutableList

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    popBackStack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChatScreen(
        state,
        popBackStack,
        viewModel::obtainEvent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatScreen(
    chatState: ChatState,
    popBackStack: () -> Unit,
    obtainEvent: (ChatEvents.Ui) -> Unit,
    modifier: Modifier = Modifier,
) {

    val navigationIcon = AppTopAppBarIconItem(
        iconId = R.drawable.baseline_arrow_back_24,
    ) {
        popBackStack.invoke()
    }

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = "Ollama",
                navigationIcon = navigationIcon
            )
        },
        bottomBar = {
            SendMessageTextField(
                value = chatState.typedText.orEmpty(),
                onTextChange = { text ->
                    obtainEvent(
                        ChatEvents.Ui.Typing(
                            text
                        )
                    )
                },
                onSend = {
                    obtainEvent(
                        ChatEvents.Ui.CallLlm(
                            history = chatState.history,
                            content = chatState.typedText.orEmpty(),
                            model = chatState.model.modelName
                        )
                    )
                }
            )
        },
        backgroundColor = MyAIAppTheme.colors.chatBackgroundColor,
        content = { padding ->
            if (chatState.error == null) {
                MessageList(
                    messages = ImmutableList(
                        chatState.history.list.reversed()
                    ),
                    pending = chatState.loading,
                    modifier = modifier.padding(padding)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(chatState.error)
                }
            }
        }
    )
}

@Composable
fun MessageList(
    messages: ImmutableList<MessageUiModel>,
    pending: Boolean,
    modifier: Modifier = Modifier
) {
    if (messages.list.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "How can I help?",
                color = MyAIAppTheme.colors.contentColor,
                style = MyAIAppTheme.typography.heading1
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.indent_16dp)),
            contentPadding = PaddingValues(dimensionResource(id = R.dimen.indent_16dp)),
            reverseLayout = true
        ) {
            items(messages.list.size) { index ->
                val message = messages.list[index]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.isOwnMessage)
                        Arrangement.End else
                        Arrangement.Start
                ) {
                    if (message.isOwnMessage) {
                        OwnMessage(
                            text = message.content,
                            modifier = Modifier.padding(start = dimensionResource(id = R.dimen.own_message_indent)),
                        )
                    } else {
                        LlmMessage(
                            text = message.content,
                            pending = pending && message.content == LlmState.THINKS.state,
                            modifier = Modifier.padding(end = dimensionResource(id = R.dimen.llm_message_indent)),
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun ChatScreenPreview(
    @PreviewParameter(ChatStatePreviewParameterProvider::class)
    chatState: ChatState,
) {
    MyAIAppTheme {
        ChatScreen(
            chatState = chatState,
            popBackStack = {},
            obtainEvent = {}
        )
    }
}