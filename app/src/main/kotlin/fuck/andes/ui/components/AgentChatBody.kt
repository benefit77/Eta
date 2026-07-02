package fuck.andes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import fuck.andes.ui.model.AgentChatMessageUi
import fuck.andes.ui.model.PendingImageUi
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 聊天主体：消息流 + 底部输入框。
 */
@OptIn(ExperimentalLayoutApi::class)
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
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_atom),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MiuixTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "有什么可以帮你？",
            style = MiuixTheme.textStyles.title1,
            color = MiuixTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "选择以下一个任务开始，或者在下方输入你想让我做的事",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val suggestions = listOf(
                SuggestionItem(
                    title = "帮我打开微信",
                    description = "快速启动微信应用",
                    iconRes = LucideR.drawable.lucide_ic_rocket,
                    prompt = "帮我打开微信"
                ),
                SuggestionItem(
                    title = "搜索通讯录联系人",
                    description = "在通讯录中搜索好友或电话",
                    iconRes = LucideR.drawable.lucide_ic_search,
                    prompt = "搜索通讯录里的联系人"
                ),
                SuggestionItem(
                    title = "描述当前屏幕内容",
                    description = "自动截屏并用多模态大模型分析",
                    iconRes = LucideR.drawable.lucide_ic_scan_text,
                    prompt = "截图并描述当前屏幕"
                )
            )

            suggestions.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .clickable { onSuggestionClick(item.prompt) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.description,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }

                }
            }
        }
    }
}

private data class SuggestionItem(
    val title: String,
    val description: String,
    val iconRes: Int,
    val prompt: String
)

