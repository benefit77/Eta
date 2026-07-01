package fuck.andes.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import fuck.andes.ui.model.AgentChatMessageUi
import fuck.andes.ui.model.PendingImageUi
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val SUGGESTION_PROMPTS = listOf(
    "帮我打开微信",
    "搜索通讯录里的联系人",
    "截图并描述当前屏幕",
)

/**
 * 聊天主体：消息流 + 底部输入框。
 */
@Composable
fun AgentChatBody(
    messages: List<AgentChatMessageUi>,
    input: String,
    isStreaming: Boolean,
    thinkingEnabled: Boolean,
    pendingImages: List<PendingImageUi>,
    onInputChange: (String) -> Unit,
    onThinkingChange: (Boolean) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttachImage: (String) -> Unit,
    onRemoveImage: (String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onRunTraceClick: () -> Unit,
    isDrawerOpen: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current

    val displayMessages = messages.asReversed()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.scrollToItem(0)
        }
    }

    var sentFromKeyboard by remember { mutableStateOf(false) }
    LaunchedEffect(isStreaming) {
        if (isStreaming && sentFromKeyboard) {
            keyboard?.hide()
            sentFromKeyboard = false
        }
    }

    LaunchedEffect(isDrawerOpen) {
        if (isDrawerOpen) {
            keyboard?.hide()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (messages.isEmpty()) {
            EmptyChatState(
                onSuggestionClick = onSuggestionClick,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                state = scrollState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
            ) {
                items(
                    items = displayMessages,
                    key = { it.id },
                ) { message ->
                    ChatMessageItem(
                        message = message,
                        onSuggestionClick = onSuggestionClick,
                        onRunTraceClick = onRunTraceClick,
                    )
                }
            }
        }
        AgentChatInputBar(
            input = input,
            isStreaming = isStreaming,
            thinkingEnabled = thinkingEnabled,
            pendingImages = pendingImages,
            onInputChange = onInputChange,
            onThinkingChange = onThinkingChange,
            onSend = {
                sentFromKeyboard = true
                onSend()
            },
            onStop = onStop,
            onAttachImage = onAttachImage,
            onRemoveImage = onRemoveImage,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        )
    }
}

@Composable
private fun EmptyChatState(
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "有什么可以帮你？",
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SUGGESTION_PROMPTS.forEach { prompt ->
                TextButton(
                    text = prompt,
                    onClick = { onSuggestionClick(prompt) },
                )
            }
        }
    }
}
