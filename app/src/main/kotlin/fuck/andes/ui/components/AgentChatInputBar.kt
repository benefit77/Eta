package fuck.andes.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import fuck.andes.ui.model.PendingImageUi
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val ATTACH_MENU_ITEMS = listOf("添加图片")

@Composable
fun AgentChatInputBar(
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
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var showAttachPopup by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onAttachImage(uri.toString())
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (pendingImages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pendingImages.forEach { image ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainer),
                    ) {
                        val bitmap = rememberDataUrlBitmap(image.dataUrl)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        IconButton(
                            onClick = { onRemoveImage(image.id) },
                            modifier = Modifier.align(Alignment.TopEnd).size(20.dp),
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Close,
                                contentDescription = "移除图片",
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
        TextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            label = "输入任务",
            useLabelAsPlaceholder = true,
            singleLine = false,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            leadingIcon = {
                Box {
                    IconButton(onClick = { showAttachPopup = true }) {
                        Icon(
                            imageVector = MiuixIcons.AddCircle,
                            contentDescription = "附件",
                        )
                    }
                    OverlayListPopup(
                        show = showAttachPopup,
                        alignment = PopupPositionProvider.Align.BottomStart,
                        onDismissRequest = { showAttachPopup = false },
                        content = {
                            ListPopupColumn {
                                ATTACH_MENU_ITEMS.forEachIndexed { index, label ->
                                    DropdownImpl(
                                        text = label,
                                        optionSize = ATTACH_MENU_ITEMS.size,
                                        isSelected = false,
                                        index = index,
                                        onSelectedIndexChange = {
                                            showAttachPopup = false
                                            photoPicker.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                                ),
                                            )
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onThinkingChange(!thinkingEnabled) },
                        enabled = !isStreaming,
                    ) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_sparkles),
                            contentDescription = if (thinkingEnabled) "关闭思考" else "开启思考",
                            tint = if (thinkingEnabled) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurfaceVariantActions
                            },
                        )
                    }
                    if (isStreaming) {
                        IconButton(onClick = onStop) {
                            Icon(
                                imageVector = MiuixIcons.Close,
                                contentDescription = "停止",
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                keyboard?.hide()
                                onSend()
                            },
                            enabled = input.isNotBlank() || pendingImages.isNotEmpty(),
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Send,
                                contentDescription = "发送",
                            )
                        }
                    }
                }
            },
        )
    }
}
