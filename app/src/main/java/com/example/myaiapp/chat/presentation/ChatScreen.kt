package com.example.myaiapp.chat.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myaiapp.R
import com.example.myaiapp.chat.domain.model.LlmState
import com.example.myaiapp.chat.presentation.state.ChatEvents
import com.example.myaiapp.chat.presentation.state.ChatState
import com.example.myaiapp.chat.presentation.ui_model.item.OwnMessageItem
import com.example.myaiapp.chat.presentation.ui_model.item.PendingItem
import com.example.myaiapp.chat.presentation.ui_model.item.UiItem
import com.example.myaiapp.ui.UiItemDelegate
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
        chatState = state,
        popBackStack = popBackStack,
        obtainEvent = viewModel::obtainEvent,
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
    Scaffold(
        topBar = { ChatTopBar(onBack = popBackStack) },
        bottomBar = {
            ChatBottomBar(
                typedText = chatState.typedText.orEmpty(),
                onTextChange = { text -> obtainEvent(ChatEvents.Ui.Typing(text)) },
                onSend = {
                    obtainEvent(
                        ChatEvents.Ui.CallLlm(
                            content = chatState.typedText.orEmpty(),
                            model = chatState.model,
                            responseType = chatState.responseType
                        )
                    )
                }
            )
        },
        backgroundColor = MyAIAppTheme.colors.chatBackgroundColor
    ) { padding ->
        ChatContent(
            chatState = chatState,
            modifier = modifier.padding(padding)
        )
    }
}

@Composable
fun MessageList(
    items: ImmutableList<UiItem>,
    isPending: Boolean,
    modifier: Modifier = Modifier
) {
    if (items.list.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "How can I help?",
                color = MyAIAppTheme.colors.contentColor,
                style = MyAIAppTheme.typography.heading1
            )
        }
    } else {
        val listState = rememberLazyListState()
        val finalItems = remember(items, isPending) {
            if (isPending) listOf(PendingItem(LlmState.THINKS.state)) + items.list else items.list
        }
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.indent_16dp)),
            contentPadding = PaddingValues(dimensionResource(id = R.dimen.indent_16dp)),
            reverseLayout = true
        ) {
            items(finalItems.size) { index ->
                val item = finalItems[index]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (item is OwnMessageItem)
                        Arrangement.End else
                        Arrangement.Start
                ) {
                    UiItemDelegate(item)
                }
            }
        }
    }
}

@Composable
private fun ChatTopBar(onBack: () -> Unit) {
    val navigationIcon = AppTopAppBarIconItem(
        iconId = R.drawable.baseline_arrow_back_24,
        onClick = { onBack() }
    )
    AppTopAppBar(
        title = "Ollama",
        navigationIcon = navigationIcon
    )
}

@Composable
private fun ChatBottomBar(
    typedText: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    SendMessageTextField(
        value = typedText,
        onTextChange = onTextChange,
        onSend = onSend
    )
}

@Composable
private fun ChatContent(
    chatState: ChatState,
    modifier: Modifier = Modifier,
) {
    if (chatState.error == null) {
        MessageList(
            items = ImmutableList(chatState.history.list.reversed()),
            isPending = chatState.isPending,
            modifier = modifier
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
